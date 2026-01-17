package ch.heig.motd.service;

import ch.heig.motd.auth.JwtProvider;
import ch.heig.motd.model.User;
import ch.heig.motd.repository.TokenRevocationStore;
import com.auth0.jwt.algorithms.Algorithm;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class AuthServiceTest {
    private UserService userService;
    private TokenRevocationStore tokenStore;
    private JwtProvider jwtProvider;
    private AuthService authService;

    @BeforeEach
    public void setup() {
        userService = mock(UserService.class);
        tokenStore = new TokenRevocationStore();
        jwtProvider = new JwtProvider(Algorithm.HMAC256("test-secret"));
        authService = new AuthServiceImpl(userService, tokenStore, jwtProvider);
    }

    @Test
    public void login_userNotFound_returnsEmpty() {
        when(userService.findByUsername("alice")).thenReturn(Optional.empty());

        var result = authService.login("alice", "password");

        assertTrue(result.isEmpty());
    }

    @Test
    public void login_invalidPassword_returnsEmpty() {
        var user = new User(1L, "alice", "hashedpw", Instant.now());
        when(userService.findByUsername("alice")).thenReturn(Optional.of(user));
        when(userService.verifyPassword(user, "wrongpw")).thenReturn(false);

        var result = authService.login("alice", "wrongpw");

        assertTrue(result.isEmpty());
    }

    @Test
    public void login_validCredentials_returnsToken() {
        var user = new User(1L, "alice", "hashedpw", Instant.now());
        when(userService.findByUsername("alice")).thenReturn(Optional.of(user));
        when(userService.verifyPassword(user, "password")).thenReturn(true);

        var result = authService.login("alice", "password");

        assertTrue(result.isPresent());
        assertNotNull(result.get());
    }

    @Test
    public void logout_revokesToken() {
        String jti = "jti-123";
        Instant exp = Instant.now().plusSeconds(3600);

        authService.logout(jti, exp);

        assertTrue(tokenStore.isRevoked(jti));
    }

    @Test
    public void validateAndGetUserId_revokedToken_returnsEmpty() {
        String token = jwtProvider.createToken(42L, "alice", "jti-123");
        tokenStore.revoke("jti-123", Instant.now().plusSeconds(3600));

        var result = authService.validateAndGetUserId(token);

        assertTrue(result.isEmpty());
    }

    @Test
    public void validateAndGetUserId_validToken_returnsUserId() {
        String token = jwtProvider.createToken(42L, "alice", "jti-123");

        var result = authService.validateAndGetUserId(token);

        assertTrue(result.isPresent());
        assertEquals(42L, result.get());
    }

    @Test
    public void validateAndGetUserId_invalidToken_returnsEmpty() {
        var result = authService.validateAndGetUserId("invalid-token");

        assertTrue(result.isEmpty());
    }
}
