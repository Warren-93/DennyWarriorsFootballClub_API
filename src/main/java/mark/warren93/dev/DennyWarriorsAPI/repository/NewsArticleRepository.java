package mark.warren93.dev.DennyWarriorsAPI.repository;

import mark.warren93.dev.DennyWarriorsAPI.model.NewsArticle;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Optional;

public interface NewsArticleRepository extends MongoRepository<NewsArticle, String> {

    Optional<NewsArticle> findBySlug(String slug);
}
