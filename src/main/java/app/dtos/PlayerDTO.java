package app.dtos;

import app.entities.Player;
import app.entities.Room;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class PlayerDTO {

    private int id;

    private String name;

    private String board;

    private Room room;

    public PlayerDTO(Player player) {
        this.id = player.getId();
        this.name = player.getName();
        this.board = player.getBoard();
        this.room = player.getRoom();
    }
}
