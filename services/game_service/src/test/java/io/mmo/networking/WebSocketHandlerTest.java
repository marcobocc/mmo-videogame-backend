package io.mmo.networking;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class WebSocketHandlerTest {

    private WebSocketHandler handler;
    private WebSocketSessionsManager sessionsManager;

    @BeforeEach
    void setup() {
        sessionsManager = new WebSocketSessionsManager();
        handler = new WebSocketHandler(sessionsManager);
    }

    @Test
    void afterConnectionEstablishedRegistersSession() throws Exception {
        WebSocketSession session = mock(WebSocketSession.class);
        Map<String, Object> attributes = new HashMap<>();
        attributes.put("username", "player1");
        when(session.getAttributes()).thenReturn(attributes);

        handler.afterConnectionEstablished(session);

        Optional<WebSocketSession> stored = sessionsManager.getSession("player1");
        assert (stored.isPresent());
    }

    @Test
    void afterConnectionClosedRemovesSession() throws Exception {
        WebSocketSession session = mock(WebSocketSession.class);
        Map<String, Object> attributes = new HashMap<>();
        attributes.put("username", "player2");
        when(session.getAttributes()).thenReturn(attributes);

        handler.afterConnectionEstablished(session);
        handler.afterConnectionClosed(session, CloseStatus.NORMAL);

        Optional<WebSocketSession> stored = sessionsManager.getSession("player2");
        assert (stored.isEmpty());
    }

    @Test
    void handleTextMessageEchoesMessage() throws Exception {
        WebSocketSession session = mock(WebSocketSession.class);
        Map<String, Object> attributes = new HashMap<>();
        attributes.put("username", "player3");
        when(session.getAttributes()).thenReturn(attributes);
        when(session.isOpen()).thenReturn(true);

        handler.afterConnectionEstablished(session);

        TextMessage message = new TextMessage("hello");
        handler.handleTextMessage(session, message);

        verify(session).sendMessage(new TextMessage("Echo: hello"));
    }
}
