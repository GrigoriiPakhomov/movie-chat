
package chat.moviechat.dto;

import chat.moviechat.model.Movie;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.LocalDateTime;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ChatMessage(
        String type,
        String username,
        String content,
        LocalDateTime timestamp,
        List<String> users,
        Movie movie
) {}
