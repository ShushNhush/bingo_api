package app.security.bingo;


import com.nimbusds.jose.shaded.json.parser.ParseException;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import com.nimbusds.jose.*;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jose.crypto.MACVerifier;

import java.util.Date;

public class TokenUtils {

    public static String generateToken(int playerId, int roomId) {
        try {
            // Create JWT claims
            JWTClaimsSet claims = new JWTClaimsSet.Builder()
                .issuer(TokenConfig.ISSUER)
                .claim("playerId", playerId)
                .claim("roomId", roomId)
                .expirationTime(new Date(System.currentTimeMillis() + TokenConfig.TOKEN_EXPIRE_TIME))
                .build();

            // Create and sign JWT
            SignedJWT signedJWT = new SignedJWT(
                new JWSHeader(JWSAlgorithm.HS256),
                claims
            );

            JWSSigner signer = new MACSigner(TokenConfig.SECRET_KEY);
            signedJWT.sign(signer);

            return signedJWT.serialize();
        } catch (Exception e) {
            throw new RuntimeException("Error generating token", e);
        }
    }

    public static boolean verifyToken(String token, int roomId) {
        try {
            SignedJWT signedJWT = SignedJWT.parse(token);

            // Verify the signature
            JWSVerifier verifier = new MACVerifier(TokenConfig.SECRET_KEY);
            if (!signedJWT.verify(verifier)) {
                return false; // Invalid signature
            }

            // Validate claims
            JWTClaimsSet claims = signedJWT.getJWTClaimsSet();
            int tokenRoomId = claims.getIntegerClaim("roomId");
            Date expirationTime = claims.getExpirationTime();

            // Ensure room ID matches and token is not expired
            return tokenRoomId == roomId && new Date().before(expirationTime);
        } catch (Exception e) {
            return false; // Token is invalid or expired
        }
    }

    public static int getPlayerIdFromToken(String token) throws ParseException, java.text.ParseException {
        SignedJWT signedJWT = SignedJWT.parse(token);
        JWTClaimsSet claims = signedJWT.getJWTClaimsSet();
        return claims.getIntegerClaim("playerId");
    }
}
