package com.commerce.api.global.common;

import java.util.List;
import org.springframework.data.domain.Page;

/**
 * 페이지네이션 공통 응답 포맷.
 *
 * <p>Spring Data의 {@link Page}를 그대로 직렬화하면 내부 구조(pageable, sort 등)가 노출되고,
 * Spring Boot 3.x는 {@code PageImpl} 직접 직렬화에 대해 "향후 구조가 바뀔 수 있다"는 경고를 낸다.
 * 그래서 목록(content) + 필요한 페이지 메타만 추려서 안정적인 API 계약을 제공한다.
 *
 * @param <T> 항목 타입
 */
public record PageResponse<T>(
        List<T> content,
        int page,            // 현재 페이지 번호 (0-based)
        int size,            // 페이지 크기
        long totalElements,  // 전체 항목 수
        int totalPages,      // 전체 페이지 수
        boolean hasNext      // 다음 페이지 존재 여부
) {
    /** 이미 DTO로 매핑된 {@code Page<T>}를 공통 응답으로 변환한다. */
    public static <T> PageResponse<T> from(Page<T> page) {
        return new PageResponse<>(
                page.getContent(),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages(),
                page.hasNext()
        );
    }
}
