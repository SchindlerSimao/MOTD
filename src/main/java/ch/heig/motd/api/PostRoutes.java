package ch.heig.motd.api;

import ch.heig.motd.controller.PostController;
import io.javalin.Javalin;

public final class PostRoutes {
    private PostRoutes() {}

    public static void register(Javalin app, PostController postController) {
        app.get(ApiConstants.Paths.POSTS, postController::list);
        app.before(ApiConstants.Paths.POSTS, postController::requireAuth);
        app.before(ApiConstants.Paths.POST_WITH_ID, postController::requireAuth);
        app.post(ApiConstants.Paths.POSTS, postController::create);
        app.put(ApiConstants.Paths.POST_WITH_ID, postController::update);
        app.delete(ApiConstants.Paths.POST_WITH_ID, postController::delete);
    }
}
