package mark.warren93.dev.DennyWarriorsAPI.service;

import mark.warren93.dev.DennyWarriorsAPI.exception.ResourceNotFoundException;
import mark.warren93.dev.DennyWarriorsAPI.model.NewsArticle;
import mark.warren93.dev.DennyWarriorsAPI.repository.NewsArticleRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

@Service
public class NewsService {

    private final NewsArticleRepository newsArticleRepository;

    public NewsService(NewsArticleRepository newsArticleRepository) {
        this.newsArticleRepository = newsArticleRepository;
    }

    public List<NewsArticle> getNews(Integer limit, Boolean published, String tag) {
        Stream<NewsArticle> stream = newsArticleRepository.findAll().stream()
                .filter(article -> published == null || article.isPublished() == published)
                .filter(article -> isBlank(tag) || safeTags(article).stream().anyMatch(existingTag -> equalsIgnoreCase(existingTag, tag)))
                .sorted(Comparator.comparing(NewsArticle::getPublishedAt,
                        Comparator.nullsLast(LocalDateTime::compareTo)).reversed());

        if (limit != null && limit > 0) {
            stream = stream.limit(limit);
        }

        return stream.toList();
    }

    public NewsArticle getArticleById(String id) {
        return newsArticleRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Article not found: " + id));
    }

    public NewsArticle getArticleBySlug(String slug) {
        return newsArticleRepository.findBySlug(slug)
                .orElseThrow(() -> new ResourceNotFoundException("Article not found: " + slug));
    }

    public NewsArticle createArticle(NewsArticle article) {
        LocalDateTime now = LocalDateTime.now();
        article.setId(null);
        article.setCreatedAt(now);
        article.setUpdatedAt(now);
        article.setTags(article.getTags() == null ? new ArrayList<>() : article.getTags());
        if (article.isPublished() && article.getPublishedAt() == null) {
            article.setPublishedAt(now);
        }
        return newsArticleRepository.save(article);
    }

    public NewsArticle updateArticle(String id, NewsArticle article) {
        NewsArticle existing = getArticleById(id);
        existing.setTitle(article.getTitle());
        existing.setSlug(article.getSlug());
        existing.setSummary(article.getSummary());
        existing.setContent(article.getContent());
        existing.setImageUrl(article.getImageUrl());
        existing.setAuthor(article.getAuthor());
        existing.setTags(article.getTags() == null ? new ArrayList<>() : article.getTags());
        existing.setPublished(article.isPublished());
        existing.setPublishedAt(article.getPublishedAt());
        if (existing.isPublished() && existing.getPublishedAt() == null) {
            existing.setPublishedAt(LocalDateTime.now());
        }
        existing.setUpdatedAt(LocalDateTime.now());
        return newsArticleRepository.save(existing);
    }

    public void deleteArticle(String id) {
        NewsArticle existing = getArticleById(id);
        newsArticleRepository.delete(existing);
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private boolean equalsIgnoreCase(String left, String right) {
        return Objects.equals(normalise(left), normalise(right));
    }

    private String normalise(String value) {
        return value == null ? null : value.trim().toLowerCase();
    }

    private List<String> safeTags(NewsArticle article) {
        return article.getTags() == null ? List.of() : article.getTags();
    }
}
