package chat.moviechat.exception;

/**
 * Исключение, когда фильм не найден в OMDb.
 */
public class MovieNotFoundException extends RuntimeException {
    public MovieNotFoundException(String query) {
        super("Фильм не найден: " + query);
    }
}