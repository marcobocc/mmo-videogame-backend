package io.mmo.websocket;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.socket.WebSocketSession;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class WebSocketSessionsManagerTest {

    private WebSocketSessionsManager sessionsManager;

    @BeforeEach
    void setup() {
        sessionsManager = new WebSocketSessionsManager();
    }

    @Test
    void registersAndGetSession() {
        WebSocketSession session = mock(WebSocketSession.class);
        String username = "player1";

        sessionsManager.registerSession(username, session);

        Optional<WebSocketSession> retrieved = sessionsManager.getSession(username);
        assertThat(retrieved).isPresent();
        assertThat(retrieved.get()).isEqualTo(session);
    }

    @Test
    void removesSession() {
        WebSocketSession session = mock(WebSocketSession.class);
        String username = "player2";

        sessionsManager.registerSession(username, session);
        sessionsManager.removeSession(username);

        Optional<WebSocketSession> retrieved = sessionsManager.getSession(username);
        assertThat(retrieved).isEmpty();
    }

    @Test
    void getSessionReturnsEmptyForUnknownPlayer() {
        Optional<WebSocketSession> retrieved = sessionsManager.getSession("unknown");
        assertThat(retrieved).isEmpty();
    }
}
