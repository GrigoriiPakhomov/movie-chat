
package chat.moviechat.server;

import lombok.extern.slf4j.Slf4j;
import chat.moviechat.websocket.ChatWebSocketHandler;
import org.springframework.stereotype.Component;

/**
 * Сервер многопользовательского чата.
 *
 * Использует WebSocket для взаимодействия с клиентами.
 * Управление подключениями и обмен сообщениями выполняет
 * ChatWebSocketHandler.
 *
 * Предоставляет информацию о состоянии сервера и количестве
 * подключённых пользователей.
 */
@Slf4j
@Component
public class ChatServer {

    private final ChatWebSocketHandler webSocketHandler;

    /**
     * Создаёт сервер чата.
     *
     * @param webSocketHandler обработчик WebSocket-подключений
     */
    public ChatServer(ChatWebSocketHandler webSocketHandler) {
        this.webSocketHandler = webSocketHandler;
        log.info("Сервер чата инициализирован");
    }

    /**
     * Возвращает количество подключённых пользователей.
     *
     * @return количество пользователей онлайн
     */
    public int getClientCount() {
        return webSocketHandler.getClientCount();
    }

    /**
     * Проверяет, запущен ли сервер чата.
     *
     * @return true, если сервер инициализирован
     */
    public boolean isRunning() {
        return true;
    }
}
