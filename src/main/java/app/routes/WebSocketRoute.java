package app.routes;

import app.config.HibernateConfig;
import app.daos.impl.RoomDAO;
import app.dtos.PlayerDTO;
import app.handlers.WebSocketHandler;
import app.utils.Utils;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.javalin.Javalin;

import java.time.Duration;
import java.util.Map;

public class WebSocketRoute {

    RoomDAO roomDAO = RoomDAO.getInstance(HibernateConfig.getEntityManagerFactory());

    public void register(Javalin app) {

        app.ws("/rooms/{roomNumber}", ws -> {

            ws.onConnect(ctx -> {
                System.out.println("WebSocket connected: " + ctx.session.getRemoteAddress());
                System.out.println("Session ID: " + ctx.sessionId());
                ctx.session.setIdleTimeout(Duration.ofHours(5));

            });

            ws.onClose(ctx -> {
                System.out.println("WebSocket disconnected: " + ctx.session.getRemoteAddress());
                WebSocketHandler.onDisconnect(ctx, Integer.parseInt(ctx.pathParam("roomNumber")));
            });

            ws.onMessage(ctx -> {
                int roomNumber = Integer.parseInt(ctx.pathParam("roomNumber"));
                String rawMessage = ctx.message();

                try {
                    ObjectMapper mapper = Utils.getObjectMapper();
                    Map<String, Object> messageMap = mapper.readValue(rawMessage, Map.class);
                    String action = (String) messageMap.get("action");

                    switch (action) {
                        case "connect":
                            PlayerDTO player = mapper.convertValue(messageMap.get("player"), PlayerDTO.class);
                            WebSocketHandler.onConnect(ctx, roomNumber, player);
                            System.out.println("Player connected: " + player.getName());
                            break;

                        case "pullNumber":
                            // Retrieve player from the message
                            PlayerDTO pullRequest = messageMap.containsKey("player")
                                    ? mapper.convertValue(messageMap.get("player"), PlayerDTO.class)
                                    : WebSocketHandler.getPlayerFromContext(ctx, roomNumber);

                            System.out.println("Pull request from: " + pullRequest.getName());
                            // Validate the host
                            if (roomDAO.getRoom(roomNumber).getHost().getId() == pullRequest.getId()) {
                                int nextNumber = roomDAO.pullNumber(roomNumber);
                                Map<String, Object> payload = Map.of("nextNumber", nextNumber);
                                WebSocketHandler.broadcast(roomNumber, "nextNumber", payload);
                                System.out.println("Number pulled: " + nextNumber);
                            } else {
                                ctx.send("Only the host can pull numbers");
                            }
                            break;

                        case "chat":
                            // Chat message broadcast
                            String chatMessage = (String) messageMap.get("message");
                            PlayerDTO sender = WebSocketHandler.getPlayerFromContext(ctx, roomNumber);
                            Map<String, Object> chatPayload = Map.of("sender", sender.getName(), "message", chatMessage);
                            WebSocketHandler.broadcast(roomNumber, "chat", chatPayload);
                            break;

                        case "submit":
                            // Submit a winning board
                            PlayerDTO submitter = WebSocketHandler.getPlayerFromContext(ctx, roomNumber);
                            boolean isWinner = roomDAO.checkWinner(roomNumber, submitter.getId());

                            // Send the result to the submitter
                            Map<String, Object> responsePayload = Map.of(
                                    "isWinner", isWinner,
                                    "message", isWinner ? submitter.getName() + " is the winner!" : "Not a winner"
                            );

                            if (isWinner) {
                                // Broadcast the winner message to all players
                                WebSocketHandler.broadcast(roomNumber, "submit-result", responsePayload);
                            } else {
                                // Send the response only to the submitter
                                WebSocketHandler.sendMessage(ctx, "submit-result", responsePayload);
                            }
                            break;
                        default:
                            ctx.send("Unknown action: " + action);
                    }
                } catch (Exception e) {
                    System.err.println("Error processing WebSocket message: " + e.getMessage());
                    ctx.send("Error processing message: " + e.getMessage());
                }
            });

        });
    }

}
