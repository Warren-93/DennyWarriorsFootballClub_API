package mark.warren93.dev.DennyWarriorsAPI.dto;

import mark.warren93.dev.DennyWarriorsAPI.model.NewsArticle;

import java.time.LocalDateTime;
import java.util.List;

public record NewsArticleResponse(
        String id,
        String title,
        String slug,
        String excerpt,
        String content,
        String imageUrl,
        String author,
        String category,
        List<String> tags,
        boolean published,
        LocalDateTime date) {

    public static NewsArticleResponse from(NewsArticle article) {
        List<String> tags = article.getTags() == null ? List.of() : article.getTags();
        String category = tags.isEmpty() ? "Club News" : tags.get(0);
        LocalDateTime date = article.getPublishedAt() != null ? article.getPublishedAt() : article.getCreatedAt();

        return new NewsArticleResponse(
                article.getId(),
                article.getTitle(),
                article.getSlug(),
                article.getSummary(),
                article.getContent(),
                article.getImageUrl(),
                article.getAuthor(),
                category,
                tags,
                article.isPublished(),
                date);
    }
}
