package chat.moviechat.websocket;

import chat.moviechat.dto.ChatMessage;
import chat.moviechat.exception.MovieNotFoundException;
import chat.moviechat.model.Movie;
import chat.moviechat.service.MovieService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.ConcurrentWebSocketSessionDecorator;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Обработчик WebSocket-соединений чата.
 *
 * Отвечает за подключение и отключение пользователей,
 * получение и рассылку сообщений, обновление списка
 * пользователей онлайн и обработку команды поиска фильмов.
 */
@Slf4j
@Component
public class ChatWebSocketHandler extends TextWebSocketHandler {

    /**
     * Активные WebSocket-сессии пользователей.
     */
    private final Map<String, WebSocketSession> sessions =
            new ConcurrentHashMap<>();

    /**
     * Связь WebSocket-сессий с именами пользователей.
     */
    private final Map<String, String> usernames =
            new ConcurrentHashMap<>();

    /**
     * Связь имён пользователей с их WebSocket-сессиями.
     */
    private final Map<String, WebSocketSession> usernameToSession =
            new ConcurrentHashMap<>();

    /**
     * Сериализатор сообщений.
     */
    private final ObjectMapper objectMapper;

    /**
     * Сервис поиска фильмов.
     */
    private final MovieService movieService;

    /**
     * Создаёт обработчик WebSocket с необходимыми зависимостями.
     *
     * @param objectMapper сериализатор сообщений
     * @param movieService сервис поиска фильмов
     */
    public ChatWebSocketHandler(
            ObjectMapper objectMapper,
            MovieService movieService
    ) {
        this.objectMapper = objectMapper;
        this.movieService = movieService;
    }

    /**
     * Вызывается после успешного подключения пользователя.
     *
     * Извлекает имя пользователя из параметров подключения,
     * проверяет его корректность и уникальность, после чего
     * добавляет пользователя в список активных сессий.
     *
     * @param session WebSocket-сессия пользователя
     * @throws IOException при ошибке отправки сообщения
     */
    @Override
    public void afterConnectionEstablished(WebSocketSession session)
            throws IOException {

        String username = null;
        String rawQuery = session.getUri().getRawQuery();

        if (rawQuery != null) {
            for (String param : rawQuery.split("&")) {
                String[] parts = param.split("=", 2);

                if (parts.length == 2 && parts[0].equals("username")) {
                    username = java.net.URLDecoder.decode(
                            parts[1],
                            java.nio.charset.StandardCharsets.UTF_8
                    );
                    break;
                }
            }
        }

        if (username == null || username.isBlank()) {
            username = "Гость";
        }

        username = username.trim();

        if (username.length() > 20) {
            session.close(CloseStatus.BAD_DATA);
            return;
        }

        if (usernameToSession.containsKey(username)) {
            session.close(CloseStatus.POLICY_VIOLATION);
            return;
        }

        WebSocketSession decoratedSession =
                new ConcurrentWebSocketSessionDecorator(
                        session,
                        10_000,
                        512 * 1024
                );

        sessions.put(session.getId(), decoratedSession);
        usernames.put(session.getId(), username);
        usernameToSession.put(username, decoratedSession);

        log.info(
                "Подключился {}. Онлайн: {}",
                username,
                sessions.size()
        );

        broadcast(new ChatMessage(
                "USER_JOINED",
                username,
                null,
                LocalDateTime.now(),
                null,
                null
        ));

        broadcastUserList();
    }

    /**
     * Обрабатывает входящее текстовое сообщение пользователя.
     *
     * Сообщения, начинающиеся с команды /movie,
     * передаются обработчику поиска фильмов.
     * Остальные сообщения рассылаются всем участникам чата.
     *
     * @param session WebSocket-сессия отправителя
     * @param message полученное сообщение
     * @throws IOException при ошибке обработки сообщения
     */
    @Override
    protected void handleTextMessage(
            WebSocketSession session,
            TextMessage message
    ) throws IOException {

        String username = usernames.get(session.getId());

        if (username == null) {
            return;
        }

        String content = message.getPayload().trim();

        if (content.isBlank()) {
            return;
        }

        if (content.equals("/movie") || content.startsWith("/movie ")) {
            handleMovieCommand(username, content);
            return;
        }

        log.info("Сообщение от {}: {}", username, content);

        broadcast(new ChatMessage(
                "CHAT",
                username,
                content,
                LocalDateTime.now(),
                null,
                null
        ));
    }

    /**
     * Вызывается при закрытии WebSocket-соединения.
     *
     * Удаляет пользователя из активных сессий,
     * уведомляет остальных участников чата и обновляет
     * список пользователей онлайн.
     *
     * @param session закрытая WebSocket-сессия
     * @param status статус закрытия соединения
     */
    @Override
    public void afterConnectionClosed(
            WebSocketSession session,
            CloseStatus status
    ) {

        String username = usernames.remove(session.getId());

        sessions.remove(session.getId());

        if (username != null) {
            usernameToSession.remove(username);

            log.info(
                    "Отключился {}. Онлайн: {}",
                    username,
                    sessions.size()
            );

            broadcast(new ChatMessage(
                    "USER_LEFT",
                    username,
                    null,
                    LocalDateTime.now(),
                    null,
                    null
            ));

            broadcastUserList();
        }
    }

    /**
     * Рассылает сообщение всем подключённым пользователям.
     *
     * Если отправка конкретной сессии завершается ошибкой,
     * соответствующая сессия удаляется из активных.
     *
     * @param chatMessage сообщение для рассылки
     */
    private void broadcast(ChatMessage chatMessage) {

        try {
            String json = objectMapper.writeValueAsString(chatMessage);
            TextMessage textMessage = new TextMessage(json);

            List<String> disconnectedSessions = new ArrayList<>();

            sessions.forEach((sessionId, session) -> {
                try {
                    if (session.isOpen()) {
                        session.sendMessage(textMessage);
                    } else {
                        disconnectedSessions.add(sessionId);
                    }
                } catch (IOException e) {
                    log.warn(
                            "Ошибка отправки сообщения в сессию {}: {}",
                            sessionId,
                            e.getMessage()
                    );

                    disconnectedSessions.add(sessionId);
                }
            });

            for (String sessionId : disconnectedSessions) {
                removeSession(sessionId);
            }

        } catch (IOException e) {
            log.error("Ошибка сериализации сообщения", e);
        }
    }

    /**
     * Удаляет отключённую или недоступную сессию из активных.
     *
     * @param sessionId идентификатор WebSocket-сессии
     */
    private void removeSession(String sessionId) {

        sessions.remove(sessionId);

        String username = usernames.remove(sessionId);

        if (username != null) {
            usernameToSession.remove(username);
        }
    }

    /**
     * Рассылает всем участникам актуальный список пользователей онлайн.
     */
    private void broadcastUserList() {

        List<String> users = new ArrayList<>(usernameToSession.keySet());

        broadcast(new ChatMessage(
                "USER_LIST",
                null,
                null,
                LocalDateTime.now(),
                users,
                null
        ));
    }

    /**
     * Обрабатывает команду поиска фильма /movie.
     *
     * Выполняет поиск через MovieService и рассылает
     * найденный фильм всем участникам чата.
     *
     * @param username имя пользователя, вызвавшего команду
     * @param content полный текст команды
     */
    private void handleMovieCommand(String username, String content) {

        String query = content.substring("/movie".length()).trim();

        if (query.isBlank()) {
            sendMessageToUser(username, new ChatMessage(
                    "CHAT",
                    "Система",
                    "Использование: /movie название фильма",
                    LocalDateTime.now(),
                    null,
                    null
            ));
            return;
        }

        try {
            Movie movie = movieService.searchMovie(query);

            broadcast(new ChatMessage(
                    "MOVIE",
                    username,
                    null,
                    LocalDateTime.now(),
                    null,
                    movie
            ));

        } catch (MovieNotFoundException e) {
            sendMessageToUser(username, new ChatMessage(
                    "CHAT",
                    "Система",
                    "Фильм не найден: " + query,
                    LocalDateTime.now(),
                    null,
                    null
            ));

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();

            sendMessageToUser(username, new ChatMessage(
                    "CHAT",
                    "Система",
                    "Поиск фильма был прерван.",
                    LocalDateTime.now(),
                    null,
                    null
            ));

        } catch (IOException e) {
            log.error("Ошибка поиска фильма: {}", query, e);

            sendMessageToUser(username, new ChatMessage(
                    "CHAT",
                    "Система",
                    "Не удалось выполнить поиск фильма. Попробуйте позже.",
                    LocalDateTime.now(),
                    null,
                    null
            ));
        }
    }

    /**
     * Отправляет сообщение конкретному пользователю.
     *
     * @param username имя получателя
     * @param chatMessage отправляемое сообщение
     */
    private void sendMessageToUser(
            String username,
            ChatMessage chatMessage
    ) {

        WebSocketSession session = usernameToSession.get(username);

        if (session == null || !session.isOpen()) {
            return;
        }

        try {
            String json = objectMapper.writeValueAsString(chatMessage);
            session.sendMessage(new TextMessage(json));

        } catch (IOException e) {
            log.error(
                    "Ошибка отправки сообщения пользователю {}",
                    username,
                    e
            );
        }
    }

    /**
     * Возвращает количество подключённых пользователей.
     *
     * @return количество активных WebSocket-сессий
     */
    public int getClientCount() {
        return sessions.size();
    }
}