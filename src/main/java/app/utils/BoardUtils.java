package app.utils;

import com.fasterxml.jackson.databind.ObjectMapper;

public class BoardUtils {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    public static String serializeBoard(String[][] board) {
        try {
            return MAPPER.writeValueAsString(board);
        } catch (Exception e) {
            throw new RuntimeException("Failed to serialize board", e);
        }
    }

    public static String[][] deserializeBoard(String boardJson) {
        try {
            return MAPPER.readValue(boardJson, String[][].class);
        } catch (Exception e) {
            throw new RuntimeException("Failed to deserialize board", e);
        }
    }
}
