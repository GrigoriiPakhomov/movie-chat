package chat.moviechat.exception;

/**
 * Исключение при некорректных входных данных.
 */
public class ValidationException extends RuntimeException {

    /**
     * Создаёт исключение с описанием ошибки валидации.
     *
     * @param message текст ошибки
     */
    public ValidationException(String message) {
        super(message);
    }
}