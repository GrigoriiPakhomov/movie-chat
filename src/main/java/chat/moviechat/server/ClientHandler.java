package chat.moviechat.server;

import lombok.extern.slf4j.Slf4j;
import chat.moviechat.exception.MovieNotFoundException;
import chat.moviechat.exception.ValidationException;
import chat.moviechat.model.ClientSession;
import chat.moviechat.model.Movie;
import chat.moviechat.util.ConsoleColors;
import chat.moviechat.util.ValidationUtil;

import java.io.*;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

/**
 * Обработчик одного клиентского подключения: читает строки,
 * маршрутизирует сообщения и команды.
 */
@Slf4j
public class ClientHandler implements Runnable {

    private final Socket socket;
    private final ChatServer server;
    private final MessageBroadcaster broadcaster;
    private String username;
    private BufferedReader reader;
    private ClientSession session;

    public ClientHandler(Socket socket, ChatServer server) {
        this.socket = socket;
        this.server = server;
        this.broadcaster = new MessageBroadcaster(server.getClients());
    }

    @Override
    public void run() {
        try {
            openStreams();
            if (!handshake()) {
                return;
            }
            broadcastUserJoined();
            readLoop();
        } catch (IOException e) {
            log.warn("Клиент {} отключился: {}", username, e.getMessage());
        } finally {
            cleanup();
        }
    }

    private void openStreams() throws IOException {
        reader = new BufferedReader(
                new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
        BufferedWriter writer = new BufferedWriter(
                new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8));
        session = new ClientSession("", writer);
    }

    private boolean handshake() throws IOException {
        session.send("Введите имя пользователя:");
        String input = reader.readLine();
        if (input == null) {
            return false;
        }
        try {
            ValidationUtil.validateUsername(input);
            server.registerClient(input, session);
            this.username = input;
            return true;
        } catch (ValidationException | IllegalArgumentException e) {
            session.send("❌ " + e.getMessage());
            return false;
        }
    }

    /**
     * Оповещает всех клиентов о присоединении нового пользователя.
     *
     * @throws IOException при ошибке рассылки
     */
    private void broadcastUserJoined() throws IOException {
        String message = ConsoleColors.GREEN
                + username + " присоединился к чату (онлайн: " + server.getClientCount() + ")"
                + ConsoleColors.RESET;
        broadcaster.broadcast(message);
    }

    private void readLoop() throws IOException {
        String line;
        while ((line = reader.readLine()) != null) {
            handleLine(line);
        }
    }

    private void handleLine(String line) {
        if (line.isBlank()) {
            return;
        }
        try {
            if (line.startsWith("/")) {
                handleCommand(line);
            } else {
                ValidationUtil.validateMessage(line);
                broadcaster.broadcast(String.format("[%s]: %s", username, line));
            }
        } catch (ValidationException e) {
            sendPrivate("❌ " + e.getMessage());
        } catch (Exception e) {
            log.error("Ошибка обработки строки от {}", username, e);
            sendPrivate("❌ Внутренняя ошибка сервера");
        }
    }

    private void handleCommand(String line) {
        String[] parts = line.split("\\s+", 2);
        String command = parts[0].toLowerCase();
        String arg = parts.length > 1 ? parts[1] : "";

        switch (command) {
            case "/movie" -> handleMovie(arg);
            case "/msg" -> handlePrivateMessage(arg);
            case "/users" -> sendPrivate("👥 Онлайн: " + server.getClientCount());
            case "/help" -> sendPrivate("/movie <название> | /msg <имя> <текст> | /users | /quit");
            case "/quit" -> handleQuit();
            default -> sendPrivate("❌ Неизвестная команда: " + command);
        }
    }

    private void handleMovie(String query) {
        if (query.isBlank()) {
            sendPrivate("❌ Укажите название: /movie Inception");
            return;
        }
        try {
            Movie movie = server.getMovieService().searchMovie(query);
            broadcaster.broadcast(movie.toChatFormat());
        } catch (MovieNotFoundException e) {
            broadcaster.broadcast("❌ Фильм не найден: " + query);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            sendPrivate("❌ Запрос прерван");
        } catch (Exception e) {
            log.error("Ошибка поиска фильма '{}'", query, e);
            sendPrivate("❌ Не удалось получить данные о фильме");
        }
    }

    private void handlePrivateMessage(String arg) {
        String[] parts = arg.split("\\s+", 2);
        if (parts.length < 2) {
            sendPrivate("❌ Формат: /msg <имя> <текст>");
            return;
        }
        String target = parts[0];
        String text = parts[1];
        try {
            ValidationUtil.validateMessage(text);
        } catch (ValidationException e) {
            sendPrivate("❌ " + e.getMessage());
            return;
        }
        String formatted = String.format("%s[личное от %s]:%s %s",
                ConsoleColors.CYAN, username, ConsoleColors.RESET, text);
        if (broadcaster.sendTo(target, formatted)) {
            sendPrivate(String.format("%s[личное → %s]:%s %s",
                    ConsoleColors.CYAN, target, ConsoleColors.RESET, text));
        } else {
            sendPrivate("❌ Пользователь не найден: " + target);
        }
    }

    private void handleQuit() {
        try {
            session.send("👋 До встречи, " + username + "!");
            socket.close();
        } catch (IOException e) {
            log.debug("Ошибка закрытия сокета", e);
        }
    }

    private void sendPrivate(String text) {
        try {
            session.send(text);
        } catch (IOException e) {
            log.debug("Не удалось отправить сообщение клиенту {}", username);
        }
    }

    private void cleanup() {
        try {
            if (username != null) {
                server.removeClient(username);
                String message = ConsoleColors.RED
                        + username + " покинул чат (онлайн: " + server.getClientCount() + ")"
                        + ConsoleColors.RESET;
                broadcaster.broadcast(message);
            }
            if (socket != null && !socket.isClosed()) {
                socket.close();
            }
        } catch (IOException e) {
            log.debug("Ошибка при очистке сессии", e);
        }
    }
}