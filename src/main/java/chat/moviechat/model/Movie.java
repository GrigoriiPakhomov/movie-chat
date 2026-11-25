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
    private String poster;
    private String plot;

    public String toChatFormat() {
        return String.format(
                "🎬 %s (%s). Жанр: %s. Рейтинг IMDb: %s.",
                title != null ? title : "—",
                year != null ? year : "—",
                genre != null ? genre : "—",
                rating != null ? rating : "—"
        );
    }
}
