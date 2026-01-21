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
import ch.heig.motd.service.PostServiceImpl;
import ch.heig.motd.service.UserService;
import ch.heig.motd.service.UserServicePostgres;
import ch.heig.motd.auth.JwtProvider;
import io.javalin.Javalin;
import io.javalin.openapi.plugin.OpenApiPlugin;
import io.javalin.openapi.plugin.SecurityComponentConfiguration;
import io.javalin.openapi.plugin.swagger.SwaggerPlugin;
import io.javalin.openapi.BearerAuth;

import javax.sql.DataSource;

/**
 * Entry point of the MOTD API server.
 */
public class App {
    /**
     * Main method to start the server.
     * @param args command line arguments
     */
    public static void main(String[] args) {
        DataSource ds = DbConfig.createFromEnv();

        PostgresUserRepository pgUser = new PostgresUserRepository(ds);
        PostgresPostRepository pgPost = new PostgresPostRepository(ds);

        UserService userService = new UserServicePostgres(pgUser);
        PostService postService = new PostServiceImpl(pgPost, pgUser);

        TokenRevocationStore tokenStore = new TokenRevocationStore();
        AuthService authService = new AuthServiceImpl(userService, tokenStore, JwtProvider.defaultProvider());

        Javalin app = Javalin.create(config -> {
            config.registerPlugin(new OpenApiPlugin(pluginConfig -> {
                pluginConfig.withDocumentationPath("/openapi");
                pluginConfig.withDefinitionConfiguration((version, definition) -> {
                    definition.withOpenApiInfo(info -> info.setTitle("MOTD API"));
                    definition.withSecurity(new SecurityComponentConfiguration()
                        .withSecurityScheme("bearerAuth", new BearerAuth()));
                });
            }));
            config.registerPlugin(new SwaggerPlugin(swaggerConfig -> {
                swaggerConfig.setUiPath("/");
                swaggerConfig.setDocumentationPath("/openapi");
            }));
        }).start(7000);

        // controllers
        AuthController authController = new AuthController(authService, userService);
        PostController postController = new PostController(postService, authService);

        // middleware
        AuthMiddleware authMiddleware = new AuthMiddleware(authService);

        // register routes centrally
        Routes.register(app, authController, postController, authMiddleware);

        System.out.println("MOTD server started on http://localhost:7000 (useDb=true)");
    }
}
