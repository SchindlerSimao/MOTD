package ch.heig.motd.repository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;

public class TokenRevocationStoreTest {
    private TokenRevocationStore store;

    @BeforeEach
    public void setup() {
        store = new TokenRevocationStore();
    }

    @Test
    public void isRevoked_notRevokedToken_returnsFalse() {
        assertFalse(store.isRevoked("jti-123"));
    }

    @Test
    public void isRevoked_revokedToken_returnsTrue() {
        Instant future = Instant.now().plusSeconds(3600);
        store.revoke("jti-123", future);

        assertTrue(store.isRevoked("jti-123"));
    }

    @Test
    public void isRevoked_expiredToken_returnsFalseAndRemovesIt() {
        Instant past = Instant.now().minusSeconds(3600);
        store.revoke("jti-123", past);

        assertFalse(store.isRevoked("jti-123"));
        assertFalse(store.isRevoked("jti-123"));
    }

    @Test
    public void revoke_multipleTokens_tracksIndependently() {
        Instant future = Instant.now().plusSeconds(3600);
        store.revoke("jti-1", future);
        store.revoke("jti-2", future);

        assertTrue(store.isRevoked("jti-1"));
        assertTrue(store.isRevoked("jti-2"));
        assertFalse(store.isRevoked("jti-3"));
    }
}
