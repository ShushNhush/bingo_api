package app.utils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class BingoBoardGenerator {

    public static String[][] generateBoard(List<String> customWords) {
        String[][] board = new String[5][5];

        if (customWords == null || customWords.isEmpty()) {
            // Default numeric board
            for (int col = 0; col < 5; col++) {
                List<String> numbers = generateColumnNumbers(col);
                for (int row = 0; row < 5; row++) {
                    board[row][col] = numbers.get(row);
                }
            }
            // Set the "FREE" slot for the numeric board
            board[2][2] = "FREE";
        } else {
            // Custom word-based board
            Collections.shuffle(customWords);
            int index = 0;
            for (int row = 0; row < 5; row++) {
                for (int col = 0; col < 5; col++) {
                    board[row][col] = (row == 2 && col == 2) ? "FREE" : customWords.get(index++);
                }
            }
        }
        return board;
    }


    private static List<String> generateColumnNumbers(int col) {
        int start = col * 15 + 1;
        int end = start + 14;
        List<String> numbers = new ArrayList<>();
        for (int i = start; i <= end; i++) {
            numbers.add(String.valueOf(i));
        }
        Collections.shuffle(numbers);
        return numbers.subList(0, 5);
    }
}
