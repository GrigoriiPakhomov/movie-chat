package chat.moviechat.util;

import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * Утилита для получения конфигурационных параметров приложения.
 *
 */
@Slf4j
public final class ConfigUtil {

    private static final int DEFAULT_PORT = 8080;
    private static final String DEFAULT_HOST = "localhost";
    private static final Properties PROPERTIES = loadProperties();

    private ConfigUtil() {
    }

    private static Properties loadProperties() {
        Properties props = new Properties();
        try (InputStream in = ConfigUtil.class.getClassLoader()
                .getResourceAsStream("application.properties")) {
            if (in != null) {
                props.load(in);
                log.debug("Загружен application.properties");
            } else {
                log.debug("application.properties не найден в classpath");
            }
        } catch (IOException e) {
            log.warn("Не удалось загрузить application.properties: {}", e.getMessage());
        }
        return props;
    }

    /**
     * Возвращает порт сервера.
     *
     * @return порт (по умолчанию 8080)
     */
    public static int getServerPort() {
        String port = System.getProperty("chat.port",
                System.getenv().getOrDefault("CHAT_PORT", String.valueOf(DEFAULT_PORT)));
        try {
            return Integer.parseInt(port);
        } catch (NumberFormatException e) {
            log.warn("Некорректный порт '{}', используется {}", port, DEFAULT_PORT);
            return DEFAULT_PORT;
        }
    }

    /**
     * Возвращает хост сервера.
     *
     * @return хост (по умолчанию localhost)
     */
    public static String getServerHost() {
        return System.getProperty("chat.host",
                System.getenv().getOrDefault("CHAT_HOST", DEFAULT_HOST));
    }

    /**
     * Возвращает API-ключ OMDb.
     * <p>
     * Ключ ищется в следующем порядке:
     * <ol>
     *     <li>системное свойство {@code omdb.apiKey};</li>
     *     <li>переменная окружения {@code OMDB_API_KEY};</li>
     *     <li>свойство {@code omdb.apiKey} в локальном файле
     *         {@code application.properties}.</li>
     * </ol>
     * Файл {@code application.properties} с реальным ключом добавлен в
     * {@code .gitignore}
     * <p>
     * Ключ можно получить бесплатно на
     * <a href="https://www.omdbapi.com/apikey.aspx">omdbapi.com/apikey.aspx</a>.
     *
     * @return API-ключ OMDb (без пробелов по краям)
     * @throws IllegalStateException если ключ не задан ни одним из способов
     */
    public static String getOmdbApiKey() {
        String key = System.getProperty("omdb.apiKey");
        if (key == null || key.isBlank()) {
            key = System.getenv("OMDB_API_KEY");
        }
        if (key == null || key.isBlank()) {
            key = PROPERTIES.getProperty("omdb.apiKey");
        }
        if (key == null || key.isBlank()) {
            throw new IllegalStateException(
                    "OMDB_API_KEY не задан. Установите переменную окружения OMDB_API_KEY "
                            + "или создайте src/main/resources/application.properties "
                            + "(см. application.properties.example).");
        }
        return key.trim();
    }
}