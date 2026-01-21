package ch.heig.motd.api;

import ch.heig.motd.controller.AuthController;
import ch.heig.motd.controller.PostController;
import ch.heig.motd.db.DbConfig;
import ch.heig.motd.repository.PostgresPostRepository;
import ch.heig.motd.repository.PostgresUserRepository;
import ch.heig.motd.repository.TokenRevocationStore;
import ch.heig.motd.service.AuthService;
import ch.heig.motd.service.AuthServiceImpl;
import ch.heig.motd.service.PostService;
import ch.heig.motd.service.PostServicePostgres;
import ch.heig.motd.service.UserService;
import ch.heig.motd.service.UserServicePostgres;
import ch.heig.motd.auth.JwtProvider;
import io.javalin.Javalin;

import javax.sql.DataSource;

public class App {
    public static void main(String[] args) {
        DataSource ds = DbConfig.createFromEnv();

        PostgresUserRepository pgUser = new PostgresUserRepository(ds);
        PostgresPostRepository pgPost = new PostgresPostRepository(ds);

        UserService userService = new UserServicePostgres(pgUser);
        PostService postService = new PostServicePostgres(pgPost, pgUser);

        TokenRevocationStore tokenStore = new TokenRevocationStore();
        AuthService authService = new AuthServiceImpl(userService, tokenStore, JwtProvider.defaultProvider());

        Javalin app = Javalin.create(config -> {
            // default configuration
        }).start(7000);

        // controllers
        AuthController authController = new AuthController(authService, userService);
        PostController postController = new PostController(postService, authService);

        // register routes centrally
        Routes.register(app, authController, postController);

        System.out.println("MOTD server started on http://localhost:7000 (useDb=true)");
    }
}
