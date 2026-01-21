package ch.heig.motd.service;

import ch.heig.motd.auth.JwtProviderInterface;
import ch.heig.motd.model.User;
import ch.heig.motd.repository.TokenStore;
import com.auth0.jwt.interfaces.DecodedJWT;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class AuthServiceImplTest {
    private UserService userService;
    private TokenStore tokenStore;
    private JwtProviderInterface jwtProvider;
    private AuthServiceImpl authService;

    @BeforeEach
    public void setup() {
        userService = mock(UserService.class);
        tokenStore = mock(TokenStore.class);
        jwtProvider = mock(JwtProviderInterface.class);
        authService = new AuthServiceImpl(userService, tokenStore, jwtProvider);
    }

    @Test
    public void login_userNotFound_returnsEmpty() {
        when(userService.findByUsername("nope")).thenReturn(Optional.empty());

        Optional<String> res = authService.login("nope", "x");

        assertTrue(res.isEmpty());
        verify(userService).findByUsername("nope");
    }

    @Test
    public void login_badPassword_returnsEmpty() {
        User u = new User(1L, "bob", "hash", Instant.now());
        when(userService.findByUsername("bob")).thenReturn(Optional.of(u));
        when(userService.verifyPassword(u, "wrong")).thenReturn(false);

        Optional<String> res = authService.login("bob", "wrong");

        assertTrue(res.isEmpty());
        verify(userService).verifyPassword(u, "wrong");
    }

    @Test
    public void login_success_returnsToken() {
        User u = new User(1L, "alice", "hash", Instant.now());
        when(userService.findByUsername("alice")).thenReturn(Optional.of(u));
        when(userService.verifyPassword(u, "pwd")).thenReturn(true);
        when(jwtProvider.createToken(eq(1L), eq("alice"), anyString())).thenReturn("tk");

        Optional<String> res = authService.login("alice", "pwd");

        assertTrue(res.isPresent());
        assertEquals("tk", res.get());
        verify(jwtProvider).createToken(eq(1L), eq("alice"), anyString());
    }

    @Test
    public void logout_callsTokenStoreRevoke() {
        Instant until = Instant.now().plusSeconds(60);
        authService.logout("jti-1", until);
        verify(tokenStore).revoke("jti-1", until);
    }

    @Test
    public void validateAndGetUserId_invalidToken_returnsEmpty() {
        when(jwtProvider.verifyToken("bad")).thenReturn(null);

        Optional<Long> res = authService.validateAndGetUserId("bad");

        assertTrue(res.isEmpty());
    }

    @Test
    public void validateAndGetUserId_revokedToken_returnsEmpty() {
        DecodedJWT jwt = mock(DecodedJWT.class);
        when(jwt.getId()).thenReturn("jti-x");
        when(jwt.getSubject()).thenReturn("5");
        when(jwtProvider.verifyToken("tok")).thenReturn(jwt);
        when(tokenStore.isRevoked("jti-x")).thenReturn(true);

        Optional<Long> res = authService.validateAndGetUserId("tok");

        assertTrue(res.isEmpty());
        verify(tokenStore).isRevoked("jti-x");
    }

    @Test
    public void validateAndGetUserId_valid_returnsUserId() {
        DecodedJWT jwt = mock(DecodedJWT.class);
        when(jwt.getId()).thenReturn("jti-y");
        when(jwt.getSubject()).thenReturn("7");
        when(jwtProvider.verifyToken("tok2")).thenReturn(jwt);
        when(tokenStore.isRevoked("jti-y")).thenReturn(false);

        Optional<Long> res = authService.validateAndGetUserId("tok2");

        assertTrue(res.isPresent());
        assertEquals(7L, res.get());
    }
}
