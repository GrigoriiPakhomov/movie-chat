
package chat.moviechat.util;

import chat.moviechat.exception.ValidationException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ValidationUtilTest {

    @Test
    @DisplayName("Принимает корректное имя пользователя")
    void acceptsValidUsername() {
        assertDoesNotThrow(() -> ValidationUtil.validateUsername("Гриша"));
    }

    @Test
    @DisplayName("Отклоняет пустое имя пользователя")
    void rejectsBlankUsername() {
        assertThrows(
                ValidationException.class,
                () -> ValidationUtil.validateUsername(" ")
        );
    }

    @Test
    @DisplayName("Отклоняет имя пользователя с двоеточием")
    void rejectsUsernameWithColon() {
        assertThrows(
                ValidationException.class,
                () -> ValidationUtil.validateUsername("Гриша:")
        );
    }

    @Test
    @DisplayName("Принимает обычное текстовое сообщение")
    void acceptsValidMessage() {
        assertDoesNotThrow(() -> ValidationUtil.validateMessage("Привет всем"));
    }

    @Test
    @DisplayName("Отклоняет пустое сообщение")
    void rejectsBlankMessage() {
        assertThrows(
                ValidationException.class,
                () -> ValidationUtil.validateMessage("")
        );
    }

    @Test
    @DisplayName("Отклоняет сообщение длиннее допустимого ограничения")
    void rejectsTooLongMessage() {
        String message = "а".repeat(501);

        assertThrows(
                ValidationException.class,
                () -> ValidationUtil.validateMessage(message)
        );
    }

    @Test
    @DisplayName("Отклоняет пустой запрос названия фильма")
    void rejectsBlankMovieQuery() {
        assertThrows(
                ValidationException.class,
                () -> ValidationUtil.validateMovieQuery(" ")
        );
    }
}
