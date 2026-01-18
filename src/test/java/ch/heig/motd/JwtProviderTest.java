package ch.heig.motd;

import ch.heig.motd.auth.JwtProvider;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.interfaces.DecodedJWT;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class JwtProviderTest {

    @Test
    public void createAndVerifyToken() {
        JwtProvider p = new JwtProvider(Algorithm.HMAC256("test-secret"));
        String token = p.createToken(42L, "alice", "jti-123");
        assertNotNull(token);
        DecodedJWT dec = p.verifyToken(token);
        assertNotNull(dec);
        assertEquals("42", dec.getSubject());
        assertEquals("alice", dec.getClaim("username").asString());
        assertEquals("jti-123", dec.getId());
    }
}

