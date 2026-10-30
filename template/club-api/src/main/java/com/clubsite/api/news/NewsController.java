package com.clubsite.api.news;

import com.clubsite.api.common.ApiListResponse;
import com.clubsite.api.news.NewsDtos.NewsArticleRequest;
import com.clubsite.api.news.NewsDtos.NewsArticleResponse;
import jakarta.validation.Valid;
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

/**
 * Public and admin endpoints side by side: the admin ones sit under
 * /api/v1/admin/**, which is what SecurityConfig protects.
 */
@RestController
@RequestMapping("/api/v1")
public class NewsController {

    private final NewsService newsService;

    public NewsController(NewsService newsService) {
        this.newsService = newsService;
    }

    // --- Public ------------------------------------------------------------

    @GetMapping("/news")
    public ApiListResponse<NewsArticleResponse> list(
            @RequestParam(required = false) Integer limit,
            @RequestParam(required = false) String tag) {
        // "published" is not a request parameter, so readers can't ask for drafts.
        return ApiListResponse.of(newsService.list(true, tag, limit).stream().map(NewsArticleResponse::from).toList());
    }

    @GetMapping("/news/{slug}")
    public NewsArticleResponse getBySlug(@PathVariable String slug) {
        return NewsArticleResponse.from(newsService.getPublishedBySlug(slug));
    }

    // --- Admin -------------------------------------------------------------

    @GetMapping("/admin/news")
    public ApiListResponse<NewsArticleResponse> adminList() {
        return ApiListResponse.of(newsService.list(false, null, null).stream().map(NewsArticleResponse::from).toList());
    }

    @PostMapping("/admin/news")
    @ResponseStatus(HttpStatus.CREATED)
    public NewsArticleResponse create(@Valid @RequestBody NewsArticleRequest request) {
        return NewsArticleResponse.from(newsService.create(request));
    }

    @PutMapping("/admin/news/{id}")
    public NewsArticleResponse update(@PathVariable String id, @Valid @RequestBody NewsArticleRequest request) {
        return NewsArticleResponse.from(newsService.update(id, request));
    }

    @DeleteMapping("/admin/news/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable String id) {
        newsService.delete(id);
    }
}
