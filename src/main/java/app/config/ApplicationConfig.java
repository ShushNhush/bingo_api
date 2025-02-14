package app.config;

import app.daos.impl.RoomDAO;
import app.handlers.WebSocketHandler;
import app.routes.WebSocketRoute;
import app.services.RoomCleanupService;
import com.fasterxml.jackson.databind.ObjectMapper;
import app.exceptions.ApiException;
import app.routes.Routes;
import app.security.controllers.AccessController;
import app.security.controllers.SecurityController;
import app.security.enums.Role;;
import app.security.exceptions.NotAuthorizedException;
import app.security.routes.SecurityRoutes;
import app.utils.Utils;
import io.javalin.Javalin;
import io.javalin.config.JavalinConfig;
import io.javalin.http.Context;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ApplicationConfig {

    private static Routes routes = new Routes();
    private static ObjectMapper jsonMapper = new Utils().getObjectMapper();
    private static SecurityController securityController = SecurityController.getInstance();
    private static AccessController accessController = new AccessController();
    private static Logger logger = LoggerFactory.getLogger(ApplicationConfig.class);
    private static int count = 1;
    private static RoomCleanupService roomCleanupService;

    public static void configuration(JavalinConfig config) {
        config.showJavalinBanner = false;
        config.bundledPlugins.enableRouteOverview("/routes", Role.ANYONE);
        config.router.contextPath = "/api"; // base path for all endpoints
        config.router.apiBuilder(routes.getRoutes());
        config.router.apiBuilder(SecurityRoutes.getSecuredRoutes());
        config.router.apiBuilder(SecurityRoutes.getSecurityRoutes());

    }

    public static Javalin startServer(int port) {
        Javalin app = Javalin.create(ApplicationConfig::configuration);

        app.before(ApplicationConfig::corsHeaders); // First middleware for CORS
        app.beforeMatched(accessController::accessHandler); // Security handler
        app.after(ApplicationConfig::afterRequest); // Run after route execution
        app.options("/*", ApplicationConfig::corsHeadersOptions); // Preflight requests


        new WebSocketRoute().register(app);

        app.exception(ApiException.class, ApplicationConfig::apiExceptionHandler);
        app.exception(app.security.exceptions.ApiException.class, ApplicationConfig::apiSecurityExceptionHandler);
        app.exception(NotAuthorizedException.class, ApplicationConfig::apiNotAuthorizedExceptionHandler);
        app.exception(Exception.class, ApplicationConfig::generalExceptionHandler);


        // Initialize and start RoomCleanupService
        RoomDAO roomDAO = new RoomDAO();
        WebSocketHandler webSocketHandler = new WebSocketHandler();
        roomCleanupService = new RoomCleanupService(roomDAO, webSocketHandler);

        // Add shutdown hook for cleanup service and other resources
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("Shutting down application...");
            roomCleanupService.shutdown(); // Stop RoomCleanupService
        }));

        app.start(port);
        return app;
    }

    public static void afterRequest(Context ctx) {
        String requestInfo = ctx.req().getMethod() + " " + ctx.req().getRequestURI();
        logger.info(" Request {} - {} was handled with status code {}", count++, requestInfo, ctx.status());
    }

    public static void stopServer(Javalin app) {
        app.stop();
    }

    public static void apiExceptionHandler(ApiException e, Context ctx) {
        ctx.status(e.getStatusCode());
        logger.warn("An API exception occurred: Code: {}, Message: {}", e.getStatusCode(), e.getMessage());
        ctx.json(Utils.convertToJsonMessage(ctx, "warning", e.getMessage()));
    }

    public static void apiSecurityExceptionHandler(app.security.exceptions.ApiException e, Context ctx) {
        ctx.status(e.getCode());
        logger.warn("A Security API exception occurred: Code: {}, Message: {}", e.getCode(), e.getMessage());
        ctx.json(Utils.convertToJsonMessage(ctx, "warning", e.getMessage()));
    }

    public static void apiNotAuthorizedExceptionHandler(NotAuthorizedException e, Context ctx) {
        ctx.status(e.getStatusCode());
        logger.warn("A Not authorized Security API exception occurred: Code: {}, Message: {}", e.getStatusCode(), e.getMessage());
        ctx.json(Utils.convertToJsonMessage(ctx, "warning", e.getMessage()));
    }

    private static void generalExceptionHandler(Exception e, Context ctx) {
        logger.error("An unhandled exception occurred", e.getMessage());
        ctx.json(Utils.convertToJsonMessage(ctx, "error", e.getMessage()));
    }

    private static void corsHeaders(Context ctx) {
        String origin = ctx.header("Origin");
        if (origin != null && origin.equals("https://bingo.gudbergsen.com")) {
            ctx.header("Access-Control-Allow-Origin", origin); // Dynamically set origin
        }
        ctx.header("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS");
        ctx.header("Access-Control-Allow-Headers", "Content-Type, Authorization");
        ctx.header("Access-Control-Allow-Credentials", "true"); // Only if needed
    }

    private static void corsHeadersOptions(Context ctx) {
        String origin = ctx.header("Origin");
        if (origin != null && origin.equals("https://bingo.gudbergsen.com")) {
            ctx.header("Access-Control-Allow-Origin", origin);
        }
        ctx.header("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS");
        ctx.header("Access-Control-Allow-Headers", "Content-Type, Authorization");
        ctx.header("Access-Control-Allow-Credentials", "true"); // Only if needed
        ctx.status(204); // No content for preflight response
    }



}
