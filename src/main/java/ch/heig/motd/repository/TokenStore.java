package ch.heig.motd.repository;

import java.time.Instant;

public interface TokenStore {
    void revoke(String jti, Instant until);
    boolean isRevoked(String jti);
}
