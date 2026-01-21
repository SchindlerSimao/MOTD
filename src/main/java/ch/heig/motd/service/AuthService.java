package ch.heig.motd.service;

import ch.heig.motd.auth.JwtProviderInterface;

import java.time.Instant;
import java.util.Optional;

/**
 * Service interface for authentication operations.
 */
public interface AuthService {
    /**
     * Logs in a user and returns a JWT token if successful.
     * @param username username to log in
     * @param password password to log in
     * @return an Optional containing the JWT token if login is successful, or empty if not
     */
    Optional<String> login(String username, String password);

    /**
     * Logs out a user by revoking the token with the given jti until the specified instant.
     * @param jti the token's unique identifier
     * @param until the instant until which the token is revoked
     */
    void logout(String jti, Instant until);

    /**
     * Checks if a token with the given jti is revoked.
     * @param jti the token's unique identifier
     * @return true if the token is revoked, false otherwise
     */
    boolean isTokenRevoked(String jti);

    /**
     * Validates a JWT token and returns the associated user ID if valid.
     * @param token the JWT token to validate
     * @return an Optional containing the user ID if the token is valid, or empty if not
     */
    Optional<Long> validateAndGetUserId(String token);

    /**
     * Gets the JwtProvider instance.
     * @return the JwtProvider
     */
    JwtProviderInterface jwtProvider();
}
