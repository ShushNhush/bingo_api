package app.handlers;

import app.config.HibernateConfig;
import app.daos.impl.RoomDAO;
import app.dtos.PlayerDTO;
import app.dtos.PlayerStatusDTO;
import app.utils.Utils;
import app.utils.Utils.*;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.javalin.websocket.WsContext;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;



public class WebSocketHandler {

    private static final RoomDAO dao = RoomDAO.getInstance(HibernateConfig.getEntityManagerFactory());
    // Map of room numbers to WebSocket sessions and their PlayerDTOs
    private static final Map<Integer, Map<WsContext, PlayerStatusDTO>> roomSessions = new ConcurrentHashMap<>();

    public static void onConnect(WsContext ctx, int roomNumber, PlayerDTO player) {
        roomSessions.putIfAbsent(roomNumber, new ConcurrentHashMap<>());
        Map<WsContext, PlayerStatusDTO> playersInRoom = roomSessions.get(roomNumber);

        // Check if the player is reconnecting
        PlayerStatusDTO existingPlayerStatus = playersInRoom.values()
                .stream()
                .filter(status -> status.getPlayer().getId() == player.getId())
                .findFirst()
                .orElse(null);

        if (existingPlayerStatus != null) {
            // Player is reconnecting, replace the old WsContext with the new one
            playersInRoom.entrySet().removeIf(entry -> entry.getValue().getPlayer().getId() == player.getId());
            existingPlayerStatus.setConnected(true); // Mark as reconnected
            playersInRoom.put(ctx, existingPlayerStatus);
            System.out.println("Player reconnected: " + player.getName());

            Map<String, Object> payload = Map.of("playerName", player.getName());
            broadcast(roomNumber, "join", payload);
        } else {
            // New connection
            playersInRoom.put(ctx, new PlayerStatusDTO(player, true));
            System.out.println("New player connected: " + player.getName());
            Map<String, Object> payload = Map.of("playerName", player.getName());
            broadcast(roomNumber, "join", payload);
        }

        PlayerDTO host = dao.getHost(roomNumber);
        if (host != null) {
            sendMessage(ctx, "host-update", Map.of("hostName", host.getName(), "hostId", host.getId()));
        }
    }


    public static PlayerDTO getPlayerFromContext(WsContext ctx, int roomNumber) {
        Map<WsContext, PlayerStatusDTO> playersInRoom = roomSessions.get(roomNumber);
        if (playersInRoom == null) {
            throw new IllegalArgumentException("Room not found: " + roomNumber);
        }

        PlayerDTO player = playersInRoom.get(ctx).getPlayer();
        if (player == null) {
            throw new IllegalArgumentException("Player not found in room: " + roomNumber);
        }

        return player;
    }

    public static void onMessage(WsContext ctx, int roomNumber, String message) {
        Map<WsContext, PlayerStatusDTO> playersInRoom = roomSessions.get(roomNumber);

        if (playersInRoom == null) {
            ctx.send("Room not found.");
            return;
        }

        PlayerDTO player = playersInRoom.get(ctx).getPlayer();
        if (player == null) {
            ctx.send("Player not found in the room.");
            return;
        }

        // Broadcast the player's message to the room
//        broadcast(roomNumber, player.getName() + ": " + message);
    }

    public static void onDisconnect(WsContext ctx, int roomNumber) {
        if (roomSessions.containsKey(roomNumber)) {
            Map<WsContext, PlayerStatusDTO> playersInRoom = roomSessions.get(roomNumber);
            PlayerStatusDTO playerStatus = playersInRoom.get(ctx);

            if (playerStatus != null) {
                playerStatus.setConnected(false); // Mark the player as disconnected
                System.out.println("Player disconnected: " + playerStatus.getPlayer().getName());
                Map<String, Object> payload = Map.of("playerName", playerStatus.getPlayer().getName());
                broadcast(roomNumber, "leave", payload);
            }

            // Remove the room if it's empty
            if (playersInRoom.isEmpty()) {
                roomSessions.remove(roomNumber);
                System.out.println("Room " + roomNumber + " is now empty and has been removed.");
            }
        }
    }

    public static void broadcast(int roomNumber, String type, Map<String, Object> payload) {
        if (roomSessions.containsKey(roomNumber)) {
            Map<WsContext, PlayerStatusDTO> playersInRoom = roomSessions.get(roomNumber);
            ObjectMapper mapper = new ObjectMapper();

            Map<String, Object> message = new HashMap<>();
            message.put("type", type);
            message.put("payload", payload);

            try {
                String jsonMessage = mapper.writeValueAsString(message);

                for (WsContext ctx : playersInRoom.keySet()) {
                    try {
                        if (ctx.session.isOpen()) { // Check if the session is still open
                            ctx.send(jsonMessage);
                        } else {
                            System.out.println("Skipping closed WebSocket session for room: " + roomNumber);
                        }
                    } catch (Exception e) {
                        System.err.println("Failed to send message to session: " + e.getMessage());
                        e.printStackTrace();
                    }
                }
            } catch (JsonProcessingException e) {
                System.err.println("Failed to serialize broadcast message: " + e.getMessage());
                e.printStackTrace();
            }
        } else {
            System.out.println("Room " + roomNumber + " not found in roomSessions.");
        }
    }

    public static void sendMessage(WsContext ctx, String type, Map<String, Object> payload) {
        ObjectMapper mapper = new ObjectMapper();
        Map<String, Object> message = new HashMap<>();
        message.put("type", type);
        message.put("payload", payload);

        try {
            String jsonMessage = mapper.writeValueAsString(message);
            ctx.send(jsonMessage);
        } catch (JsonProcessingException e) {
            System.err.println("Failed to send message: " + e.getMessage());
        }
    }

    public static Map<WsContext, PlayerStatusDTO> getPlayersInRoom(int roomNumber) {
        return roomSessions.getOrDefault(roomNumber, Map.of());
    }

    public static PlayerStatusDTO getPlayerStatus(WsContext ctx, int roomNumber) {

        if (roomSessions.containsKey(roomNumber)) {
            return roomSessions.get(roomNumber).get(ctx);
        }
        return null;
    }

    public static void closeRoom(int roomNumber) {
        Map<WsContext, PlayerStatusDTO> playersInRoom = roomSessions.get(roomNumber);
        if (playersInRoom != null) {
            playersInRoom.keySet().forEach(ctx -> {
                try {
                    if (ctx.session.isOpen()) {
                        ctx.send("Room closed due to inactivity."); // Notify each player
                    }
                    ctx.session.close(); // Close WebSocket connection
                } catch (Exception e) {
                    e.printStackTrace();
                }
            });
            roomSessions.remove(roomNumber); // Remove the room from the session map
        }
    }
}


