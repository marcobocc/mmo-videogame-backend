package io.mmo.security;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
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
        LOGGER.info("JwtHandshakeInterceptor: beforeHandshake called for URI: {}", request.getURI());

        String authHeader = request.getHeaders().getFirst("Authorization");
        if (authHeader == null) {
            LOGGER.warn("Authorization header missing");
            response.setStatusCode(HttpStatus.UNAUTHORIZED);
            return false;
        }

        if (!authHeader.startsWith("Bearer ")) {
            LOGGER.warn("Authorization header does not contain Bearer token");
            response.setStatusCode(HttpStatus.UNAUTHORIZED);
            return false;
        }

        String token = authHeader.substring(7);
        try {
            String username = jwtValidator.getUsernameFromToken(token);
            attributes.put("username", username);
            LOGGER.info("WebSocket JWT validated for user '{}'", username);
            return true;

        } catch (JwtValidationException e) {
            LOGGER.warn("JWT validation failed: {}", e.getMessage());
            response.setStatusCode(HttpStatus.UNAUTHORIZED);
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
