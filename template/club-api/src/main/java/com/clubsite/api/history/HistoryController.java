package com.clubsite.api.history;

import com.clubsite.api.common.ApiException;
import com.clubsite.api.common.ApiListResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
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

import java.time.Instant;
import java.util.Comparator;

/**
 * The smallest complete feature module: model, repository, request record
 * and controller, with no separate service because there is no logic beyond
 * CRUD. Copy this one when adding a new simple content type (sponsors,
 * coaching staff, documents...).
 */
@RestController
@RequestMapping("/api/v1")
public class HistoryController {

    public record HistoryEntryRequest(
            @NotNull HistoryEntry.Type type,
            Integer year,
            @NotBlank @Size(max = 200) String title,
            @Size(max = 4000) String description,
            String imageUrl,
            int displayOrder) {
    }

    private final HistoryEntryRepository repository;

    public HistoryController(HistoryEntryRepository repository) {
        this.repository = repository;
    }

    @GetMapping({"/history", "/admin/history"})
    public ApiListResponse<HistoryEntry> list(@RequestParam(required = false) HistoryEntry.Type type) {
        return ApiListResponse.of(repository.findAll().stream()
                .filter(entry -> type == null || type == entry.getType())
                .sorted(Comparator.comparingInt(HistoryEntry::getDisplayOrder)
                        .thenComparing(HistoryEntry::getYear, Comparator.nullsLast(Integer::compareTo)))
                .toList());
    }

    @PostMapping("/admin/history")
    @ResponseStatus(HttpStatus.CREATED)
    public HistoryEntry create(@Valid @RequestBody HistoryEntryRequest request) {
        HistoryEntry entry = new HistoryEntry();
        entry.setCreatedAt(Instant.now());
        return save(entry, request);
    }

    @PutMapping("/admin/history/{id}")
    public HistoryEntry update(@PathVariable String id, @Valid @RequestBody HistoryEntryRequest request) {
        return save(get(id), request);
    }

    @DeleteMapping("/admin/history/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable String id) {
        repository.delete(get(id));
    }

    private HistoryEntry get(String id) {
        return repository.findById(id).orElseThrow(() -> ApiException.notFound("History entry", id));
    }

    private HistoryEntry save(HistoryEntry entry, HistoryEntryRequest r) {
        entry.setType(r.type());
        entry.setYear(r.year());
        entry.setTitle(r.title().trim());
        entry.setDescription(r.description());
        entry.setImageUrl(r.imageUrl());
        entry.setDisplayOrder(r.displayOrder());
        entry.setUpdatedAt(Instant.now());
        return repository.save(entry);
    }
}
