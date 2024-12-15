package app;

import app.config.ApplicationConfig;
import app.daos.impl.RoomDAO;
import app.handlers.WebSocketHandler;
import app.services.RoomCleanupService;
import io.javalin.Javalin;


public class Main {

    public static void main(String[] args) {
        Javalin app = ApplicationConfig.startServer(7171);


    }
}