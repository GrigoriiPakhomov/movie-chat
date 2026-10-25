package chat.moviechat.server;

import chat.moviechat.exception.MovieNotFoundException;
import chat.moviechat.exception.ValidationException;
import chat.moviechat.model.ClientSession;
import chat.moviechat.model.Movie;
import chat.moviechat.util.ConsoleColors;
import chat.moviechat.util.ValidationUtil;
import lombok.extern.slf4j.Slf4j;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

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

            if (handshake()) {
                readLoop();
            }
        } catch (IOException e) {
            log.debug("Соединение с клиентом {} закрыто: {}", username, e.getMessage());
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

            synchronized (server) {
                server.registerClient(input, session);
                username = input;

                String message = ConsoleColors.GREEN
                        + username + " присоединился к чату (онлайн: "
                        + server.getClientCount() + ")"
                        + ConsoleColors.RESET;

                broadcaster.broadcast(message);
            }

            return true;
        } catch (ValidationException | IllegalArgumentException e) {
            session.send("❌ " + e.getMessage());
            return false;
        }
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
                broadcaster.broadcast("[" + username + "]: " + line);
            }
        } catch (ValidationException e) {
            sendPrivate("❌ " + e.getMessage());
        } catch (Exception e) {
            log.error("Ошибка обработки сообщения от {}", username, e);
            sendPrivate("❌ Внутренняя ошибка сервера");
        }
    }

    private void handleCommand(String line) {
        String[] parts = line.split("\\s+", 2);
        String command = parts[0].toLowerCase();
        String argument = parts.length > 1 ? parts[1] : "";

        switch (command) {
            case "/movie" -> handleMovie(argument);
            case "/msg" -> handlePrivateMessage(argument);
            case "/users" -> sendPrivate("👥 Онлайн: " + server.getClientCount());
            case "/help" -> sendPrivate(
                    "/movie <название> | /msg <имя> <текст> | /users | /quit");
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
            sendPrivate("❌ Фильм не найден: " + query);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            sendPrivate("❌ Запрос прерван");
        } catch (Exception e) {
            log.error("Ошибка поиска фильма '{}'", query, e);
            sendPrivate("❌ Не удалось получить данные о фильме");
        }
    }

    private void handlePrivateMessage(String argument) {
        String[] parts = argument.split("\\s+", 2);

        if (parts.length < 2) {
            sendPrivate("❌ Формат: /msg <имя> <текст>");
            return;
        }

        String target = parts[0];
        String message = parts[1];

        try {
            ValidationUtil.validateMessage(message);
        } catch (ValidationException e) {
            sendPrivate("❌ " + e.getMessage());
            return;
        }

        String formatted = ConsoleColors.CYAN
                + "[личное от " + username + "]:"
                + ConsoleColors.RESET + " " + message;

        if (broadcaster.sendTo(target, formatted)) {
            sendPrivate(ConsoleColors.CYAN
                    + "[личное → " + target + "]:"
                    + ConsoleColors.RESET + " " + message);
        } else {
            sendPrivate("❌ Пользователь не найден: " + target);
        }
    }

    private void handleQuit() {
        try {
            session.send("👋 До встречи, " + username + "!");
            socket.close();
        } catch (IOException e) {
            log.debug("Не удалось закрыть соединение клиента {}", username);
        }
    }

    private void sendPrivate(String message) {
        try {
            session.send(message);
        } catch (IOException e) {
            log.debug("Не удалось отправить сообщение клиенту {}", username);
        }
    }

    private void cleanup() {
        if (username != null) {
            synchronized (server) {
                server.removeClient(username);

                String message = ConsoleColors.RED
                        + username + " покинул чат (онлайн: "
                        + server.getClientCount() + ")"
                        + ConsoleColors.RESET;

                broadcaster.broadcast(message);
            }
        }

        try {
            if (!socket.isClosed()) {
                socket.close();
            }
        } catch (IOException e) {
            log.debug("Ошибка закрытия соединения клиента {}", username);
        }
    }
}
