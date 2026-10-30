package mark.warren93.dev.DennyWarriorsAPI.model;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@ToString
@Document(collection = "news")
public class NewsArticle {

    @Id
    private String id;
    private String title;
    private String slug;
    private String summary;
    private String content;
    private String imageUrl;
    private String author;
    private List<String> tags = new ArrayList<>();
    private boolean published;
    private LocalDateTime publishedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
