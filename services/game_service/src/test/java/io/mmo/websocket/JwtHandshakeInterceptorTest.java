package io.mmo.websocket;

import io.mmo.JwtValidationException;
import io.mmo.JwtValidator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.web.socket.WebSocketHandler;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class JwtHandshakeInterceptorTest {

    private JwtValidator jwtValidator;
    private JwtHandshakeInterceptor interceptor;

    @BeforeEach
    void setup() {
        jwtValidator = mock(JwtValidator.class);
        interceptor = new JwtHandshakeInterceptor(jwtValidator);
    }

    @Test
    void beforeHandshakeValidTokenAllowsConnection() throws JwtValidationException {
        String token = "validtoken";
        String username = "player1";

        // Mock JwtValidator to return the username for this token
        when(jwtValidator.getUsernameFromToken(token)).thenReturn(username);

        ServerHttpRequest request = mock(ServerHttpRequest.class);
        ServerHttpResponse response = mock(ServerHttpResponse.class);
        WebSocketHandler wsHandler = mock(WebSocketHandler.class);
        Map<String, Object> attributes = new HashMap<>();

        HttpHeaders headers = new HttpHeaders();
        headers.add("Authorization", "Bearer " + token);
        when(request.getHeaders()).thenReturn(headers);

        boolean result = interceptor.beforeHandshake(request, response, wsHandler, attributes);

        assertThat(result).isTrue();
        assertThat(attributes).containsEntry("username", username);
        verify(jwtValidator).getUsernameFromToken(token);
        verify(response, never()).setStatusCode(any());
    }

    @Test
    void beforeHandshakeInvalidTokenBlocksConnection() throws JwtValidationException {
        String token = "invalidtoken";

        // Mock JwtValidator to throw exception for this token
        when(jwtValidator.getUsernameFromToken(token))
                .thenThrow(new JwtValidationException("Invalid JWT"));

        ServerHttpRequest request = mock(ServerHttpRequest.class);
        ServerHttpResponse response = mock(ServerHttpResponse.class);
        WebSocketHandler wsHandler = mock(WebSocketHandler.class);
        Map<String, Object> attributes = new HashMap<>();

        HttpHeaders headers = new HttpHeaders();
        headers.add("Authorization", "Bearer " + token);
        when(request.getHeaders()).thenReturn(headers);

        boolean result = interceptor.beforeHandshake(request, response, wsHandler, attributes);

        assertThat(result).isFalse();
        assertThat(attributes).doesNotContainKey("username");
        verify(jwtValidator).getUsernameFromToken(token);
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
        verifyNoInteractions(jwtValidator);
        verify(response).setStatusCode(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void beforeHandshakeNonBearerHeaderBlocksConnection() {
        ServerHttpRequest request = mock(ServerHttpRequest.class);
        ServerHttpResponse response = mock(ServerHttpResponse.class);
        WebSocketHandler wsHandler = mock(WebSocketHandler.class);
        Map<String, Object> attributes = new HashMap<>();

        HttpHeaders headers = new HttpHeaders();
        headers.add("Authorization", "Token sometoken"); // Not starting with "Bearer "
        when(request.getHeaders()).thenReturn(headers);

        boolean result = interceptor.beforeHandshake(request, response, wsHandler, attributes);

        assertThat(result).isFalse();
        assertThat(attributes).doesNotContainKey("username");
        verifyNoInteractions(jwtValidator);
        verify(response).setStatusCode(HttpStatus.UNAUTHORIZED);
    }
}
