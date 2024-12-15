package app.services;

import app.config.HibernateConfig;
import app.daos.impl.RoomDAO;
import app.entities.Room;
import app.handlers.WebSocketHandler;
import jakarta.persistence.EntityManagerFactory;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class RoomCleanupService {
    private final RoomDAO roomDAO; // Assume a DAO for accessing Room entities
    private final EntityManagerFactory emf = HibernateConfig.getEntityManagerFactory();
    private final WebSocketHandler webSocketHandler; // For managing WebSocket sessions
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();

    public RoomCleanupService(RoomDAO roomDAO, WebSocketHandler webSocketHandler) {
        this.roomDAO = roomDAO;
        this.webSocketHandler = webSocketHandler;

        // Schedule cleanup to run every minute
        scheduler.scheduleAtFixedRate(this::cleanupInactiveRooms, 0, 1, TimeUnit.MINUTES);
    }

    private void cleanupInactiveRooms() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime inactivityThreshold = now.minusSeconds(30); // Set the inactivity threshold (e.g., 10 minutes)

        try (var em = emf.createEntityManager()) {
            em.getTransaction().begin();

            // Find inactive rooms
            List<Room> inactiveRooms = em.createQuery(
                            "SELECT r FROM Room r WHERE r.lastActiveAt < :threshold", Room.class)
                    .setParameter("threshold", inactivityThreshold)
                    .getResultList();

            for (Room room : inactiveRooms) {
                try {
                    // Notify players and close WebSocket connections
                    webSocketHandler.broadcast(room.getRoomNumber(), "room-closed",
                            Map.of("message", "Room closed due to inactivity."));

                    webSocketHandler.closeRoom(room.getRoomNumber()); // Close all WebSocket sessions for this room

                    // Delete the room
                    em.remove(room);
                } catch (Exception e) {
                    System.err.println("Error cleaning up room: " + room.getRoomNumber());
                    e.printStackTrace();
                }
            }

            em.getTransaction().commit();
        } catch (Exception e) {
            e.printStackTrace(); // Log or handle errors appropriately
        }
    }


    public void shutdown() {
        scheduler.shutdown();
    }
}
