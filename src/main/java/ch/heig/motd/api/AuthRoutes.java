package ch.heig.motd.api;

import ch.heig.motd.controller.AuthController;
import io.javalin.Javalin;

/**
 * Class to register authentication-related routes.
 */
public final class AuthRoutes {
    /**
     * Private constructor to prevent instantiation.
     */
    private AuthRoutes() {}

    /**
     * Registers authentication routes.
     * @param app the Javalin application
     * @param authController the authentication controller
     */
    public static void register(Javalin app, AuthController authController, AuthMiddleware authMiddleware) {
        app.post(ApiConstants.Paths.AUTH_REGISTER, authController::register);
        app.post(ApiConstants.Paths.AUTH_LOGIN, authController::login);
        app.post(ApiConstants.Paths.AUTH_LOGOUT, authController::logout);
        app.before(ApiConstants.Paths.AUTH_LOGOUT, authMiddleware::requireAuth);
        app.before(ApiConstants.Paths.AUTH_DELETE, authMiddleware::requireAuth);
        app.delete(ApiConstants.Paths.AUTH_DELETE, authController::delete);
    }
}
