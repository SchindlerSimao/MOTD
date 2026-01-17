package ch.heig.motd.api;

import ch.heig.motd.controller.AuthController;
import ch.heig.motd.controller.PostController;
import ch.heig.motd.db.DbConfig;
import ch.heig.motd.repository.PostgresPostRepository;
import ch.heig.motd.repository.PostgresUserRepository;
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

        var pgUser = new PostgresUserRepository(ds);
        var pgPost = new PostgresPostRepository(ds);

        UserService userService = new UserServicePostgres(pgUser);
        PostService postService = new PostServicePostgres(pgPost, pgUser);

        var tokenStore = new ch.heig.motd.repository.TokenRevocationStore();
        AuthService authService = new AuthServiceImpl(userService, tokenStore, JwtProvider.defaultProvider());

        var app = Javalin.create(config -> {
            // default configuration
        }).start(7000);

        // controllers
        var authController = new AuthController(authService, userService);
        var postController = new PostController(postService, authService);

        // register routes centrally
        Routes.register(app, authController, postController);

        System.out.println("MOTD server started on http://localhost:7000 (useDb=true)");
    }
}
