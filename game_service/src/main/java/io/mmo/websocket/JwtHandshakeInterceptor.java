package io.mmo.websocket;

import io.jsonwebtoken.Claims;
import io.mmo.JwtValidator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;

import java.util.Map;

@Component
public class JwtHandshakeInterceptor implements HandshakeInterceptor {

    private static final Logger LOGGER = LoggerFactory.getLogger(JwtHandshakeInterceptor.class);
    private final JwtValidator jwtValidator;

    public JwtHandshakeInterceptor(JwtValidator jwtValidator) {
        this.jwtValidator = jwtValidator;
    }

    @Override
    public boolean beforeHandshake(ServerHttpRequest request,
                                   @NonNull ServerHttpResponse response,
                                   @NonNull WebSocketHandler wsHandler,
                                   @NonNull Map<String, Object> attributes) {

        String authHeader = request.getHeaders().getFirst("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            LOGGER.warn("Missing or invalid Authorization header");
            response.setStatusCode(org.springframework.http.HttpStatus.UNAUTHORIZED);
            return false;
        }

        String token = authHeader.substring(7);
        try {
            Claims claims = jwtValidator.validateToken(token);
            String username = claims.getSubject();
            attributes.put("username", username);
            LOGGER.info("WebSocket JWT validated for user '{}'", username);
            return true;
        } catch (Exception e) {
            LOGGER.warn("JWT validation failed: {}", e.getMessage());
            response.setStatusCode(org.springframework.http.HttpStatus.UNAUTHORIZED);
            return false;
        }
    }

    @Override
    public void afterHandshake(@NonNull ServerHttpRequest request,
                               @NonNull ServerHttpResponse response,
                               @NonNull WebSocketHandler wsHandler,
                               Exception exception) {
    }
}
