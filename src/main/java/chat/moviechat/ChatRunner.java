package chat.moviechat;

import chat.moviechat.client.ChatClient;
import chat.moviechat.server.ChatServer;
import chat.moviechat.util.ConfigUtil;
import chat.moviechat.util.ValidationUtil;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * Управляет запуском сервера, клиентов и интерактивного режима чата.
 */
@Slf4j
public class ChatRunner {

    private static final String QUIT_MARKER = "__QUIT__";

    public void run(String[] args) {
        String mode = args.length > 0 ? args[0].toLowerCase() : "interactive";

        switch (mode) {
            case "server" -> new ChatServer(ConfigUtil.getServerPort()).start();
            case "client" -> runSingleClient();
            case "interactive" -> runInteractive();
            default -> log.error("Неизвестный режим: {}", mode);
        }
    }

    private void runSingleClient() {
        Scanner scanner = new Scanner(System.in);

        log.info("Введите имя пользователя:");

        if (!scanner.hasNextLine()) {
            return;
        }

        String username = scanner.nextLine().trim();

        try {
            ValidationUtil.validateUsername(username);
        } catch (RuntimeException e) {
            log.error("Некорректное имя: {}", e.getMessage());
            return;
        }

        BlockingQueue<String> inputQueue = new LinkedBlockingQueue<>();
        BlockingQueue<String> outputQueue = new LinkedBlockingQueue<>();

        startOutputPrinter(outputQueue);

        Thread clientThread = new Thread(
                () -> new ChatClient(
                        ConfigUtil.getServerHost(),
                        ConfigUtil.getServerPort(),
                        username,
                        inputQueue,
                        outputQueue
                ).start(),
                "client-" + username
        );

        clientThread.start();

        while (clientThread.isAlive() && scanner.hasNextLine()) {
            String message = scanner.nextLine();

            if ("exit".equalsIgnoreCase(message.trim())) {
                inputQueue.offer(QUIT_MARKER);
                break;
            }

            inputQueue.offer(message);
        }

        waitForClient(clientThread);
    }

    private void runInteractive() {
        startServer();

        Scanner scanner = new Scanner(System.in);
        int clientCount = readClientCount(scanner);

        if (clientCount == 0) {
            return;
        }

        List<String> usernames = readUsernames(scanner, clientCount);

        if (usernames.size() != clientCount) {
            return;
        }

        Map<String, BlockingQueue<String>> clientQueues = new LinkedHashMap<>();
        List<Thread> clientThreads = new ArrayList<>();
        BlockingQueue<String> outputQueue = new LinkedBlockingQueue<>();

        startOutputPrinter(outputQueue);

        for (String username : usernames) {
            BlockingQueue<String> inputQueue = new LinkedBlockingQueue<>();
            clientQueues.put(username, inputQueue);

            Thread thread = new Thread(
                    () -> new ChatClient(
                            ConfigUtil.getServerHost(),
                            ConfigUtil.getServerPort(),
                            username,
                            inputQueue,
                            outputQueue
                    ).start(),
                    "client-" + username
            );

            clientThreads.add(thread);
            thread.start();
        }

        printInstructions(usernames);
        readMessages(scanner, clientQueues);

        clientQueues.values().forEach(queue -> queue.offer(QUIT_MARKER));
        clientThreads.forEach(ChatRunner::waitForClient);

        log.info("Завершение работы.");
    }

    private void startServer() {
        Thread serverThread = new Thread(
                () -> new ChatServer(ConfigUtil.getServerPort()).start(),
                "server"
        );

        serverThread.setDaemon(true);
        serverThread.start();

        try {
            Thread.sleep(500);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.warn("Ожидание запуска сервера прервано");
        }
    }

    private int readClientCount(Scanner scanner) {
        log.info("Сколько клиентов создать?");

        if (!scanner.hasNextLine()) {
            return 0;
        }

        try {
            int count = Integer.parseInt(scanner.nextLine().trim());

            if (count < 1) {
                log.warn("Количество клиентов должно быть больше нуля");
                return 1;
            }

            return count;
        } catch (NumberFormatException e) {
            log.warn("Некорректное число. Будет создан один клиент.");
            return 1;
        }
    }

    private List<String> readUsernames(Scanner scanner, int count) {
        List<String> usernames = new ArrayList<>();

        for (int i = 1; i <= count; i++) {
            while (true) {
                log.info("Введите имя клиента {}:", i);

                if (!scanner.hasNextLine()) {
                    return usernames;
                }

                String username = scanner.nextLine().trim();

                try {
                    ValidationUtil.validateUsername(username);

                    if (usernames.contains(username)) {
                        log.warn("Имя '{}' уже используется", username);
                        continue;
                    }

                    usernames.add(username);
                    break;
                } catch (RuntimeException e) {
                    log.warn("Некорректное имя: {}", e.getMessage());
                }
            }
        }

        return usernames;
    }

    private void printInstructions(List<String> usernames) {
        log.info("Чат запущен. Формат: <Имя>: <сообщение>");
        log.info("Команды: exit — выход, <Имя>: /quit — отключить клиента");
        log.info("Доступные клиенты: {}", usernames);
    }

    private void readMessages(
            Scanner scanner,
            Map<String, BlockingQueue<String>> clientQueues
    ) {
        while (scanner.hasNextLine()) {
            String line = scanner.nextLine().trim();

            if ("exit".equalsIgnoreCase(line)) {
                return;
            }

            int separator = line.indexOf(':');

            if (separator < 1) {
                log.warn("Формат: <Имя>: <сообщение>");
                continue;
            }

            String username = line.substring(0, separator).trim();
            String message = line.substring(separator + 1).trim();

            BlockingQueue<String> queue = clientQueues.get(username);

            if (queue == null) {
                log.warn("Нет такого клиента: {}", username);
                continue;
            }

            queue.offer("/quit".equalsIgnoreCase(message) ? QUIT_MARKER : message);
        }
    }

    private void startOutputPrinter(BlockingQueue<String> outputQueue) {
        Thread outputThread = new Thread(() -> {
            Set<String> displayedBroadcasts = ConcurrentHashMap.newKeySet();

            while (!Thread.currentThread().isInterrupted()) {
                try {
                    String event = outputQueue.take();
                    int separator = event.indexOf('\t');

                    if (separator < 0) {
                        log.info("{}", event);
                        continue;
                    }

                    String eventId = event.substring(0, separator);
                    String message = event.substring(separator + 1);

                    if (eventId.startsWith("broadcast-")
                            && !displayedBroadcasts.add(eventId)) {
                        continue;
                    }

                    log.info("{}", message);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        }, "chat-output");

        outputThread.setDaemon(true);
        outputThread.start();
    }

    private static void waitForClient(Thread thread) {
        try {
            thread.join(2000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.warn("Ожидание завершения клиента прервано");
        }
    }
}
