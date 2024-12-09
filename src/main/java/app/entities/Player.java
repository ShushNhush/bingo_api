package app.entities;

import app.dtos.PlayerDTO;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Entity
public class Player {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    private String name;

    @Column(columnDefinition = "TEXT")
    private String board;

    @ManyToOne(optional = false)
    private Room room;

    private LocalDateTime lastActiveAt;

    @PrePersist
    protected void onCreate() {
        lastActiveAt = LocalDateTime.now();
    }

    public Player(PlayerDTO playerDTO) {
        this.id = playerDTO.getId();
        this.name = playerDTO.getName();
        this.board = playerDTO.getBoard();
        this.room = playerDTO.getRoom();
    }

    public void updateLastActive() {
        this.lastActiveAt = LocalDateTime.now();
    }
}
