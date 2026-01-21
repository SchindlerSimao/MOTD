package ch.heig.motd.service;

import ch.heig.motd.auth.JwtProvider;
import ch.heig.motd.model.User;
import ch.heig.motd.repository.TokenStore;
import com.auth0.jwt.algorithms.Algorithm;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class AuthServiceTest {
    private UserService userService;
    private TokenStore tokenStore;
    private JwtProvider jwtProvider;
    private AuthService authService;

    @BeforeEach
    public void setup() {
        userService = mock(UserService.class);
        tokenStore = mock(TokenStore.class);
        jwtProvider = new JwtProvider(Algorithm.HMAC256("test-secret"));
        authService = new AuthServiceImpl(userService, tokenStore, jwtProvider);
    }

    @Test
    public void login_userNotFound_returnsEmpty() {
        when(userService.findByUsername("alice")).thenReturn(Optional.empty());

        Optional<String> result = authService.login("alice", "password");

        assertTrue(result.isEmpty());
    }

    @Test
    public void login_invalidPassword_returnsEmpty() {
        User user = new User(1L, "alice", "hashedpw", Instant.now());
        when(userService.findByUsername("alice")).thenReturn(Optional.of(user));
        when(userService.verifyPassword(user, "wrongpw")).thenReturn(false);

        Optional<String> result = authService.login("alice", "wrongpw");

        assertTrue(result.isEmpty());
    }

    @Test
    public void login_validCredentials_returnsToken() {
        User user = new User(1L, "alice", "hashedpw", Instant.now());
        when(userService.findByUsername("alice")).thenReturn(Optional.of(user));
        when(userService.verifyPassword(user, "password")).thenReturn(true);

        Optional<String> result = authService.login("alice", "password");

        assertTrue(result.isPresent());
        assertNotNull(result.get());
    }

    @Test
    public void logout_revokesToken() {
        String jti = "jti-123";
        Instant exp = Instant.now().plusSeconds(3600);

        authService.logout(jti, exp);

        verify(tokenStore).revoke(jti, exp);
    }

    @Test
    public void validateAndGetUserId_revokedToken_returnsEmpty() {
        String token = jwtProvider.createToken(42L, "alice", "jti-123");
        when(tokenStore.isRevoked("jti-123")).thenReturn(true);

        Optional<Long> result = authService.validateAndGetUserId(token);

        assertTrue(result.isEmpty());
    }

    @Test
    public void validateAndGetUserId_validToken_returnsUserId() {
        String token = jwtProvider.createToken(42L, "alice", "jti-123");
        when(tokenStore.isRevoked("jti-123")).thenReturn(false);

        Optional<Long> result = authService.validateAndGetUserId(token);

        assertTrue(result.isPresent());
        assertEquals(42L, result.get());
    }

    @Test
    public void validateAndGetUserId_invalidToken_returnsEmpty() {
        Optional<Long> result = authService.validateAndGetUserId("invalid-token");

        assertTrue(result.isEmpty());
    }
}
