package app.entities;

import app.dtos.RoomDTO;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Random;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Getter
@Setter
@NoArgsConstructor
@Entity
public class Room {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    @Column(columnDefinition = "TEXT")
    private String rules;

    private int roomNumber;

    private boolean isWon;

    @ManyToOne
    private Player host;

    @ElementCollection
    private List<Integer> availableNumbers;

    @ElementCollection
    private List<Integer> pulledNumbers;

    @OneToMany(mappedBy = "room", cascade = CascadeType.REMOVE, orphanRemoval = true)
    private List<Player> players;

    private LocalDateTime lastActiveAt;

    @PrePersist
    protected void onCreate() {
        lastActiveAt = LocalDateTime.now();
    }

    public Room(RoomDTO roomDTO) {
        this.id = roomDTO.getId();
        this.rules = roomDTO.getRules();
        this.roomNumber = roomDTO.getRoomNumber();
        this.isWon = roomDTO.isWon();
        this.availableNumbers = roomDTO.getAvailableNumbers();
        this.pulledNumbers = roomDTO.getPulledNumbers();
        this.players = (roomDTO.getPlayers() != null)
                ? roomDTO.getPlayers().stream()
                .map(Player::new)
                .collect(Collectors.toList())
                : new ArrayList<>();
    }

    public void initializeNumbers() {
        availableNumbers = IntStream.rangeClosed(1, 75)
                .boxed()
                .collect(Collectors.toList());
        pulledNumbers = new ArrayList<>();
    }

    public int pullNumber() {
        if (availableNumbers.isEmpty()) {
            throw new IllegalStateException("No numbers left to pull!");
        }
        int index = new Random().nextInt(availableNumbers.size());
        int number = availableNumbers.remove(index); // Remove from availableNumbers
        pulledNumbers.add(number);   // Add to pulledNumbers
        return number;
    }

    public void addPlayer(Player player) {
        if (players == null) {
            players = new ArrayList<>();
        }
        players.add(player);
        player.setRoom(this); // Ensure consistency
    }

    public void updateLastActive() {
        this.lastActiveAt = LocalDateTime.now();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Room room = (Room) o;
        return id == room.id && roomNumber == room.roomNumber;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, roomNumber);
    }
}
