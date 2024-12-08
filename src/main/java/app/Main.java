package app;

import app.config.ApplicationConfig;
import app.config.HibernateConfig;
import app.daos.impl.RoomDAO;
import app.dtos.PlayerDTO;
import app.dtos.RoomDTO;
import app.entities.Room;
import app.handlers.WebSocketHandler;
import io.javalin.Javalin;
import jakarta.persistence.EntityManagerFactory;

public class Main {

    public static void main(String[] args) {
       Javalin app = ApplicationConfig.startServer(7171);

        // Register WebSocket routes directly
        app.ws("/rooms/{roomNumber}", ws -> {
            ws.onConnect(ctx -> {
                int roomNumber = Integer.parseInt(ctx.pathParam("roomNumber"));
                PlayerDTO player = ctx.sessionAttribute("player");

                if (player != null) {
                    WebSocketHandler.onConnect(ctx, roomNumber, player);
                } else {
                    ctx.send("Player information missing");
                    ctx.session.close();
                }
            });

            ws.onMessage(ctx -> {
                int roomNumber = Integer.parseInt(ctx.pathParam("roomNumber"));
                WebSocketHandler.onMessage(ctx, roomNumber, ctx.message());
            });

            ws.onClose(ctx -> {
                int roomNumber = Integer.parseInt(ctx.pathParam("roomNumber"));
                WebSocketHandler.onDisconnect(ctx, roomNumber);
            });
        });
    }
}