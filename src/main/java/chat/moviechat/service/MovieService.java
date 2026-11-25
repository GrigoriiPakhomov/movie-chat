
package chat.moviechat.service;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import lombok.extern.slf4j.Slf4j;
import chat.moviechat.exception.MovieNotFoundException;
import chat.moviechat.model.Movie;
import chat.moviechat.util.ConfigUtil;
import chat.moviechat.util.ValidationUtil;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

/**
 * Сервис поиска фильмов через публичный OMDb API.
 *
 * Используется для получения информации о фильме:
 * название, год выпуска, жанр, рейтинг, постер и описание.
 *
 * Зарегистрирован как Spring Bean для внедрения
 * в другие компоненты приложения.
 */
@Slf4j
@Service
public class MovieService {

    private static final String OMDB_URL =
            "https://www.omdbapi.com/?t=%s&apikey=%s";

    private static final Duration TIMEOUT = Duration.ofSeconds(10);

    private final HttpClient httpClient;
    private final String apiKey;

    /**
     * Создаёт сервис с API-ключом из конфигурации приложения.
     */
    public MovieService() {
        this(ConfigUtil.getOmdbApiKey());
    }

    /**
     * Конструктор для тестирования (позволяет подменить ключ).
     *
     * @param apiKey API-ключ OMDb
     */
    public MovieService(String apiKey) {
        this.apiKey = apiKey;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(TIMEOUT)
                .build();
    }

    /**
     * Ищет фильм по названию через OMDb API.
     *
     * @param query название фильма
     * @return найденный фильм с полной информацией
     * @throws MovieNotFoundException если фильм не найден
     * @throws IOException при сетевой ошибке или ошибке ответа API
     * @throws InterruptedException при прерывании HTTP-запроса
     */
    public Movie searchMovie(String query)
            throws IOException, InterruptedException {

        ValidationUtil.validateMovieQuery(query);

        String encoded = URLEncoder.encode(
                query.trim(),
                StandardCharsets.UTF_8
        );

        String url = String.format(OMDB_URL, encoded, apiKey);

        log.debug("Поиск фильма: {}", query);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(TIMEOUT)
                .GET()
                .build();

        HttpResponse<String> response = httpClient.send(
                request,
                HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8)
        );

        if (response.statusCode() != 200) {
            throw new IOException(
                    "OMDb вернул статус " + response.statusCode()
            );
        }

        JsonObject json = JsonParser.parseString(response.body())
                .getAsJsonObject();

        String responseFlag = json.has("Response")
                ? json.get("Response").getAsString()
                : "False";

        if (!"True".equalsIgnoreCase(responseFlag)) {
            throw new MovieNotFoundException(query);
        }

        Movie movie = Movie.builder()
                .title(getString(json, "Title"))
                .year(getString(json, "Year"))
                .genre(getString(json, "Genre"))
                .rating(getString(json, "imdbRating"))
                .poster(getString(json, "Poster"))
                .plot(getString(json, "Plot"))
                .build();

        log.info("Найден фильм: {}", movie.getTitle());

        return movie;
    }

    /**
     * Получает строковое значение поля из JSON-ответа OMDb.
     *
     * Если поле отсутствует или содержит null,
     * возвращает значение по умолчанию.
     *
     * @param json JSON-объект с ответом OMDb
     * @param field название поля
     * @return значение поля или "—", если значение отсутствует
     */
    private String getString(JsonObject json, String field) {
        return json.has(field) && !json.get(field).isJsonNull()
                ? json.get(field).getAsString()
                : "—";
    }
}
