package com.whatsapp.common.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Page Response DTO
 *
 * Standard pagination response for list queries.
 *
 * Structure:
 * - content: List of items
 * - page: Current page number (0-based)
 * - size: Items per page
 * - totalElements: Total number of items
 * - totalPages: Total number of pages
 * - first: Is this the first page
 * - last: Is this the last page
 *
 * Usage:
 * GET /api/users?page=0&size=10
 *
 * Response:
 * {
 *   "content": [...],
 *   "page": 0,
 *   "size": 10,
 *   "totalElements": 100,
 *   "totalPages": 10,
 *   "first": true,
 *   "last": false
 * }
 *
 * @author WhatsApp Clone Team
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class PageResponse<T> {

    /**
     * List of items in current page
     */
    private List<T> content;

    /**
     * Current page number (0-based)
     */
    private Integer page;

    /**
     * Number of items per page
     */
    private Integer size;

    /**
     * Total number of items across all pages
     */
    private Long totalElements;

    /**
     * Total number of pages
     */
    private Integer totalPages;

    /**
     * Is this the first page
     */
    private Boolean first;

    /**
     * Is this the last page
     */
    private Boolean last;

    /**
     * Create page response
     */
    public static <T> PageResponse<T> of(List<T> content, int page, int size, long totalElements) {
        int totalPages = (int) Math.ceil((double) totalElements / size);

        return PageResponse.<T>builder()
                .content(content)
                .page(page)
                .size(size)
                .totalElements(totalElements)
                .totalPages(totalPages)
                .first(page == 0)
                .last(page >= totalPages - 1)
                .build();
    }
}