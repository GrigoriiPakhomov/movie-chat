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
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * Точка входа в приложение.
 *
 */
@Slf4j
public final class ChatApplication {

    private static final String QUIT_MARKER = "__QUIT__";

    private ChatApplication() {
    }

    public static void main(String[] args) {
        String mode = args.length > 0 ? args[0].toLowerCase() : "interactive";
        try {
            switch (mode) {
                case "server" -> new ChatServer(ConfigUtil.getServerPort()).start();
                case "client" -> runSingleClient();
                case "interactive" -> runInteractive();
                default -> log.error("Неизвестный режим: {}", args[0]);
            }
        } catch (Exception e) {
            log.error("Ошибка запуска приложения", e);
        }
    }

    /** Одиночный клиент — для запуска в отдельном терминале. */
    private static void runSingleClient() {
        Scanner scanner = new Scanner(System.in);
        System.out.print("Имя: ");
        String name = scanner.nextLine();
        try {
            ValidationUtil.validateUsername(name);
        } catch (Exception e) {
            System.out.println("❌ " + e.getMessage());
            return;
        }
        BlockingQueue<String> queue = new LinkedBlockingQueue<>();
        Thread clientThread = new Thread(
                () -> new ChatClient(ConfigUtil.getServerHost(), ConfigUtil.getServerPort(),
                        name, queue).start(),
                "client-" + name);
        clientThread.start();

        // main читает консоль и кладёт в очередь
        while (clientThread.isAlive() && scanner.hasNextLine()) {
            queue.offer(scanner.nextLine());
        }
        try {
            clientThread.join();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    /** Сервер + несколько клиентов в одном процессе. */
    private static void runInteractive() {
        // 1. Сервер в фоне
        Thread serverThread = new Thread(
                () -> new ChatServer(ConfigUtil.getServerPort()).start(),
                "server");
        serverThread.setDaemon(true);
        serverThread.start();
        sleepQuietly(500);

        Scanner scanner = new Scanner(System.in);

        // 2. Сколько клиентов
        System.out.print("Сколько клиентов создать? ");
        int count = parseIntSafe(scanner.nextLine(), 2);
        if (count < 1) {
            count = 1;
        }

        // 3. Имена + запуск клиентов
        Map<String, BlockingQueue<String>> queues = new LinkedHashMap<>();
        List<Thread> clientThreads = new ArrayList<>();

        for (int i = 1; i <= count; i++) {
            String name = readName(scanner, i, queues.keySet());
            if (name == null) {
                return;
            }
            BlockingQueue<String> queue = new LinkedBlockingQueue<>();
            queues.put(name, queue);
            Thread t = new Thread(
                    () -> new ChatClient(ConfigUtil.getServerHost(), ConfigUtil.getServerPort(),
                            name, queue).start(),
                    "client-" + name);
            t.start();
            clientThreads.add(t);
        }

        System.out.println();
        System.out.println("Чат запущен. Формат ввода: <Имя>: <текст>");
        System.out.println("Спец-команды: exit — выход, <Имя>: /quit — отключить клиента");
        System.out.println();

        // 4. Цикл ввода
        while (scanner.hasNextLine()) {
            String line = scanner.nextLine();
            if ("exit".equalsIgnoreCase(line.trim())) {
                break;
            }
            int colon = line.indexOf(':');
            if (colon < 0) {
                System.out.println("❌ Формат: <Имя>: <текст>  (например: Гриша: Привет)");
                continue;
            }
            String name = line.substring(0, colon).trim();
            String text = line.substring(colon + 1).trim();
            BlockingQueue<String> queue = queues.get(name);
            if (queue == null) {
                System.out.println("❌ Нет такого клиента: " + name
                        + ". Доступные: " + queues.keySet());
                continue;
            }
            if ("/quit".equalsIgnoreCase(text)) {
                queue.offer(QUIT_MARKER);
            } else {
                queue.offer(text);
            }
        }

        // 5. Дождаться клиентов
        for (Thread t : clientThreads) {
            try {
                t.join(2000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
        System.out.println("Завершение работы.");
    }

    private static String readName(Scanner scanner, int index, java.util.Set<String> taken) {
        while (true) {
            System.out.print("Имя клиента " + index + ": ");
            System.out.flush();
            if (!scanner.hasNextLine()) {
                return null;
            }
            String name = scanner.nextLine();
            try {
                ValidationUtil.validateUsername(name);
                if (taken.contains(name)) {
                    System.out.println("❌ Имя уже занято");
                    continue;
                }
                return name;
            } catch (Exception e) {
                System.out.println("❌ " + e.getMessage());
            }
        }
    }

    private static int parseIntSafe(String s, int defaultValue) {
        try {
            return Integer.parseInt(s.trim());
        } catch (Exception e) {
            return defaultValue;
        }
    }

    private static void sleepQuietly(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}