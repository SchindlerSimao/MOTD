package ch.heig.motd.service;

import ch.heig.motd.auth.JwtProviderInterface;
import ch.heig.motd.repository.TokenStore;
import com.auth0.jwt.interfaces.DecodedJWT;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

/**
 * Implementation of the AuthService interface.
 */
public class AuthServiceImpl implements AuthService {
    /**
     * Logger for the AuthServiceImpl class.
     */
    private static final Logger log = LoggerFactory.getLogger(AuthServiceImpl.class);

    /**
     * User service for user-related operations.
     */
    private final UserService userService;

    /**
     * Token store for managing revoked tokens.
     */
    private final TokenStore tokenStore;

    /**
     * JWT provider for token creation and validation.
     */
    private final JwtProviderInterface jwtProvider;

    /**
     * Constructor.
     * @param userService user service
     * @param tokenStore token store
     * @param jwtProvider JWT provider
     */
    public AuthServiceImpl(UserService userService, TokenStore tokenStore, JwtProviderInterface jwtProvider) {
        this.userService = userService;
        this.tokenStore = tokenStore;
        this.jwtProvider = jwtProvider;
    }

    @Override
    public Optional<String> login(String username, String password) {
        log.info("Login attempt for {}", username);
        Optional<ch.heig.motd.model.User> ou = userService.findByUsername(username);
        if (ou.isEmpty()) { log.warn("Login failed - user not found: {}", username); return Optional.empty(); }
        ch.heig.motd.model.User u = ou.get();
        if (!userService.verifyPassword(u, password)) { log.warn("Login failed - invalid password for {}", username); return Optional.empty(); }
        String jti = UUID.randomUUID().toString();
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
        return validateAndGetClaims(token).map(claims -> Long.parseLong(claims.getSubject()));
    }

    @Override
    public Optional<DecodedJWT> validateAndGetClaims(String token) {
        try {
            DecodedJWT claims = jwtProvider.verifyToken(token);
            if (claims == null) { log.warn("Invalid token"); return Optional.empty(); }
            String jti = claims.getId();
            if (isTokenRevoked(jti)) { log.warn("Token is revoked: {}", jti); return Optional.empty(); }
            return Optional.of(claims);
        } catch (Exception e) {
            log.error("Error validating token", e);
            return Optional.empty();
        }
    }

    @Override
    public JwtProviderInterface jwtProvider() { return jwtProvider; }
}
