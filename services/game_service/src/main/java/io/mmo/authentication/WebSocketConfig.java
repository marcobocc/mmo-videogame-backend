package io.mmo.authentication;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

@Setter
@Getter
@EnableWebSocket
@Configuration
public class WebSocketConfig implements WebSocketConfigurer {

    private final WebSocketHandler handler;
    private final WebSocketProperties properties;
    private final JwtHandshakeInterceptor jwtInterceptor;

    public WebSocketConfig(WebSocketHandler handler,
                           WebSocketProperties properties,
                           JwtHandshakeInterceptor jwtInterceptor) {
        this.handler = handler;
        this.properties = properties;
        this.jwtInterceptor = jwtInterceptor;
    }

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(handler, properties.getEndpoint())
                .addInterceptors(jwtInterceptor)
                .setAllowedOrigins(properties.getAllowedOrigins().split(","));
    }
}
