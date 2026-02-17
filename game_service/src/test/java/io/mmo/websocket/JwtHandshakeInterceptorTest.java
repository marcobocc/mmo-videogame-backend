package io.mmo.websocket;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import io.mmo.JwtProperties;
import io.mmo.JwtValidator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.web.socket.WebSocketHandler;

import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class JwtHandshakeInterceptorTest {

    private JwtHandshakeInterceptor interceptor;
    private JwtValidator jwtValidator;
    private Key key;

    @BeforeEach
    void setup() {
        JwtProperties properties = new JwtProperties();
        properties.setSecret("verylongsecuresecretkeyforjwt1234567890!");
        jwtValidator = new JwtValidator(properties);
        interceptor = new JwtHandshakeInterceptor(jwtValidator);
        key = Keys.hmacShaKeyFor(properties.getSecret().getBytes(StandardCharsets.UTF_8));
    }

    @Test
    void beforeHandshakeValidTokenAllowsConnection() {
        String token = Jwts.builder()
                           .setSubject("player1")
                           .signWith(key)
                           .compact();

        ServerHttpRequest request = mock(ServerHttpRequest.class);
        ServerHttpResponse response = mock(ServerHttpResponse.class);
        WebSocketHandler wsHandler = mock(WebSocketHandler.class);
        Map<String, Object> attributes = new HashMap<>();

        HttpHeaders headers = new HttpHeaders();
        headers.add("Authorization", "Bearer " + token);
        when(request.getHeaders()).thenReturn(headers);

        boolean result = interceptor.beforeHandshake(request, response, wsHandler, attributes);

        assertThat(result).isTrue();
        assertThat(attributes).containsKey("username");
        assertThat(attributes.get("username")).isEqualTo("player1");
    }

    @Test
    void beforeHandshakeInvalidTokenBlocksConnection() {
        ServerHttpRequest request = mock(ServerHttpRequest.class);
        ServerHttpResponse response = mock(ServerHttpResponse.class);
        WebSocketHandler wsHandler = mock(WebSocketHandler.class);
        Map<String, Object> attributes = new HashMap<>();

        HttpHeaders headers = new HttpHeaders();
        headers.add("Authorization", "Bearer invalidtoken");
        when(request.getHeaders()).thenReturn(headers);

        boolean result = interceptor.beforeHandshake(request, response, wsHandler, attributes);

        assertThat(result).isFalse();
        assertThat(attributes).doesNotContainKey("username");
        verify(response).setStatusCode(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void beforeHandshakeMissingHeaderBlocksConnection() {
        ServerHttpRequest request = mock(ServerHttpRequest.class);
        ServerHttpResponse response = mock(ServerHttpResponse.class);
        WebSocketHandler wsHandler = mock(WebSocketHandler.class);
        Map<String, Object> attributes = new HashMap<>();

        when(request.getHeaders()).thenReturn(new HttpHeaders());

        boolean result = interceptor.beforeHandshake(request, response, wsHandler, attributes);

        assertThat(result).isFalse();
        assertThat(attributes).doesNotContainKey("username");
        verify(response).setStatusCode(HttpStatus.UNAUTHORIZED);
    }
}
