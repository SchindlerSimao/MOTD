package ch.heig.motd.controller;

import ch.heig.motd.api.ApiConstants;
import ch.heig.motd.dto.Credentials;
import ch.heig.motd.model.User;
import ch.heig.motd.service.AuthService;
import ch.heig.motd.service.UserService;
import com.auth0.jwt.interfaces.DecodedJWT;
import io.javalin.http.Context;
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
     * Logger for the AuthController class.
     */
    private static final Logger log = LoggerFactory.getLogger(AuthController.class);

    /**
     * Authentication service for handling auth-related operations.
     */
    private final AuthService authService;

    /**
     * User service for handling user-related operations.
     */
    private final UserService userService;

    /**
     * Constructor for AuthController.
     * @param authService authentication service
     * @param userService user service
     */
    public AuthController(AuthService authService, UserService userService) {
        this.authService = authService;
        this.userService = userService;
    }

    /**
     * Handles user registration.
     * @param ctx the Javalin context
     */
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
     * Handles user login.
     * @param ctx the Javalin context
     */
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
     * Handles user logout.
     * @param ctx the Javalin context
     */
    public void logout(Context ctx) {
        try {
            final String auth = ctx.header(ApiConstants.Headers.AUTHORIZATION);
            if (auth == null || !auth.startsWith(ApiConstants.Headers.BEARER_PREFIX)) { ctx.status(401).json(Map.of(ApiConstants.Keys.ERROR, ApiConstants.Errors.MISSING_TOKEN)); return; }
            final String token = auth.substring(ApiConstants.Headers.BEARER_PREFIX.length());
            final DecodedJWT dec = authService.jwtProvider().verifyToken(token);
            if (dec == null) { ctx.status(401).json(Map.of(ApiConstants.Keys.ERROR, ApiConstants.Errors.INVALID_TOKEN)); return; }
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
     * Handles user account deletion.
     * @param ctx the Javalin context
     */
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
