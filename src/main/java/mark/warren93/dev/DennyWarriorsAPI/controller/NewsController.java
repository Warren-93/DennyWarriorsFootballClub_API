package mark.warren93.dev.DennyWarriorsAPI.controller;

import mark.warren93.dev.DennyWarriorsAPI.model.NewsArticle;
import mark.warren93.dev.DennyWarriorsAPI.service.NewsService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/news")
public class NewsController {

    private final NewsService newsService;

    public NewsController(NewsService newsService) {
        this.newsService = newsService;
    }

    @GetMapping
    public List<NewsArticle> getNews(
            @RequestParam(required = false) Integer limit,
            @RequestParam(required = false) Boolean published,
            @RequestParam(required = false) String tag) {
        return newsService.getNews(limit, published, tag);
    }

    @GetMapping("/{id}")
    public NewsArticle getArticleById(@PathVariable String id) {
        return newsService.getArticleById(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public NewsArticle createArticle(@RequestBody NewsArticle article) {
        return newsService.createArticle(article);
    }

    @PutMapping("/{id}")
    public NewsArticle updateArticle(@PathVariable String id, @RequestBody NewsArticle article) {
        return newsService.updateArticle(id, article);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteArticle(@PathVariable String id) {
        newsService.deleteArticle(id);
    }
}
