package mark.warren93.dev.DennyWarriorsAPI.dto;

import java.util.List;

public record ApiListResponse<T>(List<T> data, int page, int size, long totalElements) {

    /**
     * Wraps an already-limited/filtered in-memory list as a single page
     * (page=0, size=data.size()). Fits this app's small per-collection
     * datasets (single club) — no DB-level pagination needed yet.
     */
    public static <T> ApiListResponse<T> of(List<T> data) {
        return new ApiListResponse<>(data, 0, data.size(), data.size());
    }
}
