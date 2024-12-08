package app.routes;

import io.javalin.apibuilder.EndpointGroup;

import static io.javalin.apibuilder.ApiBuilder.*;

public class Routes {

    private final RoomRoutes roomRoute = new RoomRoutes();

    public EndpointGroup getRoutes() {
        return () -> {

            path("/rooms", roomRoute.getRoutes());

        };
    }
}
