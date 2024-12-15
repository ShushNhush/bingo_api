package app.dtos;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class PlayerStatusDTO {

    private PlayerDTO player;
    private boolean isConnected;

    public PlayerStatusDTO(PlayerDTO player, boolean isConnected) {
        this.player = player;
        this.isConnected = isConnected;
    }

}
