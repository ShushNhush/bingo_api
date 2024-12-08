package app.utils;

import java.util.Random;

public class RoomCodeGenerator {

    private static final int MIN = 1000;
    private static final int MAX = 9999;

    public static int generateCode() {
        Random random = new Random();
        return random.nextInt((MAX - MIN) + 1) + MIN; // Generates a number between 1000 and 9999
    }
}