package io.mmo.websocket;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

@Component
@RequiredArgsConstructor
public class WebSocketHandler extends TextWebSocketHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(WebSocketHandler.class);
    private final WebSocketSessionsManager sessionsManager;

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        String username = (String) session.getAttributes().get("username");
        sessionsManager.registerSession(username, session);
        LOGGER.info("User connected: {}", username);
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, @NonNull CloseStatus status) {
        String username = (String) session.getAttributes().get("username");
        sessionsManager.removeSession(username);
        LOGGER.info("User disconnected: {}", username);
    }

    @Override
    protected void handleTextMessage(@NonNull WebSocketSession session, @NonNull TextMessage message) {
        try {
            session.sendMessage(new TextMessage("Echo: " + message.getPayload()));
        } catch (Exception e) {
            LOGGER.error("Failed to send message to '{}': {}", session.getAttributes()
                                                                      .get("username"), e.getMessage(), e);
        }
    }
}
