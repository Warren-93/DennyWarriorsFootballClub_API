package com.clubsite.api.news;

import com.clubsite.api.common.ApiException;
import com.clubsite.api.common.Text;
import com.clubsite.api.news.NewsDtos.NewsArticleRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

@Service
public class NewsService {

    private final NewsArticleRepository repository;
    private final MongoTemplate mongoTemplate;

    public NewsService(NewsArticleRepository repository, MongoTemplate mongoTemplate) {
        this.repository = repository;
        this.mongoTemplate = mongoTemplate;
    }

    /**
     * Filtering, sorting and limiting all happen in MongoDB, not in memory.
     *
     * @param publishedOnly true for the public site, false for the admin list (includes drafts)
     */
    public List<NewsArticle> list(boolean publishedOnly, String tag, Integer limit) {
        Query query = new Query();
        if (publishedOnly) {
            query.addCriteria(Criteria.where("published").is(true));
        }
        if (!Text.isBlank(tag)) {
            // Exact, case-insensitive match against any element of the tags array.
            query.addCriteria(Criteria.where("tags").regex("^" + Pattern.quote(tag.trim()) + "$", "i"));
        }
        query.with(Sort.by(Sort.Order.desc("publishedAt"), Sort.Order.desc("createdAt")));
        if (limit != null && limit > 0) {
            query.limit(limit);
        }
        return mongoTemplate.find(query, NewsArticle.class);
    }

    public NewsArticle getById(String id) {
        return repository.findById(id).orElseThrow(() -> ApiException.notFound("Article", id));
    }

    /** Public lookup: drafts are reported as not found, so their slugs don't leak. */
    public NewsArticle getPublishedBySlug(String slug) {
        return repository.findBySlugAndPublishedTrue(slug).orElseThrow(() -> ApiException.notFound("Article", slug));
    }

    public NewsArticle create(NewsArticleRequest request) {
        NewsArticle article = new NewsArticle();
        article.setCreatedAt(Instant.now());
        return save(article, request);
    }

    public NewsArticle update(String id, NewsArticleRequest request) {
        return save(getById(id), request);
    }

    public void delete(String id) {
        repository.delete(getById(id));
    }

    private NewsArticle save(NewsArticle article, NewsArticleRequest request) {
        String slug = Text.slugify(Text.isBlank(request.slug()) ? request.title() : request.slug());
        if (slug.isEmpty()) {
            throw ApiException.badRequest("slug must contain at least one letter or digit");
        }
        repository.findBySlug(slug)
                .filter(other -> !other.getId().equals(article.getId()))
                .ifPresent(other -> {
                    throw ApiException.conflict("Another article already uses the slug '" + slug + "'");
                });

        article.setTitle(request.title().trim());
        article.setSlug(slug);
        article.setSummary(request.summary());
        article.setContent(request.content());
        article.setImageUrl(request.imageUrl());
        article.setAuthor(request.author());
        article.setTags(request.tags() == null ? new ArrayList<>() : new ArrayList<>(request.tags()));
        article.setPublished(request.published());
        article.setPublishedAt(request.publishedAt() != null ? request.publishedAt() : article.getPublishedAt());
        if (article.isPublished() && article.getPublishedAt() == null) {
            article.setPublishedAt(Instant.now());
        }
        article.setUpdatedAt(Instant.now());
        return repository.save(article);
    }
}
