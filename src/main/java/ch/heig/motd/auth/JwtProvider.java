package ch.heig.motd.auth;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.auth0.jwt.interfaces.JWTVerifier;

import java.time.Instant;
import java.util.Date;

public class JwtProvider {
    private final Algorithm algorithm;

    public JwtProvider(Algorithm algorithm) { this.algorithm = algorithm; }

    public static JwtProvider defaultProvider() {
        String secret = System.getenv("JWT_SECRET");
        if (secret == null || secret.isBlank()) {
            throw new IllegalStateException("JWT_SECRET environment variable is required but not set");
        }
        Algorithm alg = Algorithm.HMAC256(secret);
        return new JwtProvider(alg);
    }

    public String createToken(long userId, String username, String jti) {
        Instant now = Instant.now();
        Instant exp = now.plusSeconds(60L * 60L * 24L); // 24h
        return JWT.create()
                .withSubject(Long.toString(userId))
                .withClaim("username", username)
                .withIssuedAt(Date.from(now))
                .withExpiresAt(Date.from(exp))
                .withJWTId(jti)
                .sign(algorithm);
    }

    public DecodedJWT verifyToken(String token) {
        try {
            JWTVerifier verifier = JWT.require(algorithm).build();
            return verifier.verify(token);
        } catch (Exception e) {
            return null;
        }
    }
}
