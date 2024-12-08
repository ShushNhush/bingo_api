package app.controllers.impl;

import app.config.HibernateConfig;
import app.daos.impl.PlayerDAO;
import app.daos.impl.RoomDAO;
import app.dtos.PlayerDTO;
import app.dtos.RoomDTO;
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
        RoomDTO newRoom = roomDAO.create(ctx.bodyAsClass(RoomDTO.class));
        ctx.json("new room created: " + newRoom.getRoomNumber());
        ctx.status(201);
    }

    public void pullNumber(Context ctx) {
        int roomId = Integer.parseInt(ctx.pathParam("roomNumber"));
        ctx.json(roomDAO.pullNumber(roomId));
        ctx.status(200);
    }

    public void addPlayer(Context ctx) {
        try {
            int roomId = Integer.parseInt(ctx.pathParam("roomNumber")); // Extract room ID from path
            PlayerDTO playerDTO = ctx.bodyAsClass(PlayerDTO.class);

            PlayerDTO newPlayer = roomDAO.addPlayerToRoom(roomId, playerDTO);

            ctx.status(201).json("Player added: " + newPlayer.getId() + ": " + newPlayer.getName());
        } catch (Exception e) {
            ctx.status(400).json(Map.of("error", "Invalid input", "details", e.getMessage()));
        }
    }

}
