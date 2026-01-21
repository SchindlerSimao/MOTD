package ch.heig.motd.auth;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.auth0.jwt.interfaces.JWTVerifier;

import java.time.Instant;
import java.util.Date;

/**
 * This class provides methods to create and verify JWT tokens.
 */
public class JwtProvider {
    /**
     * The algorithm used for signing and verifying JWT tokens.
     */
    private final Algorithm algorithm;

    /**
     * Creates a new JwtProvider with the specified algorithm.
     * @param algorithm the algorithm to use for signing and verifying tokens
     */
    public JwtProvider(Algorithm algorithm) { this.algorithm = algorithm; }

    /**
     * Creates a default JwtProvider using the secret from the JWT_SECRET environment variable.
     * @return a JwtProvider instance
     * @throws IllegalStateException if the JWT_SECRET environment variable is not set
     */
    public static JwtProvider defaultProvider() {
        String secret = System.getenv("JWT_SECRET");
        if (secret == null || secret.isBlank()) {
            throw new IllegalStateException("JWT_SECRET environment variable is required but not set");
        }
        Algorithm alg = Algorithm.HMAC256(secret);
        return new JwtProvider(alg);
    }

    /**
     * Creates a JWT token with the given user ID, username, and JWT ID (jti).
     * @param userId id of the user
     * @param username user name
     * @param jti JWT ID
     * @return the generated JWT token
     */
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

    /**
     * Verifies the given JWT token and returns the decoded token if valid.
     * @param token the JWT token to verify
     * @return the decoded JWT token if valid, null otherwise
     */
    public DecodedJWT verifyToken(String token) {
        try {
            JWTVerifier verifier = JWT.require(algorithm).build();
            return verifier.verify(token);
        } catch (Exception e) {
            return null;
        }
    }
}
