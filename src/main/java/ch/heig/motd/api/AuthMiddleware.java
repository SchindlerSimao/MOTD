package ch.heig.motd.api;

import ch.heig.motd.service.AuthService;
import com.auth0.jwt.interfaces.DecodedJWT;
import io.javalin.http.Context;

import java.util.Map;
import java.util.Optional;

public class AuthMiddleware {
    private final AuthService authService;

    public AuthMiddleware(AuthService authService) {
        this.authService = authService;
    }

    public void requireAuth(Context ctx) {
        // only enforce for mutating HTTP methods
        io.javalin.http.HandlerType method = ctx.method();
        if (!(method == io.javalin.http.HandlerType.POST || method == io.javalin.http.HandlerType.PUT || method == io.javalin.http.HandlerType.DELETE)) {
            return;
        }
        String auth = ctx.header(ApiConstants.Headers.AUTHORIZATION);
        if (auth == null || !auth.startsWith(ApiConstants.Headers.BEARER_PREFIX)) {
            ctx.status(401).json(Map.of(ApiConstants.Keys.ERROR, ApiConstants.Errors.MISSING_TOKEN));
            return;
        }
        String token = auth.substring(ApiConstants.Headers.BEARER_PREFIX.length());
        Optional<DecodedJWT> decOpt = authService.validateAndGetClaims(token);
        if (decOpt.isEmpty()) {
            ctx.status(401).json(Map.of(ApiConstants.Keys.ERROR, ApiConstants.Errors.INVALID_TOKEN));
            return;
        }
        DecodedJWT dec = decOpt.get();
        ctx.attribute("decodedJwt", dec);
        ctx.attribute("uid", Long.parseLong(dec.getSubject()));
    }
}
