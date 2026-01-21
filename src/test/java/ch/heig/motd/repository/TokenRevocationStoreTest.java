package ch.heig.motd.repository;

import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;

public class TokenRevocationStoreTest {
    @Test
    public void revoke_and_isRevoked_behaviour() throws InterruptedException {
        TokenRevocationStore store = new TokenRevocationStore();
        String jti = "jti-1";
        Instant until = Instant.now().plusMillis(200);
        store.revoke(jti, until);
        assertTrue(store.isRevoked(jti));
        // wait for expiration
        Thread.sleep(300);
        assertFalse(store.isRevoked(jti));
    }
}

