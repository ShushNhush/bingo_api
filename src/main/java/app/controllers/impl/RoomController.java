package app.controllers.impl;

import app.config.HibernateConfig;
import app.daos.impl.PlayerDAO;
import app.daos.impl.RoomDAO;
import app.dtos.PlayerDTO;
import app.dtos.RoomDTO;
import app.handlers.WebSocketHandler;
import io.javalin.http.Context;
import jakarta.persistence.EntityManagerFactory;

import java.util.Map;

public class RoomController {

    private EntityManagerFactory emf = HibernateConfig.getEntityManagerFactory();
    private RoomDAO roomDAO = RoomDAO.getInstance(emf);
    private PlayerDAO playerDAO = PlayerDAO.getInstance(emf);

    public void getAll(Context ctx) {
        ctx.json(roomDAO.getAll());
        ctx.status(200);
    }

    public void create(Context ctx) {
        try {
            // Parse the request body
            Map<String, Object> body = ctx.bodyAsClass(Map.class);

            String rules = (String) body.get("rules");
            if (rules == null || rules.isBlank()) {
                throw new IllegalArgumentException("Rules cannot be null or empty");
            }
            RoomDTO roomDTO = new RoomDTO();
            roomDTO.setRules(rules);

            Map<String, Object> hostMap = (Map<String, Object>) body.get("host");
            if (hostMap == null) {
                throw new IllegalArgumentException("Host information is required");
            }
            String hostName = (String) hostMap.get("name");
            if (hostName == null || hostName.isBlank()) {
                throw new IllegalArgumentException("Host name cannot be null or empty");
            }
            PlayerDTO host = new PlayerDTO();
            host.setName(hostName);

            // Create the room
            RoomDTO newRoom = roomDAO.create(roomDTO, host);

            // Initialize WebSocket session for the room
            WebSocketHandler.createRoomSession(newRoom.getRoomNumber());

            ctx.json(Map.of("message", "New room created", "roomNumber", newRoom.getRoomNumber()));
            ctx.status(201);
        } catch (IllegalArgumentException e) {
            ctx.status(400).json(Map.of("error", "Invalid input", "details", e.getMessage()));
        } catch (Exception e) {
            ctx.status(500).json(Map.of("error", "An unexpected error occurred", "details", e.getMessage()));
        }
    }

    public void pullNumber(Context ctx) {
        int roomId = Integer.parseInt(ctx.pathParam("roomNumber"));
        ctx.json(roomDAO.pullNumber(roomId));
        ctx.status(200);
    }

    public void addPlayer(Context ctx) {
        try {
            int roomNumber = Integer.parseInt(ctx.pathParam("roomNumber")); // Extract room number
            PlayerDTO playerDTO = ctx.bodyAsClass(PlayerDTO.class);

            // Add player to the database
            PlayerDTO newPlayer = roomDAO.addPlayerToRoom(roomNumber, playerDTO);

            // Notify WebSocket participants
            WebSocketHandler.addPlayerToRoom(roomNumber, newPlayer);

            ctx.status(201).json(Map.of("message", "Player added", "player", newPlayer));
        } catch (Exception e) {
            ctx.status(400).json(Map.of("error", "Invalid input", "details", e.getMessage()));
        }
    }

}
