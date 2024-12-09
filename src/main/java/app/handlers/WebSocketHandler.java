package app.handlers;

import app.dtos.PlayerDTO;
import app.utils.Utils;
import app.utils.Utils.*;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.javalin.websocket.WsContext;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class WebSocketHandler {

    // Map of room numbers to WebSocket sessions and their PlayerDTOs
    private static final Map<Integer, Map<WsContext, PlayerDTO>> roomSessions = new ConcurrentHashMap<>();

    public static void onConnect(WsContext ctx, int roomNumber, PlayerDTO player) {
        System.out.println("Player connecting: " + player.getName());

        roomSessions.putIfAbsent(roomNumber, new ConcurrentHashMap<>());

        // Add the player to the room
        Map<WsContext, PlayerDTO> playersInRoom = roomSessions.get(roomNumber);
        if (player != null) {
            playersInRoom.put(ctx, player);
            System.out.println("Player " + player.getName() + " added to room " + roomNumber);
        } else {
            System.out.println("Player data missing, closing connection");
            ctx.send("Player data missing");
            ctx.session.close();
        }
    }

    public static void onMessage(WsContext ctx, int roomNumber, String message) {
        Map<WsContext, PlayerDTO> playersInRoom = roomSessions.get(roomNumber);

        if (playersInRoom == null) {
            ctx.send("Room not found.");
            return;
        }

        PlayerDTO player = playersInRoom.get(ctx);
        if (player == null) {
            ctx.send("Player not found in the room.");
            return;
        }

        // Broadcast the player's message to the room
        broadcastMessage(roomNumber, player.getName() + ": " + message);
    }

    public static void onDisconnect(WsContext ctx, int roomNumber) {
        Map<WsContext, PlayerDTO> playersInRoom = roomSessions.get(roomNumber);

        if (playersInRoom != null) {
            PlayerDTO player = playersInRoom.remove(ctx); // Remove the player
            if (player != null) {
                broadcastMessage(roomNumber, player.getName() + " left the room.");
            }

            if (playersInRoom.isEmpty()) {
                roomSessions.remove(roomNumber); // Cleanup empty room
            }
        }
    }

    private static void broadcastMessage(int roomNumber, String message) {

        Map<WsContext, PlayerDTO> playersInRoom = roomSessions.get(roomNumber);

        if (playersInRoom == null || playersInRoom.isEmpty()) {
            System.out.println("No players in room " + roomNumber + " to broadcast message: " + message);
            return;
        }

        playersInRoom.keySet().stream()
                .filter(ctx -> ctx.session.isOpen()) // Ensure the session is open
                .forEach(ctx -> {
                    try {
                        ctx.send(message); // Attempt to send the message
                    } catch (Exception e) {
                        System.err.println("Error sending message to session: " + ctx.session.getRemoteAddress());
                        e.printStackTrace();
                    }
                });
    }

    public static Map<WsContext, PlayerDTO> getPlayersInRoom(int roomNumber) {
        return roomSessions.getOrDefault(roomNumber, Map.of());
    }
}
