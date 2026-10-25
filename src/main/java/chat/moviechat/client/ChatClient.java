
package chat.moviechat.client;

import lombok.extern.slf4j.Slf4j;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import java.util.concurrent.BlockingQueue;

/**
 * TCP-клиент чата для отправки и получения сообщений через сервер.
 */
@Slf4j
public class ChatClient {

    private static final String QUIT_MARKER = "__QUIT__";
    private static final String BROADCAST_PREFIX =
            String.valueOf((char) 1) + "BROADCAST:";

    private final String host;
    private final int port;
    private final String username;
    private final BlockingQueue<String> input;
    private final BlockingQueue<String> output;
    private volatile boolean running;

    public ChatClient(
            String host,
            int port,
            String username,
            BlockingQueue<String> input
    ) {
        this(host, port, username, input, null);
    }

    public ChatClient(
            String host,
            int port,
            String username,
            BlockingQueue<String> input,
            BlockingQueue<String> output
    ) {
        this.host = host;
        this.port = port;
        this.username = username;
        this.input = input;
        this.output = output;
    }

    public void start() {
        try (Socket socket = new Socket(host, port);
             BufferedReader reader = new BufferedReader(
                     new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
             BufferedWriter writer = new BufferedWriter(
                     new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8))) {

            running = true;

            reader.readLine();

            writer.write(username);
            writer.newLine();
            writer.flush();

            Thread readerThread = new Thread(
                    () -> receiveLoop(reader),
                    "reader-" + username
            );
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
            String message;

            try {
                message = input.take();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            }

            if (QUIT_MARKER.equals(message)) {
                writer.write("/quit");
                writer.newLine();
                writer.flush();
                return;
            }

            if (message.isBlank()) {
                continue;
            }

            writer.write(message);
            writer.newLine();
            writer.flush();
        }
    }

    private void receiveLoop(BufferedReader reader) {
        try {
            String message;

            while ((message = reader.readLine()) != null) {
                forwardMessage(message);
            }
        } catch (IOException e) {
            log.debug("Соединение закрыто для {}: {}", username, e.getMessage());
        } finally {
            running = false;
        }
    }

    private void forwardMessage(String message) {
        if (output == null) {
            log.info("[{}] {}", username, message);
            return;
        }

        if (message.startsWith(BROADCAST_PREFIX)) {
            String broadcast = message.substring(BROADCAST_PREFIX.length());
            int separator = broadcast.indexOf(':');

            if (separator >= 0) {
                String id = broadcast.substring(0, separator);
                String text = broadcast.substring(separator + 1);

                output.offer("broadcast-" + id + "\t" + text);
                return;
            }
        }

        output.offer("message-" + UUID.randomUUID() + "\t[" + username + "] " + message);
    }
}
