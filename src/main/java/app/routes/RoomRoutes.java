package app.routes;

import app.controllers.impl.RoomController;
import io.javalin.apibuilder.EndpointGroup;

import static io.javalin.apibuilder.ApiBuilder.*;

public class RoomRoutes {

    private final RoomController roomController = new RoomController();

    protected EndpointGroup getRoutes() {

        return () -> {
            get("/", roomController::getAll);
            get("/{roomNumber}", roomController::getRoom);
            post("/", roomController::create);
            post("/{roomNumber}/pull", roomController::pullNumber);
            post("/{roomNumber}/join", roomController::addPlayer);
            post("/{roomNumber}/submit", roomController::checkWinner);
            get("/{roomNumber}/players/{playerId}", roomController::getPlayerBoard);
            delete("/{roomNumber}", roomController::deleteRoom);
        };
    }
}
