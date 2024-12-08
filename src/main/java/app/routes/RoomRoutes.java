package app.routes;

import app.controllers.impl.RoomController;
import io.javalin.apibuilder.EndpointGroup;

import static io.javalin.apibuilder.ApiBuilder.get;
import static io.javalin.apibuilder.ApiBuilder.post;

public class RoomRoutes {

    private final RoomController roomController = new RoomController();

    protected EndpointGroup getRoutes() {

        return () -> {
            get("/", roomController::getAll);
            post("/", roomController::create);
            post("/{roomNumber}/pull", roomController::pullNumber);
            post("/{roomNumber}/join", roomController::addPlayer);
        };
    }
}
