package ch.heig.motd.service;

import ch.heig.motd.auth.JwtProvider;

import java.time.Instant;
import java.util.Optional;

public interface AuthService {
    Optional<String> login(String username, String password);
    void logout(String jti, Instant until);
    boolean isTokenRevoked(String jti);
    Optional<Long> validateAndGetUserId(String token);
    JwtProvider jwtProvider();
}
