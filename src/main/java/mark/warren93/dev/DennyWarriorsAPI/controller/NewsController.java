package mark.warren93.dev.DennyWarriorsAPI.controller;

import mark.warren93.dev.DennyWarriorsAPI.dto.ApiListResponse;
import mark.warren93.dev.DennyWarriorsAPI.dto.NewsArticleResponse;
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
    public ApiListResponse<NewsArticleResponse> getNews(
            @RequestParam(required = false) Integer limit,
            @RequestParam(required = false) String tag) {
        // Public listing only ever shows published articles — the "published"
        // filter itself isn't a public-controllable query param, so a reader
        // can't request drafts by passing published=false.
        var articles = newsService.getNews(limit, true, tag).stream()
                .map(NewsArticleResponse::from)
                .toList();
        return ApiListResponse.of(articles);
    }

    @GetMapping("/{slug}")
    public NewsArticleResponse getArticleBySlug(@PathVariable String slug) {
        return NewsArticleResponse.from(newsService.getArticleBySlug(slug));
    }
}
