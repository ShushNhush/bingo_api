package app;

import app.config.ApplicationConfig;
import app.daos.impl.RoomDAO;
import app.handlers.WebSocketHandler;
import app.services.RoomCleanupService;
import io.javalin.Javalin;


public class Main {

    public static void main(String[] args) {
        Javalin app = ApplicationConfig.startServer(7171);


        RoomDAO roomDAO = new RoomDAO();
        WebSocketHandler webSocketHandler = new WebSocketHandler();

        // Initialize and start the RoomCleanupService
        RoomCleanupService roomCleanupService = new RoomCleanupService(roomDAO, webSocketHandler);

        // Add shutdown hooks to stop services and clean up resources
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("Shutting down application...");
            roomCleanupService.shutdown(); // Stop the RoomCleanupService
        }));


    }
}