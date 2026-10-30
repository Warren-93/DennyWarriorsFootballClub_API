package com.clubsite.api.news;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.util.List;

public final class NewsDtos {

    private NewsDtos() {
    }

    /**
     * What an editor may set. Server-managed fields (id, createdAt,
     * updatedAt) are deliberately absent, so a request can't overwrite them.
     */
    public record NewsArticleRequest(
            @NotBlank @Size(max = 200) String title,
            /* Optional: generated from the title when blank. */
            @Size(max = 200) String slug,
            @Size(max = 500) String summary,
            @NotBlank String content,
            String imageUrl,
            @Size(max = 100) String author,
            List<@NotBlank String> tags,
            boolean published,
            /* Optional: defaults to "now" the first time the article is published. */
            Instant publishedAt) {
    }

    public record NewsArticleResponse(
            String id,
            String title,
            String slug,
            String summary,
            String content,
            String imageUrl,
            String author,
            List<String> tags,
            boolean published,
            Instant publishedAt,
            Instant updatedAt) {

        public static NewsArticleResponse from(NewsArticle article) {
            return new NewsArticleResponse(
                    article.getId(),
                    article.getTitle(),
                    article.getSlug(),
                    article.getSummary(),
                    article.getContent(),
                    article.getImageUrl(),
                    article.getAuthor(),
                    article.getTags() == null ? List.of() : article.getTags(),
                    article.isPublished(),
                    article.getPublishedAt(),
                    article.getUpdatedAt());
        }
    }
}
