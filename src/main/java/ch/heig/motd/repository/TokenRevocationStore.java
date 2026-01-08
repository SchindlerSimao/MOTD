package ch.heig.motd.repository;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class TokenRevocationStore {
    private final Map<String, Instant> revoked = new ConcurrentHashMap<>();

    public void revoke(String jti, Instant until) {
        revoked.put(jti, until);
    }

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
