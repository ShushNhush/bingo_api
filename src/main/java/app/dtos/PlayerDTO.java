package app.dtos;

import app.entities.Player;
import app.entities.Room;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class PlayerDTO {

    private int id;

    private String name;

    private String board;

    @JsonIgnore
    private Room room;

    private LocalDateTime lastActiveAt;

    public PlayerDTO(Player player) {
        this.id = player.getId();
        this.name = player.getName();
        this.board = player.getBoard();
        this.room = player.getRoom();
        this.lastActiveAt = player.getLastActiveAt();
    }

    public PlayerDTO(int id, String name) {
        this.id = id;
        this.name = name;
    }
}
