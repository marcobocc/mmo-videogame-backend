package io.mmo;

import io.mmo.websocket.JwtHandshakeInterceptor;
import io.mmo.websocket.WebSocketHandler;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

@Setter
@Getter
@EnableWebSocket
@Configuration
@ConfigurationProperties(prefix = "websocket")
public class WebSocketConfig implements WebSocketConfigurer {

    private final WebSocketHandler handler;
    private final JwtHandshakeInterceptor jwtInterceptor;

    private String endpoint;
    private String allowedOrigins;

    public WebSocketConfig(WebSocketHandler handler, JwtHandshakeInterceptor jwtInterceptor) {
        this.handler = handler;
        this.jwtInterceptor = jwtInterceptor;
    }

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(handler, endpoint)
                .addInterceptors(jwtInterceptor)
                .setAllowedOrigins(allowedOrigins.split(","));
    }
}
