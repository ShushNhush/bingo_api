package app.controllers.impl;

import app.config.HibernateConfig;
import app.daos.impl.PlayerDAO;
import app.daos.impl.RoomDAO;
import app.dtos.HostDTO;
import app.dtos.PlayerDTO;
import app.dtos.RoomDTO;
import app.dtos.RoomWithHostDTO;
import app.handlers.WebSocketHandler;
import app.security.exceptions.ApiException;
import app.utils.JWTUtil;
import app.utils.Utils;
import io.javalin.http.Context;
import io.javalin.websocket.WsContext;
import jakarta.persistence.EntityManagerFactory;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
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
            // Parse the request body into a wrapper class
            RoomWithHostDTO request = ctx.bodyAsClass(RoomWithHostDTO.class);

            if (request.getRoom() == null || request.getPlayer() == null) {
                throw new IllegalArgumentException("Room and host information are required.");
            }

            RoomWithHostDTO result = roomDAO.create(request.getRoom(), request.getPlayer());

            // Generate JWT for the host
            String jwt = createToken(result.getPlayer());

            // Build WebSocket URL
            String wsUrl = "/api/rooms/" + result.getRoom().getRoomNumber();

            HostDTO host = new HostDTO(result.getPlayer().getId(), result.getPlayer().getName());

            // Respond with the wrapper object
            Map<String, Object> response = new HashMap<>();
            response.put("token", jwt);
            response.put("host", host);
            response.put("webSocketUrl", wsUrl);

            ctx.status(201).json(response);

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
            PlayerDTO playerDTO = ctx.bodyAsClass(PlayerDTO.class);        // Parse player details from request body

            RoomWithHostDTO result = roomDAO.addPlayerToRoom(roomNumber, playerDTO); // Add player to the room

            RoomDTO roomDTO = roomDAO.getRoom(roomNumber);

            String webSocketUrl = "ws://localhost:7171/api/rooms/" + roomNumber;
            String jwt = createToken(result.getPlayer());

            HostDTO host = new HostDTO(roomDTO.getHost().getId(), roomDTO.getHost().getName());

            Map<String, Object> response = new HashMap<>();
            response.put("token", jwt);
            response.put("host", host);
            response.put("webSocketUrl", webSocketUrl);


            ctx.status(201).json(response);
        } catch (IllegalArgumentException e) {
            ctx.status(400).json(Map.of("error", "Invalid input", "details", e.getMessage()));
        } catch (Exception e) {
            e.printStackTrace();
            ctx.status(500).json(Map.of("error", "Unexpected error", "details", e.getMessage()));
        }
    }

    public void deleteRoom(Context ctx) {
        try {
            int roomNumber = Integer.parseInt(ctx.pathParam("roomNumber"));
            roomDAO.deleteRoom(roomNumber);
            ctx.status(204).json("Room deleted successfully");
        } catch (IllegalArgumentException e) {
            ctx.status(400).json(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            ctx.status(500).json(Map.of("error", "An unexpected error occurred", "details", e.getMessage()));
        }
    }

    public void getPlayerBoard(Context ctx) {

        int roomNumber = Integer.parseInt(ctx.pathParam("roomNumber"));
        int playerId = Integer.parseInt(ctx.pathParam("playerId"));

        PlayerDTO player = playerDAO.getById(playerId);

        if (player == null || player.getRoom().getRoomNumber() != roomNumber) {
            throw new ApiException(404, "Player not found or does not belong to this room.");
        }
        ctx.json(Map.of("board", player.getBoard()));
    }

    public void checkWinner(Context ctx) {
        try {
            int roomNumber = Integer.parseInt(ctx.pathParam("roomNumber"));
            int playerId = Integer.parseInt(ctx.queryParam("playerId"));

            boolean isWinner = roomDAO.checkWinner(roomNumber, playerId);

            ctx.json(Map.of("isWinner", isWinner));
            ctx.status(200);
        } catch (IllegalArgumentException e) {
            ctx.status(400).json(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            ctx.status(500).json(Map.of("error", "An unexpected error occurred", "details", e.getMessage()));
        }
    }

    public String createToken(PlayerDTO player) {
        try {
            String ISSUER;
            String TOKEN_EXPIRE_TIME;
            String SECRET_KEY;

            if (System.getenv("DEPLOYED") != null) {
                ISSUER = System.getenv("ISSUER");
                TOKEN_EXPIRE_TIME = System.getenv("TOKEN_EXPIRE_TIME");
                SECRET_KEY = System.getenv("SECRET_KEY");
            } else {
                ISSUER = Utils.getPropertyValue("ISSUER", "config.properties");
                TOKEN_EXPIRE_TIME = Utils.getPropertyValue("TOKEN_EXPIRE_TIME", "config.properties");
                SECRET_KEY = Utils.getPropertyValue("SECRET_KEY", "config.properties");
            }
            return JWTUtil.generateJWT(player, SECRET_KEY, ISSUER, TOKEN_EXPIRE_TIME);
        } catch (Exception e) {
            e.printStackTrace();
            throw new ApiException(500, "Could not create token");
        }
    }


    public void getRoom(Context ctx) {

        int roomNumber = Integer.parseInt(ctx.pathParam("roomNumber"));
        RoomDTO room = roomDAO.getRoom(roomNumber);

        if (room == null) {
            throw new ApiException(404, "Room not found");
        }

        ctx.json(room);
        ctx.status(200);
    }
}
