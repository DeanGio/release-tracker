package com.deangio.releasetracker.api;

import java.util.List;

public record PageResponse<T>(
        List<T> items,
        int limit,
        int offset,
        long total,
        Integer nextOffset) {

    public static <T> PageResponse<T> of(List<T> items, int limit, int offset, long total) {
        int next = offset + items.size();
        return new PageResponse<>(
                List.copyOf(items),
                limit,
                offset,
                total,
                next < total ? next : null);
    }
}
