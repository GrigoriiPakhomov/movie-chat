package chat.moviechat.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Модель фильма, полученного из OMDb API.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Movie {

    private String title;
    private String year;
    private String genre;
    private String rating;

    public String toChatFormat() {
        return String.format(" Найден фильм: \"%s\" (%s). Жанр: %s. Рейтинг: %s.",
                title, year, genre, rating);
    }
}