package chat.moviechat.server;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import chat.moviechat.model.ClientSession;

import java.io.IOException;
import java.util.Map;

/**
 * Рассылает сообщения всем подключённым клиентам.
 */
@Slf4j
@RequiredArgsConstructor
public class MessageBroadcaster {

    private final Map<String, ClientSession> clients;

    /**
     * Отправляет сообщение всем клиентам. Неактивные клиенты удаляются.
     *
     * @param message текст сообщения
     */
    public void broadcast(String message) {
        clients.forEach((name, session) -> {
            try {
                session.send(message);
            } catch (IOException e) {
                log.warn("Не удалось отправить сообщение клиенту {}: {}", name, e.getMessage());
            }
        });
    }

    /**
     * Отправляет сообщение одному клиенту.
     *
     * @param username имя получателя
     * @param message  текст
     * @return true, если отправлено
     */
    public boolean sendTo(String username, String message) {
        ClientSession session = clients.get(username);
        if (session == null) {
            return false;
        }
        try {
            session.send(message);
            return true;
        } catch (IOException e) {
            log.warn("Ошибка отправки {}: {}", username, e.getMessage());
            return false;
        }
    }
}