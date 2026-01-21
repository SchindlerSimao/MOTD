package ch.heig.motd.api;

import ch.heig.motd.controller.AuthController;
import ch.heig.motd.controller.PostController;
import io.javalin.Javalin;

/**
 * This class registers all the routes for the application.
 */
public final class Routes {
    /**
     * Private constructor to prevent instantiation.
     */
    private Routes() {}

    /**
     * Registers all routes with the given Javalin app and controllers.
     * @param app the Javalin application
     * @param authController the controller handling authentication
     * @param postController the controller handling post operations
     */
    public static void register(Javalin app, AuthController authController, PostController postController, AuthMiddleware authMiddleware) {
        AuthRoutes.register(app, authController, authMiddleware);
        PostRoutes.register(app, postController, authMiddleware);
    }
}
