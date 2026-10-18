package chat.moviechat.util;

import chat.moviechat.exception.ValidationException;

/**
 * Утилита валидации пользовательского ввода.
 */
public final class ValidationUtil {

    private static final int MAX_USERNAME_LENGTH = 20;
    private static final int MAX_MESSAGE_LENGTH = 500;
    private static final int MAX_MOVIE_QUERY_LENGTH = 200;

    private ValidationUtil() {
    }

    /**
     * Проверяет корректность имени пользователя.
     * Запрещены символы '/', ':', пробелы в начале/конце, пустые имена.
     *
     * @param username имя пользователя
     * @throws ValidationException если имя некорректно
     */
    public static void validateUsername(String username) {
        if (username == null || username.isBlank()) {
            throw new ValidationException("Имя пользователя не может быть пустым");
        }
        if (username.length() > MAX_USERNAME_LENGTH) {
            throw new ValidationException(
                    "Имя пользователя длиннее " + MAX_USERNAME_LENGTH + " символов");
        }
        if (!username.equals(username.trim())) {
            throw new ValidationException("Имя не должно содержать пробелы по краям");
        }
        if (username.contains("/") || username.contains(":")) {
            throw new ValidationException("Имя не должно содержать символы '/' или ':'");
        }
        if (username.chars().anyMatch(Character::isWhitespace)) {
            throw new ValidationException("Имя не должно содержать пробелов");
        }
    }

    /**
     * Проверяет корректность текстового сообщения.
     *
     * @param message текст сообщения
     * @throws ValidationException если сообщение некорректно
     */
    public static void validateMessage(String message) {
        if (message == null || message.isBlank()) {
            throw new ValidationException("Сообщение не может быть пустым");
        }
        if (message.length() > MAX_MESSAGE_LENGTH) {
            throw new ValidationException(
                    "Сообщение длиннее " + MAX_MESSAGE_LENGTH + " символов");
        }
    }

    /**
     * Проверяет корректность поискового запроса фильма.
     *
     * @param query запрос
     * @throws ValidationException если запрос некорректен
     */
    public static void validateMovieQuery(String query) {
        if (query == null || query.isBlank()) {
            throw new ValidationException("Название фильма не может быть пустым");
        }
        if (query.length() > MAX_MOVIE_QUERY_LENGTH) {
            throw new ValidationException(
                    "Слишком длинный поисковый запрос (максимум "
                            + MAX_MOVIE_QUERY_LENGTH + " символов)");
        }
    }
}