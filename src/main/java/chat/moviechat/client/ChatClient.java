package chat.moviechat.client;

import lombok.extern.slf4j.Slf4j;

import java.io.*;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.BlockingQueue;

/**
 * Консольный TCP-клиент чата.
 * .
 */
@Slf4j
public class ChatClient {

    private final String host;
    private final int port;
    private final String username;
    private final BlockingQueue<String> input;
    private volatile boolean running;

    public ChatClient(String host, int port, String username, BlockingQueue<String> input) {
        this.host = host;
        this.port = port;
        this.username = username;
        this.input = input;
    }

    /**
     * Подключается к серверу и запускает цикл обмена.
     * Блокируется до отключения клиента.
     */
    public void start() {
        try (Socket socket = new Socket(host, port);
             BufferedReader reader = new BufferedReader(
                     new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
             BufferedWriter writer = new BufferedWriter(
                     new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8))) {

            running = true;

            String welcome = reader.readLine();
            if (welcome != null) {
                System.out.println("[" + username + "] " + welcome);
            }

            writer.write(username);
            writer.newLine();
            writer.flush();

            Thread readerThread = new Thread(() -> receiveLoop(reader),
                    "reader-" + username);
            readerThread.setDaemon(true);
            readerThread.start();

            sendLoop(writer);
        } catch (IOException e) {
            log.error("Ошибка подключения к {}:{} для {}", host, port, username, e);
        } finally {
            running = false;
        }
    }

    private void sendLoop(BufferedWriter writer) throws IOException {
        while (running) {
            String line;
            try {
                line = input.take();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            }
            if ("__QUIT__".equals(line)) {
                writer.write("/quit");
                writer.newLine();
                writer.flush();
                return;
            }
            if (line.isBlank()) {
                continue;
            }
            writer.write(line);
            writer.newLine();
            writer.flush();
        }
    }

    private void receiveLoop(BufferedReader reader) {
        try {
            String line;
            while ((line = reader.readLine()) != null) {
                System.out.println("[" + username + "] " + line);
            }
        } catch (IOException e) {
            log.debug("Соединение закрыто для {}: {}", username, e.getMessage());
        } finally {
            running = false;
        }
    }
}