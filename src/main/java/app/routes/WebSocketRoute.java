package app.routes;

import app.dtos.PlayerDTO;
import app.handlers.WebSocketHandler;
import app.utils.Utils;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.javalin.Javalin;
import io.javalin.websocket.WsContext;

import java.time.Duration;
import java.util.Map;

public class WebSocketRoute {

    public void register(Javalin app) {

        app.ws("/rooms/{roomNumber}", ws -> {

            ws.onConnect(ctx -> {
                System.out.println("WebSocket connected: " + ctx.session.getRemoteAddress());
                ctx.session.setIdleTimeout(Duration.ofHours(5));

            });

            ws.onClose(ctx -> {
                System.out.println("WebSocket disconnected: " + ctx.session.getRemoteAddress());
            });

            ws.onMessage(ctx -> {
                int roomNumber = Integer.parseInt(ctx.pathParam("roomNumber"));
                String rawMessage = ctx.message();

                try {
                    // Attempt to parse as JSON
                    ObjectMapper mapper = Utils.getObjectMapper();
                    Map<String, Object> messageMap;
                    String action = null;

                    try {
                        messageMap = mapper.readValue(rawMessage, Map.class);
                        action = (String) messageMap.get("action");
                    } catch (Exception e) {
                        // Handle as plain text if not valid JSON
                        System.out.println("Received plain text message: " + rawMessage);
                        WebSocketHandler.onMessage(ctx, roomNumber, rawMessage);
                        return;
                    }

                    // Process JSON messages based on the "action" field
                    if ("connect".equals(action)) {
                        PlayerDTO player = mapper.convertValue(messageMap.get("player"), PlayerDTO.class);
                        WebSocketHandler.onConnect(ctx, roomNumber, player);
                        System.out.println("Player connected: " + player.getName());
                    } else if ("message".equals(action)) {
                        String message = (String) messageMap.get("message");
                        WebSocketHandler.onMessage(ctx, roomNumber, message);
                    } else {
                        ctx.send("Unrecognized action: " + action);
                    }
                } catch (Exception e) {
                    System.err.println("Error processing WebSocket message: " + e.getMessage());
                    ctx.send("Error processing message: " + e.getMessage());
                }
            });

        });
    }


//        app.ws("/rooms/{roomNumber}", ws -> {
//            ws.onConnect(ctx -> {
//                System.out.println("WebSocket connected: " + ctx.session.getRemoteAddress());
//                int roomNumber = Integer.parseInt(ctx.pathParam("roomNumber"));
//                PlayerDTO player = ctx.sessionAttribute("player");
//
//                if (player != null) {
//                    WebSocketHandler.onConnect(ctx, roomNumber, player);
//                } else {
//                    ctx.send("Player information missing");
//                    ctx.session.close();
//                }
//            });
//
////            ws.onMessage(ctx -> {
////                int roomNumber = Integer.parseInt(ctx.pathParam("roomNumber"));
////                String message = ctx.message();
////                WebSocketHandler.onMessage(ctx, roomNumber, message);
////            });
//
//            ws.onClose(ctx -> {
//                System.out.println("WebSocket disconnected: " + ctx.session.getRemoteAddress());
//                int roomNumber = Integer.parseInt(ctx.pathParam("roomNumber"));
//                WebSocketHandler.onDisconnect(ctx, roomNumber);
//            });
//        });
//    }
}
