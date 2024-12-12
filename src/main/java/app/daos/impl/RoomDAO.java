package app.daos.impl;

import app.dtos.PlayerDTO;
import app.dtos.RoomDTO;
import app.dtos.RoomWithHostDTO;
import app.entities.Player;
import app.entities.Room;
import app.utils.BingoBoardGenerator;
import app.utils.BoardUtils;
import app.utils.RoomCodeGenerator;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.NoResultException;
import jakarta.persistence.TypedQuery;

import java.util.Arrays;
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

    public RoomDTO getRoom(int roomNumber) {
        try (var em = emf.createEntityManager()) {
            var query = em.createQuery("SELECT r FROM Room r WHERE r.roomNumber = :roomNumber", Room.class);
            query.setParameter("roomNumber", roomNumber);
            Room room = query.getSingleResult();
            return new RoomDTO(room);
        }
    }

    public RoomWithHostDTO create(RoomDTO roomDTO, PlayerDTO hostDTO) {
        try (var em = emf.createEntityManager()) {
            em.getTransaction().begin();

            Room room = new Room(roomDTO);
            room.initializeNumbers();
            room.setRoomNumber(generateUniqueRoomNumber());
            em.persist(room);

            Player host = new Player(hostDTO);
            host.setRoom(room); // Set the association
            host.setBoard(BoardUtils.serializeBoard(BingoBoardGenerator.generateBoard(null)));
            em.persist(host);

            room.setHost(host); // Update room with the host
            em.merge(room);

            em.getTransaction().commit();

            return new RoomWithHostDTO(new RoomDTO(room), new PlayerDTO(host));
        }
    }

    public void deleteRoom(int roomNumber) {
        try (var em = emf.createEntityManager()) {
            em.getTransaction().begin();

            Room room = em.createQuery("SELECT r FROM Room r WHERE r.roomNumber = :roomNumber", Room.class)
                    .setParameter("roomNumber", roomNumber)
                    .getSingleResult();

            em.remove(room);

            em.getTransaction().commit();
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

    public RoomWithHostDTO addPlayerToRoom(int roomNumber, PlayerDTO playerDTO) {
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
            return new RoomWithHostDTO(new RoomDTO(room), new PlayerDTO(newPlayer));
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

    public boolean checkWinner(int roomNumber, int playerId) {
        try (var em = emf.createEntityManager()) {
            em.getTransaction().begin();

            // Fetch the room
            Room room = em.createQuery("SELECT r FROM Room r WHERE r.roomNumber = :roomNumber", Room.class)
                    .setParameter("roomNumber", roomNumber)
                    .getSingleResult();

            // Fetch the player
            Player player = em.find(Player.class, playerId);

            if (player == null || room == null) {
                throw new IllegalArgumentException("Room or player not found.");
            }

            // Parse the player's board
            ObjectMapper mapper = new ObjectMapper();
            String[][] board = mapper.readValue(player.getBoard(), String[][].class);

            // Get the pulled numbers
            List<Integer> pulledNumbers = room.getPulledNumbers();

            // Check for a winning condition
            boolean isWinner = checkWinningCondition(board, pulledNumbers);

            em.getTransaction().commit();
            return isWinner;
        } catch (Exception e) {
            throw new RuntimeException("Error checking winner: " + e.getMessage(), e);
        }
    }

    private boolean checkWinningCondition(String[][] board, List<Integer> pulledNumbers) {
        // Check rows
        for (String[] row : board) {
            if (Arrays.stream(row).allMatch(num -> pulledNumbers.contains(Integer.parseInt(num)))) {
                return true;
            }
        }

        // Check columns
        for (int col = 0; col < board[0].length; col++) {
            boolean columnMatch = true;
            for (String[] row : board) {
                if (!pulledNumbers.contains(Integer.parseInt(row[col]))) {
                    columnMatch = false;
                    break;
                }
            }
            if (columnMatch) return true;
        }

        // Check diagonals
        boolean leftDiagonal = true;
        boolean rightDiagonal = true;
        for (int i = 0; i < board.length; i++) {
            if (!pulledNumbers.contains(Integer.parseInt(board[i][i]))) {
                leftDiagonal = false;
            }
            if (!pulledNumbers.contains(Integer.parseInt(board[i][board.length - 1 - i]))) {
                rightDiagonal = false;
            }
        }
        return leftDiagonal || rightDiagonal;
    }


}
