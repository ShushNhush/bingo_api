package app.dtos;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Setter
@Getter
@NoArgsConstructor
public class RoomWithHostDTO {
    private RoomDTO room;
    private PlayerDTO player;

    public RoomWithHostDTO(RoomDTO room, PlayerDTO player) {
        this.room = room;
        this.player = player;
    }
}