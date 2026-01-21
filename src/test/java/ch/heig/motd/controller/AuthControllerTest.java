package ch.heig.motd.controller;

import ch.heig.motd.api.ApiConstants;
import ch.heig.motd.dto.Credentials;
import ch.heig.motd.service.AuthService;
import ch.heig.motd.service.UserService;
import ch.heig.motd.auth.JwtProvider;
import ch.heig.motd.api.AuthMiddleware;
import com.auth0.jwt.interfaces.DecodedJWT;
import io.javalin.http.Context;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Date;
import java.util.Map;
import java.util.Optional;

import static org.mockito.Mockito.*;

public class AuthControllerTest {
    private AuthService authService;
    private UserService userService;
    private AuthController controller;
    private Context ctx;

    @BeforeEach
    public void setup() {
        authService = mock(AuthService.class);
        userService = mock(UserService.class);
        controller = new AuthController(authService, userService);
        ctx = mock(Context.class);
        when(ctx.status(anyInt())).thenReturn(ctx);
    }

    @Test
    public void register_missingBody_returns400() {
        when(ctx.body()).thenReturn("");

        controller.register(ctx);

        verify(ctx).status(400);
        verify(ctx).json(argThat(obj -> ((Map) obj).get(ApiConstants.Keys.ERROR).equals(ApiConstants.Errors.MISSING_USERNAME_OR_PASSWORD)));
    }

    @Test
    public void register_existingUser_returns409() {
        when(ctx.body()).thenReturn("{\"username\": \"bob\", \"password\": \"x\"}");
        when(ctx.bodyAsClass(Credentials.class)).thenReturn(new Credentials("bob", "x"));
        when(userService.register("bob", "x")).thenThrow(new IllegalArgumentException("username.exists"));

        controller.register(ctx);

        verify(ctx).status(409);
        verify(ctx).json(argThat(obj -> ((Map) obj).get(ApiConstants.Keys.ERROR).equals(ApiConstants.Errors.USERNAME_EXISTS)));
    }

    @Test
    public void login_invalidCredentials_returns401() {
        when(ctx.bodyAsClass(Map.class)).thenReturn(Map.of(ApiConstants.Keys.USERNAME, "alice", ApiConstants.Keys.PASSWORD, "bad"));
        when(authService.login("alice", "bad")).thenReturn(Optional.empty());

        controller.login(ctx);

        verify(ctx).status(401);
        verify(ctx).json(argThat(obj -> ((Map) obj).get(ApiConstants.Keys.ERROR).equals(ApiConstants.Errors.INVALID_CREDENTIALS)));
    }

    @Test
    public void login_valid_returns200() {
        when(ctx.bodyAsClass(Map.class)).thenReturn(Map.of(ApiConstants.Keys.USERNAME, "alice", ApiConstants.Keys.PASSWORD, "good"));
        when(authService.login("alice", "good")).thenReturn(Optional.of("tok-1"));

        controller.login(ctx);

        verify(ctx).status(200);
        verify(ctx).json(argThat(obj -> ((Map) obj).get(ApiConstants.Keys.TOKEN).equals("tok-1")));
    }

    @Test
    public void logout_missingToken_returns401() {
        when(ctx.header(ApiConstants.Headers.AUTHORIZATION)).thenReturn(null);

        controller.logout(ctx);

        verify(ctx).status(401);
        verify(ctx).json(argThat(obj -> ((Map) obj).get(ApiConstants.Keys.ERROR).equals(ApiConstants.Errors.MISSING_TOKEN)));
    }

    @Test
    public void logout_invalidToken_returns401() {
        when(ctx.header(ApiConstants.Headers.AUTHORIZATION)).thenReturn(ApiConstants.Headers.BEARER_PREFIX + "bad");
        JwtProvider jwtProv = mock(JwtProvider.class);
        when(authService.jwtProvider()).thenReturn(jwtProv);
        when(jwtProv.verifyToken("bad")).thenReturn(null);

        controller.logout(ctx);

        verify(ctx).status(401);
        verify(ctx).json(argThat(obj -> ((Map) obj).get(ApiConstants.Keys.ERROR).equals(ApiConstants.Errors.INVALID_TOKEN)));
    }

    @Test
    public void logout_validToken_revokes_and_returns200() {
        String token = "goodTok";
        when(ctx.header(ApiConstants.Headers.AUTHORIZATION)).thenReturn(ApiConstants.Headers.BEARER_PREFIX + token);
        JwtProvider jwtProv = mock(JwtProvider.class);
        DecodedJWT dec = mock(DecodedJWT.class);
        when(authService.jwtProvider()).thenReturn(jwtProv);
        when(jwtProv.verifyToken(token)).thenReturn(dec);
        when(dec.getId()).thenReturn("jti-123");
        when(dec.getExpiresAt()).thenReturn(Date.from(Instant.now().plusSeconds(3600)));

        controller.logout(ctx);

        verify(authService).logout(eq("jti-123"), any());
        verify(ctx).status(200);
        verify(ctx).json(argThat(obj -> ((Map) obj).get("message").equals("logged.out")));
    }

    @Test
    public void delete_missingToken_returns401() {
        when(ctx.header(ApiConstants.Headers.AUTHORIZATION)).thenReturn(null);
        when(ctx.method()).thenReturn(io.javalin.http.HandlerType.DELETE);

        // use centralized middleware
        AuthMiddleware authMiddleware = new AuthMiddleware(authService);
        authMiddleware.requireAuth(ctx);

        verify(ctx).status(401);
        verify(ctx).json(argThat(obj -> ((Map) obj).get(ApiConstants.Keys.ERROR).equals(ApiConstants.Errors.MISSING_TOKEN)));
    }

    @Test
    public void delete_invalidToken_returns401() {
        when(ctx.header(ApiConstants.Headers.AUTHORIZATION)).thenReturn(ApiConstants.Headers.BEARER_PREFIX + "bad");
        when(ctx.method()).thenReturn(io.javalin.http.HandlerType.DELETE);
        when(authService.validateAndGetUserId("bad")).thenReturn(Optional.empty());

        AuthMiddleware authMiddleware = new AuthMiddleware(authService);
        authMiddleware.requireAuth(ctx);

        verify(ctx).status(401);
        verify(ctx).json(argThat(obj -> ((Map) obj).get(ApiConstants.Keys.ERROR).equals(ApiConstants.Errors.INVALID_TOKEN)));
    }

    @Test
    public void delete_validToken_deletes_and_returns204() {
        String token = "goodTok";
        when(ctx.header(ApiConstants.Headers.AUTHORIZATION)).thenReturn(ApiConstants.Headers.BEARER_PREFIX + token);
        when(ctx.method()).thenReturn(io.javalin.http.HandlerType.DELETE);
        when(authService.validateAndGetUserId(token)).thenReturn(Optional.of(123L));

        AuthMiddleware authMiddleware = new AuthMiddleware(authService);
        authMiddleware.requireAuth(ctx);
        when(ctx.attribute("uid")).thenReturn(123L);

        controller.delete(ctx);

        verify(userService).delete(123L);
        verify(ctx).status(204);
    }
}
