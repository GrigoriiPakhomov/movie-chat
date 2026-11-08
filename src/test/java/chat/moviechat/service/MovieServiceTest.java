
package chat.moviechat.service;

import chat.moviechat.exception.ValidationException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Тесты сервиса поиска фильмов.
 *
 * Проверяется обработка некорректных поисковых запросов.
 */
class MovieServiceTest {

    private final MovieService movieService = new MovieService("test-api-key");

    /**
     * Проверяет отклонение пустого поискового запроса.
     */
    @Test
    @DisplayName("Пустой запрос не проходит валидацию")
    void shouldRejectBlankQuery() {
        assertThrows(
                ValidationException.class,
                () -> movieService.searchMovie(" ")
        );
    }

    /**
     * Проверяет отклонение null вместо названия фильма.
     */
    @Test
    @DisplayName("Null не проходит валидацию")
    void shouldRejectNullQuery() {
        assertThrows(
                ValidationException.class,
                () -> movieService.searchMovie(null)
        );
    }
}
