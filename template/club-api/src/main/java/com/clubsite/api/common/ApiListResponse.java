package com.clubsite.api.common;

import java.util.List;

/**
 * Envelope for every list endpoint, so pagination can be added later
 * without changing the response shape clients already parse.
 */
public record ApiListResponse<T>(List<T> data, int page, int size, long totalElements) {

    /** A single page holding the whole (already filtered/limited) list. */
    public static <T> ApiListResponse<T> of(List<T> data) {
        return new ApiListResponse<>(data, 0, data.size(), data.size());
    }
}
