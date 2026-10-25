
package chat.moviechat;

import chat.moviechat.runner.ChatRunner;

/**
 * Точка входа в приложение Movie Chat.
 */
public final class ChatApplication {

    private ChatApplication() {
    }

    public static void main(String[] args) {
        new ChatRunner().run(args);
    }
}
