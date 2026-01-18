package ch.heig.motd.repository;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class TokenRevocationStore implements TokenStore {
    private final Map<String, Instant> revoked = new ConcurrentHashMap<>();

    @Override
    public void revoke(String jti, Instant until) {
        revoked.put(jti, until);
    }

    @Override
    public boolean isRevoked(String jti) {
        var exp = revoked.get(jti);
        if (exp == null) return false;
        if (exp.isBefore(Instant.now())) {
            revoked.remove(jti);
            return false;
        }
        return true;
    }
}
