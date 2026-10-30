package com.clubsite.api.news;

import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
@Document(collection = "news")
public class NewsArticle {

    @Id
    private String id;
    private String title;
    @Indexed(unique = true)
    private String slug;
    private String summary;
    private String content;
    private String imageUrl;
    private String author;
    /** The first tag doubles as the article's category on the site. */
    private List<String> tags = new ArrayList<>();
    private boolean published;
    private Instant publishedAt;
    private Instant createdAt;
    private Instant updatedAt;
}
