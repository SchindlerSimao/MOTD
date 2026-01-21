package ch.heig.motd.repository;

import java.time.Instant;

/**
 * Interface for a token store to manage revoked tokens.
 */
public interface TokenStore {
    /**
     * Revokes a token until a specified instant.
     * @param jti the token's unique identifier
     * @param until the instant until which the token is revoked
     */
    void revoke(String jti, Instant until);

    /**
     * Checks if a token is revoked.
     * @param jti the token's unique identifier
     * @return true if the token is revoked, false otherwise
     */
    boolean isRevoked(String jti);
}
