package app.utils;

import app.config.HibernateConfig;
import app.daos.impl.RoomDAO;
import app.dtos.PlayerDTO;
import app.entities.Room;
import com.nimbusds.jose.*;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jose.crypto.MACVerifier;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;

import java.util.Date;

public class JWTUtil {

    private static final String SECRET = System.getenv("SECRET_KEY");

    private static final RoomDAO roomDAO = RoomDAO.getInstance(HibernateConfig.getEntityManagerFactory());

    public static String generateJWT(PlayerDTO player, String SECRET_KEY, String ISSUER, String TOKEN_EXPIRE_TIME) throws JOSEException {
        // Create the claims
        JWTClaimsSet claimsSet = new JWTClaimsSet.Builder()
            .subject(player.getName())
            .claim("id", player.getId())
            .claim("roomNumber", player.getRoom().getRoomNumber())
            .issuer(ISSUER)
            .issueTime(new Date())
            .expirationTime(new Date(System.currentTimeMillis() + Long.parseLong(TOKEN_EXPIRE_TIME))) // 1-hour expiration
            .build();

        // Create the signed JWT
        JWSHeader header = new JWSHeader(JWSAlgorithm.HS256);
        SignedJWT signedJWT = new SignedJWT(header, claimsSet);

        // Sign the JWT
        JWSSigner signer = new MACSigner(SECRET_KEY.getBytes());
        signedJWT.sign(signer);

        return signedJWT.serialize();
    }

    public static PlayerDTO validateAndExtractPlayer(String jwt) throws Exception {
        SignedJWT signedJWT = SignedJWT.parse(jwt);

        // Verify the signature
        JWSVerifier verifier = new MACVerifier(SECRET.getBytes());
        if (!signedJWT.verify(verifier)) {
            throw new JOSEException("JWT signature verification failed");
        }

        // Extract claims
        var claims = signedJWT.getJWTClaimsSet();
        PlayerDTO player = new PlayerDTO();
        player.setId(Math.toIntExact(claims.getLongClaim("id"))); // Convert Long to int
        player.setName(claims.getSubject());
        player.setRoom(new Room(roomDAO.getRoom(claims.getIntegerClaim("roomNumber"))));
        return player;
    }
}
