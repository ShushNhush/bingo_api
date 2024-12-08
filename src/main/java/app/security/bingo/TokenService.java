package app.security.bingo;

public class TokenService {

    public String createToken(int playerId, int roomId) {
        return TokenUtils.generateToken(playerId, roomId);
    }

    public boolean validateToken(String token, int roomId) {
        return TokenUtils.verifyToken(token, roomId);
    }

    public int extractPlayerId(String token) {
        try {
            return TokenUtils.getPlayerIdFromToken(token);
        } catch (Exception e) {
            throw new RuntimeException("Error extracting player ID from token", e);
        }
    }
}
