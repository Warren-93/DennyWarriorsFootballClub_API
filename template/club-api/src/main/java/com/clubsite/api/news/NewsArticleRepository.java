package com.clubsite.api.news;

import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Optional;

public interface NewsArticleRepository extends MongoRepository<NewsArticle, String> {

    Optional<NewsArticle> findBySlug(String slug);

    Optional<NewsArticle> findBySlugAndPublishedTrue(String slug);
}
