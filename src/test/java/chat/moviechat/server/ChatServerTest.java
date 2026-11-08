
package chat.moviechat.server;

import chat.moviechat.websocket.ChatWebSocketHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Тесты сервера многопользовательского чата.
 *
 * Проверяется получение состояния сервера
 * и количества подключённых пользователей.
 */
class ChatServerTest {

    private ChatWebSocketHandler webSocketHandler;
    private ChatServer chatServer;

    /**
     * Создаёт тестовые зависимости перед каждым тестом.
     */
    @BeforeEach
    void setUp() {
        webSocketHandler = mock(ChatWebSocketHandler.class);
        chatServer = new ChatServer(webSocketHandler);
    }

    /**
     * Проверяет получение количества пользователей онлайн.
     */
    @Test
    @DisplayName("Возвращает количество подключённых пользователей")
    void shouldReturnClientCount() {
        when(webSocketHandler.getClientCount()).thenReturn(3);

        assertEquals(3, chatServer.getClientCount());
    }

    /**
     * Проверяет состояние запущенного сервера.
     */
    @Test
    @DisplayName("Сервер считается запущенным после инициализации")
    void shouldReturnRunningStatus() {
        assertTrue(chatServer.isRunning());
    }
}
