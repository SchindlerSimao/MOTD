package ch.heig.motd.controller;

import ch.heig.motd.api.ApiConstants;
import ch.heig.motd.dto.Credentials;
import ch.heig.motd.model.User;
import ch.heig.motd.service.AuthService;
import ch.heig.motd.service.UserService;
import io.javalin.http.Context;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.Instant;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
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
        // important: Javalin's Context.status(...) returns the Context for chaining
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
}
