package io.mmo.authentication;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Setter
@Getter
@ConfigurationProperties(prefix = "websocket")
public class WebSocketProperties {
    private String endpoint;
    private String allowedOrigins;
}