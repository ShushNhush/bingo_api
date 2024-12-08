package app.dtos;

import app.entities.Room;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Getter
@AllArgsConstructor
@NoArgsConstructor
public class RoomDTO {

    private int id;

    private String rules;

    private int roomNumber;

    private boolean isWon;

    private List<Integer> availableNumbers;

    private List<Integer> pulledNumbers;

    private List<PlayerDTO> players = new ArrayList<>();;

    public RoomDTO(Room room) {
        this.id = room.getId();
        this.rules = room.getRules();
        this.roomNumber = room.getRoomNumber();
        this.isWon = room.isWon();
        this.availableNumbers = room.getAvailableNumbers();
        this.pulledNumbers = room.getPulledNumbers();
        this.players = room.getPlayers().stream()
                .map(PlayerDTO::new)
                .collect(java.util.stream.Collectors.toList());
    }
}
