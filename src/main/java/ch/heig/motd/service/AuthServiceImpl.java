package ch.heig.motd.service;

import ch.heig.motd.auth.JwtProvider;
import ch.heig.motd.repository.TokenStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

public class AuthServiceImpl implements AuthService {
    private static final Logger log = LoggerFactory.getLogger(AuthServiceImpl.class);

    private final UserService userService;
    private final TokenStore tokenStore;
    private final JwtProvider jwtProvider;

    public AuthServiceImpl(UserService userService, TokenStore tokenStore, JwtProvider jwtProvider) {
        this.userService = userService;
        this.tokenStore = tokenStore;
        this.jwtProvider = jwtProvider;
    }

    @Override
    public Optional<String> login(String username, String password) {
        log.info("Login attempt for {}", username);
        var ou = userService.findByUsername(username);
        if (ou.isEmpty()) { log.warn("Login failed - user not found: {}", username); return Optional.empty(); }
        var u = ou.get();
        if (!userService.verifyPassword(u, password)) { log.warn("Login failed - invalid password for {}", username); return Optional.empty(); }
        var jti = UUID.randomUUID().toString();
        String token = jwtProvider.createToken(u.getId(), u.getUsername(), jti);
        log.info("Login success for {} (jti={})", username, jti);
        return Optional.of(token);
    }

    @Override
    public void logout(String jti, Instant until) {
        log.info("Logout jti={} until={} ", jti, until);
        tokenStore.revoke(jti, until);
    }

    @Override
    public boolean isTokenRevoked(String jti) { return tokenStore.isRevoked(jti); }

    @Override
    public Optional<Long> validateAndGetUserId(String token) {
        try {
            var claims = jwtProvider.verifyToken(token);
            if (claims == null) { log.warn("Invalid token"); return Optional.empty(); }
            String jti = claims.getId();
            if (isTokenRevoked(jti)) { log.warn("Token is revoked: {}", jti); return Optional.empty(); }
            return Optional.of(Long.parseLong(claims.getSubject()));
        } catch (Exception e) {
            log.error("Error validating token", e);
            return Optional.empty();
        }
    }

    @Override
    public JwtProvider jwtProvider() { return jwtProvider; }
}
