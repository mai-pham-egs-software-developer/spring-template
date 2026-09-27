package com.my.craft.opensearch.models;

import java.util.List;

public record PageResponse<T>(List<T> data, int page, int size, long total, boolean hasNext) {

    public static <T> PageResponse<T> of(PageRequest request, long total, List<T> data) {
        boolean hasNext = (long) (request.page() + 1) * request.size() < total;
        return new PageResponse<>(data, request.page(), request.size(), total, hasNext);
    }
}
