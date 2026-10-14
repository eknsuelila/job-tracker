package com.portfolio.jobtracker.common;

import org.springframework.data.domain.Page;

import java.util.List;

/** Stable, frontend-friendly pagination wrapper (instead of serializing Spring's Page directly). */
public record PageResponse<T>(List<T> content, int page, int size, long totalElements, int totalPages) {

    public static <T> PageResponse<T> from(Page<T> page) {
        return new PageResponse<>(page.getContent(), page.getNumber(), page.getSize(),
                page.getTotalElements(), page.getTotalPages());
    }
}
