package chat.moviechat.server;

import chat.moviechat.model.ClientSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Рассылает сообщения клиентам и присваивает рассылкам уникальные идентификаторы.
 */
@Slf4j
@RequiredArgsConstructor
public class MessageBroadcaster {

    private static final String BROADCAST_PREFIX =
            String.valueOf((char) 1) + "BROADCAST:";
    private static final AtomicLong BROADCAST_ID = new AtomicLong();

    private final Map<String, ClientSession> clients;

    public void broadcast(String message) {
        String packet = BROADCAST_PREFIX
                + BROADCAST_ID.incrementAndGet()
                + ":" + message;

        clients.forEach((name, session) -> {
            try {
                session.send(packet);
            } catch (IOException e) {
                log.warn("Не удалось отправить сообщение клиенту {}: {}", name, e.getMessage());
            }
        });
    }

    public boolean sendTo(String username, String message) {
        ClientSession session = clients.get(username);

        if (session == null) {
            return false;
        }

        try {
            session.send(message);
            return true;
        } catch (IOException e) {
            log.warn("Ошибка отправки сообщения клиенту {}: {}", username, e.getMessage());
            return false;
        }
    }
}
