package app.handlers;

import app.dtos.PlayerDTO;
import io.javalin.websocket.WsContext;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class WebSocketHandler {

    // Map of room numbers to WebSocket sessions and their PlayerDTOs
    private static final Map<Integer, Map<WsContext, PlayerDTO>> roomSessions = new ConcurrentHashMap<>();

    public static void createRoomSession(int roomNumber) {
        roomSessions.putIfAbsent(roomNumber, new ConcurrentHashMap<>());
    }

    public static void addPlayerToRoom(int roomNumber, PlayerDTO player) {
        // Broadcast a message to all WebSocket sessions in the room
        broadcastMessage(roomNumber, player.getName() + " joined the room.");
    }

    public static void onConnect(WsContext ctx, int roomNumber, PlayerDTO player) {
        roomSessions.putIfAbsent(roomNumber, new ConcurrentHashMap<>());
        roomSessions.get(roomNumber).put(ctx, player);
        broadcastMessage(roomNumber, player.getName() + " joined the room.");
    }

    public static void onMessage(WsContext ctx, int roomNumber, String message) {
        PlayerDTO player = roomSessions.get(roomNumber).get(ctx);
        broadcastMessage(roomNumber, (player != null ? player.getName() : "Unknown") + ": " + message);
    }

    public static void onDisconnect(WsContext ctx, int roomNumber) {
        PlayerDTO player = roomSessions.get(roomNumber).remove(ctx);
        if (player != null) {
            broadcastMessage(roomNumber, player.getName() + " left the room.");
        }
        if (roomSessions.get(roomNumber).isEmpty()) {
            roomSessions.remove(roomNumber);
        }
    }

    private static void broadcastMessage(int roomNumber, String message) {
        roomSessions.getOrDefault(roomNumber, Map.of()).keySet()
                .forEach(session -> session.send(message));
    }
}
