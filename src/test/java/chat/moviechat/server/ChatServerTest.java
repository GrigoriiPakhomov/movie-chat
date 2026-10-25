
package chat.moviechat.server;

import chat.moviechat.model.ClientSession;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.BufferedWriter;
import java.io.StringWriter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ChatServerTest {

    @Test
    @DisplayName("Регистрирует клиента и увеличивает количество участников")
    void registersClient() {
        ChatServer server = new ChatServer(0);

        server.registerClient("Гриша", createSession());

        assertEquals(1, server.getClientCount());
    }

    @Test
    @DisplayName("Не позволяет зарегистрировать двух клиентов с одинаковым именем")
    void rejectsDuplicateUsername() {
        ChatServer server = new ChatServer(0);

        server.registerClient("Гриша", createSession());

        assertThrows(
                IllegalArgumentException.class,
                () -> server.registerClient("Гриша", createSession())
        );

        assertEquals(1, server.getClientCount());
    }

    @Test
    @DisplayName("Удаляет отключившегося клиента из списка")
    void removesClient() {
        ChatServer server = new ChatServer(0);

        server.registerClient("Гриша", createSession());
        server.removeClient("Гриша");

        assertEquals(0, server.getClientCount());
    }

    private ClientSession createSession() {
        return new ClientSession("", new BufferedWriter(new StringWriter()));
    }
}
