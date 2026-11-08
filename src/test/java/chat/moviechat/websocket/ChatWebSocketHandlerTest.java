
package chat.moviechat.websocket;

import chat.moviechat.service.MovieService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.net.URI;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Тесты WebSocket-обработчика чата.
 *
 * Проверяется подключение пользователя и рассылка
 * текстовых сообщений через WebSocket.
 */
class ChatWebSocketHandlerTest {

    private ChatWebSocketHandler handler;
    private WebSocketSession session;

    /**
     * Создаёт обработчик и тестовую WebSocket-сессию.
     */
    @BeforeEach
    void setUp() throws Exception {
        MovieService movieService = mock(MovieService.class);

        handler = new ChatWebSocketHandler(
                new ObjectMapper().findAndRegisterModules(),
                movieService
        );

        session = mock(WebSocketSession.class);

        when(session.getId()).thenReturn("test-session");
        when(session.getUri()).thenReturn(
                URI.create("ws://localhost:8080/ws/chat?username=Alex")
        );
        when(session.isOpen()).thenReturn(true);
    }

    /**
     * Проверяет успешное подключение пользователя.
     */
    @Test
    @DisplayName("Пользователь успешно подключается к чату")
    void shouldConnectUser() throws Exception {
        handler.afterConnectionEstablished(session);

        assertEquals(1, handler.getClientCount());
    }

    /**
     * Проверяет получение и рассылку текстового сообщения.
     */
    @Test
    @DisplayName("Сообщение отправляется подключённым пользователям")
    void shouldBroadcastChatMessage() throws Exception {
        handler.afterConnectionEstablished(session);

        handler.handleTextMessage(
                session,
                new TextMessage("Привет")
        );

        verify(session, times(3)).sendMessage(any(TextMessage.class));

        var captor = org.mockito.ArgumentCaptor.forClass(TextMessage.class);

        verify(session, times(3)).sendMessage(captor.capture());

        boolean messageFound = captor.getAllValues().stream()
                .anyMatch(message ->
                        message.getPayload().contains("\"type\":\"CHAT\"")
                                && message.getPayload().contains("Привет")
                );

        assertTrue(messageFound);
    }

    /**
     * Проверяет удаление пользователя после отключения.
     */
    @Test
    @DisplayName("Отключившийся пользователь удаляется из списка")
    void shouldRemoveDisconnectedUser() throws Exception {
        handler.afterConnectionEstablished(session);

        handler.afterConnectionClosed(
                session,
                org.springframework.web.socket.CloseStatus.NORMAL
        );

        assertEquals(0, handler.getClientCount());
    }
}
