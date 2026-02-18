package io.mmo.websocket;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketSession;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class WebSocketSessionsManager {

    private static final Logger LOGGER = LoggerFactory.getLogger(WebSocketSessionsManager.class);
    private final Map<String, WebSocketSession> sessions = new ConcurrentHashMap<>();

    public void registerSession(String username, WebSocketSession session) {
        sessions.put(username, session);
        LOGGER.info("Registered session for '{}'", username);
    }

    public void removeSession(String username) {
        sessions.remove(username);
        LOGGER.info("Removed session for '{}'", username);
    }

    public Optional<WebSocketSession> getSession(String username) {
        return Optional.ofNullable(sessions.get(username));
    }
}
