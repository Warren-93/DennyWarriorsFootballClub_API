package mark.warren93.dev.DennyWarriorsAPI.controller;

import mark.warren93.dev.DennyWarriorsAPI.dto.ApiListResponse;
import mark.warren93.dev.DennyWarriorsAPI.model.NewsArticle;
import mark.warren93.dev.DennyWarriorsAPI.service.NewsService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/news")
public class NewsController {

    private final NewsService newsService;

    public NewsController(NewsService newsService) {
        this.newsService = newsService;
    }

    @GetMapping
    public ApiListResponse<NewsArticle> getNews(
            @RequestParam(required = false) Integer limit,
            @RequestParam(required = false) Boolean published,
            @RequestParam(required = false) String tag) {
        return ApiListResponse.of(newsService.getNews(limit, published, tag));
    }

    @GetMapping("/{slug}")
    public NewsArticle getArticleBySlug(@PathVariable String slug) {
        return newsService.getArticleBySlug(slug);
    }
}
