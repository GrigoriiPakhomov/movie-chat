package chat.moviechat.server;

import chat.moviechat.model.ClientSession;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MessageBroadcasterTest {

    @Test
    @DisplayName("Рассылка сообщения всем клиентам")
    void broadcastsMessageToAllClients() throws IOException {
        TestClientSession firstClient = new TestClientSession();
        TestClientSession secondClient = new TestClientSession();

        Map<String, ClientSession> clients = new HashMap<>();
        clients.put("Анна", firstClient);
        clients.put("Иван", secondClient);

        MessageBroadcaster broadcaster = new MessageBroadcaster(clients);

        broadcaster.broadcast("Привет!");

        assertTrue(firstClient.getMessage().endsWith("Привет!"));
        assertTrue(secondClient.getMessage().endsWith("Привет!"));
    }

    @Test
    @DisplayName("Отправка личного сообщения существующему клиенту")
    void sendsPrivateMessageToExistingClient() {
        TestClientSession client = new TestClientSession();

        Map<String, ClientSession> clients = new HashMap<>();
        clients.put("Анна", client);

        MessageBroadcaster broadcaster = new MessageBroadcaster(clients);

        assertTrue(broadcaster.sendTo("Анна", "Привет!"));
        assertEquals("Привет!", client.getMessage());
    }

    @Test
    @DisplayName("Сообщение несуществующему клиенту не отправляется")
    void doesNotSendMessageToMissingClient() {
        TestClientSession client = new TestClientSession();

        Map<String, ClientSession> clients = new HashMap<>();
        clients.put("Анна", client);

        MessageBroadcaster broadcaster = new MessageBroadcaster(clients);

        assertFalse(broadcaster.sendTo("Иван", "Привет!"));
        assertNull(client.getMessage());
    }

    private static class TestClientSession extends ClientSession {

        private String message;

        TestClientSession() {
            super(null, null);
        }

        @Override
        public synchronized void send(String message) throws IOException {
            this.message = message;
        }

        String getMessage() {
            return message;
        }
    }
}
