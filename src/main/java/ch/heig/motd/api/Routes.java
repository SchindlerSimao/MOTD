package ch.heig.motd.api;

import ch.heig.motd.controller.AuthController;
import ch.heig.motd.controller.PostController;
import io.javalin.Javalin;

public final class Routes {
    private Routes() {}

    public static void register(Javalin app, AuthController authController, PostController postController) {
        AuthRoutes.register(app, authController);
        PostRoutes.register(app, postController);
    }
}
