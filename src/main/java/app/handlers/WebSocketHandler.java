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
        roomSessions.putIfAbsent(roomNumber, new ConcurrentHashMap<>());
        Map<WsContext, PlayerDTO> playersInRoom = roomSessions.get(roomNumber);

        // Remove old session if the player is reconnecting
        playersInRoom.entrySet().removeIf(entry -> entry.getValue().getId() == player.getId());

        // Add new session
        playersInRoom.put(ctx, player);

        System.out.println("Player reconnected: " + player.getName() + " to room: " + roomNumber);
    }


    public static PlayerDTO getPlayerFromContext(WsContext ctx, int roomNumber) {
        Map<WsContext, PlayerDTO> playersInRoom = roomSessions.get(roomNumber);
        if (playersInRoom == null) {
            throw new IllegalArgumentException("Room not found: " + roomNumber);
        }

        PlayerDTO player = playersInRoom.get(ctx);
        if (player == null) {
            throw new IllegalArgumentException("Player not found in room: " + roomNumber);
        }

        return player;
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

    public static void broadcastMessage(int roomNumber, String message) {

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
