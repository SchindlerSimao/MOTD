package ch.heig.motd.auth;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.interfaces.DecodedJWT;

import java.time.Instant;
import java.util.Date;

public class JwtProvider {
    private final Algorithm algorithm;

    public JwtProvider(Algorithm algorithm) { this.algorithm = algorithm; }

    public static JwtProvider defaultProvider() {
        String secret = System.getenv("JWT_SECRET");
        if (secret == null || secret.isBlank()) secret = "change-me-in-prod"; // TODO: better secret management? :)
        var alg = Algorithm.HMAC256(secret);
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
            var verifier = JWT.require(algorithm).build();
            return verifier.verify(token);
        } catch (Exception e) {
            return null;
        }
    }
}
