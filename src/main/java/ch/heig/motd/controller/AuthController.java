package ch.heig.motd.controller;

import ch.heig.motd.api.ApiConstants;
import ch.heig.motd.dto.Credentials;
import ch.heig.motd.model.User;
import ch.heig.motd.service.AuthService;
import ch.heig.motd.service.UserService;
import com.auth0.jwt.interfaces.DecodedJWT;
import io.javalin.http.Context;
import io.javalin.openapi.*;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.Map;
import java.util.Optional;

/**
 * Authentication controller handling user registration, login, logout, and account deletion.
 */
public class AuthController {
    /**
     * Logger instance for logging.
     */
    private static final Logger log = LoggerFactory.getLogger(AuthController.class);

    /**
     * Authentication service.
     */
    private final AuthService authService;

    /**
     * User service.
     */
    private final UserService userService;

    /**
     * Constructor.
     * @param authService authentication service
     * @param userService user service
     */
    public AuthController(AuthService authService, UserService userService) {
        this.authService = authService;
        this.userService = userService;
    }

    /**
     * Register a new user.
     * @param ctx Javalin context
     */
    @OpenApi(
        path = "/auth/register",
        methods = HttpMethod.POST,
        summary = "Register a new user",
        tags = {"Auth"},
        requestBody = @OpenApiRequestBody(content = @OpenApiContent(from = Credentials.class)),
        responses = {
            @OpenApiResponse(status = "201", description = "User created"),
            @OpenApiResponse(status = "400", description = "Bad request"),
            @OpenApiResponse(status = "409", description = "Username exists")
        }
    )
    public void register(Context ctx) {
        try {
            String body = ctx.body();
            if (StringUtils.isBlank(body)) {
                log.warn("Empty request body in register");
                ctx.status(400).json(Map.of(ApiConstants.Keys.ERROR, ApiConstants.Errors.MISSING_USERNAME_OR_PASSWORD));
                return;
            }

            Credentials credentials;
            try {
                credentials = ctx.bodyAsClass(Credentials.class);
            } catch (Exception ex) {
                log.warn("Malformed JSON in register: {}", body);
                ctx.status(400).json(Map.of(ApiConstants.Keys.ERROR, ApiConstants.Errors.MISSING_USERNAME_OR_PASSWORD));
                return;
            }

            if (credentials == null || StringUtils.isBlank(credentials.username()) || StringUtils.isBlank(credentials.password())) {
                log.warn("Missing username or password in register request, username={}", (credentials == null ? null : credentials.username()));
                ctx.status(400).json(Map.of(ApiConstants.Keys.ERROR, ApiConstants.Errors.MISSING_USERNAME_OR_PASSWORD));
                return;
            }

            try {
                User u = userService.register(credentials.username(), credentials.password());
                ctx.status(201).json(Map.of("id", u.getId(), "username", u.getUsername(), "createdAt", u.getCreatedAt().toString()));
            } catch (IllegalArgumentException e) {
                log.info("Attempt to register existing username={}", credentials.username());
                ctx.status(409).json(Map.of(ApiConstants.Keys.ERROR, ApiConstants.Errors.USERNAME_EXISTS));
            }
        } catch (Exception e) {
            log.error("Unexpected error in register", e);
            ctx.status(500).json(Map.of(ApiConstants.Keys.ERROR, ApiConstants.Errors.INTERNAL_ERROR));
        }
    }

    /**
     * Login a user.
     * @param ctx Javalin context
     */
    @OpenApi(
        path = "/auth/login",
        methods = HttpMethod.POST,
        summary = "Login",
        tags = {"Auth"},
        requestBody = @OpenApiRequestBody(content = @OpenApiContent(from = Credentials.class)),
        responses = {
            @OpenApiResponse(status = "200", description = "Login successful"),
            @OpenApiResponse(status = "400", description = "Bad request"),
            @OpenApiResponse(status = "401", description = "Invalid credentials")
        }
    )
    public void login(Context ctx) {
        try {
            Map body = ctx.bodyAsClass(Map.class);
            String username = (String) body.get(ApiConstants.Keys.USERNAME);
            String password = (String) body.get(ApiConstants.Keys.PASSWORD);
            if (username == null || password == null) {
                ctx.status(400).json(Map.of(ApiConstants.Keys.ERROR, ApiConstants.Errors.MISSING_USERNAME_OR_PASSWORD));
                return;
            }
            Optional<String> ot = authService.login(username, password);
            if (ot.isEmpty()) { ctx.status(401).json(Map.of(ApiConstants.Keys.ERROR, ApiConstants.Errors.INVALID_CREDENTIALS)); return; }
            String token = ot.get();
            ctx.status(200).json(Map.of(ApiConstants.Keys.TOKEN, token));
        } catch (Exception e) {
            log.error("Unexpected error in login", e);
            ctx.status(500).json(Map.of(ApiConstants.Keys.ERROR, ApiConstants.Errors.INTERNAL_ERROR));
        }
    }

    /**
     * Logout a user.
     * @param ctx Javalin context
     */
    @OpenApi(
        path = "/auth/logout",
        methods = HttpMethod.POST,
        summary = "Logout",
        tags = {"Auth"},
        security = @OpenApiSecurity(name = "bearerAuth"),
        responses = {
            @OpenApiResponse(status = "200", description = "Logged out"),
            @OpenApiResponse(status = "401", description = "Unauthorized")
        }
    )
    public void logout(Context ctx) {
        try {
            // The AuthMiddleware sets the "decodedJwt" and "uid" attributes when the request is authenticated.
            DecodedJWT dec = ctx.attribute("decodedJwt");
            if (dec == null) { ctx.status(401).json(Map.of(ApiConstants.Keys.ERROR, ApiConstants.Errors.MISSING_TOKEN)); return; }
            String jti = dec.getId();
            Instant exp = dec.getExpiresAt().toInstant();
            authService.logout(jti, exp);
            ctx.status(200).json(Map.of("message", "logged.out"));
        } catch (Exception e) {
            log.error("Unexpected error in logout", e);
            ctx.status(500).json(Map.of(ApiConstants.Keys.ERROR, ApiConstants.Errors.INTERNAL_ERROR));
        }
    }

    /**
     * Delete the authenticated user's account.
     * @param ctx Javalin context
     */
    @OpenApi(
        path = "/auth/delete",
        methods = HttpMethod.DELETE,
        summary = "Delete account",
        tags = {"Auth"},
        security = @OpenApiSecurity(name = "bearerAuth"),
        responses = {
            @OpenApiResponse(status = "204", description = "Account deleted"),
            @OpenApiResponse(status = "401", description = "Unauthorized")
        }
    )
    public void delete(Context ctx) {
        try {
            // Rely on requireAuth before-handler to set the "uid" attribute.
            Long uid = ctx.attribute("uid");

            // If uid is still null, authentication failed or requireAuth wasn't registered; return 401.
            if (uid == null) { ctx.status(401).json(Map.of(ApiConstants.Keys.ERROR, ApiConstants.Errors.UNAUTHORIZED)); return; }

            userService.delete(uid);
            log.info("User account deleted: userId={}", uid);
            ctx.status(204);
        } catch (Exception e) {
            log.error("Unexpected error in delete", e);
            ctx.status(500).json(Map.of(ApiConstants.Keys.ERROR, ApiConstants.Errors.INTERNAL_ERROR));
        }
    }
}
