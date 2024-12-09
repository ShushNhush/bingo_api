package app.daos.impl;

import app.dtos.PlayerDTO;
import app.dtos.RoomDTO;
import app.entities.Player;
import app.entities.Room;
import app.utils.BingoBoardGenerator;
import app.utils.BoardUtils;
import app.utils.RoomCodeGenerator;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.NoResultException;
import jakarta.persistence.TypedQuery;

import java.util.List;

public class RoomDAO {

    private static RoomDAO instance;
    private static EntityManagerFactory emf;

    public static RoomDAO getInstance(EntityManagerFactory emf_) {
        if (instance == null) {
            emf = emf_;
            instance = new RoomDAO();
        }
        return instance;
    }

    public List<Room> getAll() {

        try (var em = emf.createEntityManager()) {

            var query = em.createQuery("SELECT r FROM Room r", Room.class);

            return query.getResultList();

        }
    }

    public RoomDTO create(RoomDTO roomDTO, PlayerDTO hostDTO) {
        try (var em = emf.createEntityManager()) {

            if (hostDTO == null) {
                throw new IllegalArgumentException("Host cannot be null!");
            }

            if (roomDTO == null) {
                throw new IllegalArgumentException("Room cannot be null!");
            }
            em.getTransaction().begin();

            Room newRoom = new Room(roomDTO);
            newRoom.initializeNumbers();
            newRoom.setRoomNumber(generateUniqueRoomNumber());

            Player host = new Player(hostDTO);
            host.setBoard(BoardUtils.serializeBoard(BingoBoardGenerator.generateBoard(null)));
            newRoom.setHost(host);
            newRoom.addPlayer(host);

            em.persist(newRoom);
            em.persist(host);

            em.getTransaction().commit();

            return new RoomDTO(newRoom);
        }
    }

    public int pullNumber(int roomNumber) {
        try (var em = emf.createEntityManager()) {
            em.getTransaction().begin();

            TypedQuery<Room> query = em.createQuery("SELECT r FROM Room r WHERE r.roomNumber = :roomNumber", Room.class);
            query.setParameter("roomNumber", roomNumber);
            Room room = query.getSingleResult();

            Player host = room.getHost();
            host.updateLastActive();

            int number = room.pullNumber();
            em.merge(room);

            em.getTransaction().commit();
            return number;
        }
        catch (NoResultException e) {
            throw new IllegalArgumentException("Room not found!");
        }
    }

    public PlayerDTO addPlayerToRoom(int roomNumber, PlayerDTO playerDTO) {
        try (var em = emf.createEntityManager()) {
            em.getTransaction().begin();

            // Retrieve the room
            TypedQuery<Room> query = em.createQuery("SELECT r FROM Room r WHERE r.roomNumber = :roomNumber", Room.class);
            query.setParameter("roomNumber", roomNumber);
            Room room = query.getSingleResult();

            if (room == null) {
                throw new IllegalArgumentException("Room not found!");
            }

            // Create and associate the player
            Player newPlayer = new Player(playerDTO);
            newPlayer.setBoard(BoardUtils.serializeBoard(BingoBoardGenerator.generateBoard(null)));
            newPlayer.setRoom(room);
            room.addPlayer(newPlayer); // Add player to the room's player list

            em.persist(newPlayer); // Persist the player
            em.merge(room);        // Update the room

            em.getTransaction().commit();
            return new PlayerDTO(newPlayer);
        }
    }

    public int generateUniqueRoomNumber() {
        int roomNumber;
        do {
            roomNumber = RoomCodeGenerator.generateCode();
        } while (isRoomNumberTaken(roomNumber));
        return roomNumber;
    }

    private boolean isRoomNumberTaken(int roomNumber) {
        // Check the database or an in-memory cache for existing room numbers
        try (var em = emf.createEntityManager()) {
            Long count = em.createQuery("SELECT COUNT(r) FROM Room r WHERE r.roomNumber = :roomNumber", Long.class)
                    .setParameter("roomNumber", roomNumber)
                    .getSingleResult();
            return count > 0;
        }
    }

}
