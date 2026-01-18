package ch.heig.motd.api;

import ch.heig.motd.controller.AuthController;
import io.javalin.Javalin;

public final class AuthRoutes {
    private AuthRoutes() {}

    public static void register(Javalin app, AuthController authController) {
        app.post(ApiConstants.Paths.AUTH_REGISTER, authController::register);
        app.post(ApiConstants.Paths.AUTH_LOGIN, authController::login);
        app.post(ApiConstants.Paths.AUTH_LOGOUT, authController::logout);
        app.delete(ApiConstants.Paths.AUTH_DELETE, authController::delete);
    }
}

