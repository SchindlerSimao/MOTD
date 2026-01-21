package ch.heig.motd.api;

import ch.heig.motd.controller.PostController;
import io.javalin.Javalin;

/**
 * This class registers the routes for post-related operations.
 */
public final class PostRoutes {
    /**
     * Private constructor to prevent instantiation.
     */
    private PostRoutes() {}

    /**
     * Registers the post routes with the given Javalin app and PostController.
     * @param app the Javalin application
     * @param postController the controller handling post operations
     */
    public static void register(Javalin app, PostController postController, AuthMiddleware authMiddleware) {
        app.get(ApiConstants.Paths.POSTS, postController::list);
        app.before(ApiConstants.Paths.POSTS, authMiddleware::requireAuth);
        app.before(ApiConstants.Paths.POST_WITH_ID, authMiddleware::requireAuth);
        app.post(ApiConstants.Paths.POSTS, postController::create);
        app.put(ApiConstants.Paths.POST_WITH_ID, postController::update);
        app.delete(ApiConstants.Paths.POST_WITH_ID, postController::delete);
    }
}
