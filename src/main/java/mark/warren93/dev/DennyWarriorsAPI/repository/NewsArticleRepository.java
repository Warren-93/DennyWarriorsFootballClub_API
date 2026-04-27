package mark.warren93.dev.DennyWarriorsAPI.repository;

import mark.warren93.dev.DennyWarriorsAPI.model.NewsArticle;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface NewsArticleRepository extends MongoRepository<NewsArticle, String> {
}
