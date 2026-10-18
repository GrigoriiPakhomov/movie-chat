package chat.moviechat.server;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import chat.moviechat.model.ClientSession;
import chat.moviechat.service.MovieService;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * TCP-сервер многопользовательского чата.
 * Принимает подключения и создаёт для каждого клиента отдельный обработчик.
 */
@Slf4j
@Getter
public class ChatServer {

    private final int port;
    private final Map<String, ClientSession> clients = new ConcurrentHashMap<>();
    private final MovieService movieService;
    private volatile boolean running;

    public ChatServer(int port) {
        this.port = port;
        this.movieService = new MovieService();
    }

    /**
     * Запускает сервер и блокируется в цикле приёма подключений.
     */
    public void start() {
        running = true;
        try (ServerSocket serverSocket = new ServerSocket(port)) {
            log.info("Сервер запущен на порту {}", port);
            while (running) {
                Socket socket = serverSocket.accept();
                log.info("Новое подключение: {}", socket.getRemoteSocketAddress());
                new Thread(new ClientHandler(socket, this)).start();
            }
        } catch (IOException e) {
            log.error("Ошибка работы сервера", e);
        }
    }

    /**
     * Регистрирует клиента в списке активных.
     *
     * @param username имя пользователя
     * @param session  сессия
     * @throws IllegalArgumentException если имя уже занято
     */
    public void registerClient(String username, ClientSession session) {
        if (clients.putIfAbsent(username, session) != null) {
            throw new IllegalArgumentException("Имя уже занято: " + username);
        }
    }

    /**
     * Удаляет клиента из списка.
     *
     * @param username имя пользователя
     */
    public void removeClient(String username) {
        clients.remove(username);
    }

    /**
     * Возвращает количество активных клиентов.
     *
     * @return количество
     */
    public int getClientCount() {
        return clients.size();
    }
}