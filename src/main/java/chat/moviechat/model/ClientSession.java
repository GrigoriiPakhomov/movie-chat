package chat.moviechat.model;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.io.BufferedWriter;
import java.io.IOException;

/**
 * Сессия подключённого клиента: имя, потоки ввода-вывода.
 */
@Getter
@RequiredArgsConstructor
public class ClientSession {

    private final String username;
    private final BufferedWriter writer;

    /**
     * Отправляет строку клиенту.
     *
     * @param line текст
     * @throws IOException при ошибке записи
     */
    public void send(String line) throws IOException {
        synchronized (writer) {
            writer.write(line);
            writer.newLine();
            writer.flush();
        }
    }
}