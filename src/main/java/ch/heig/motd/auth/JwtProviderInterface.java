package ch.heig.motd.auth;

import com.auth0.jwt.interfaces.DecodedJWT;

public interface JwtProviderInterface {
    String createToken(long userId, String username, String jti);
    DecodedJWT verifyToken(String token);
}
