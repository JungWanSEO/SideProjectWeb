package com.commerce.api.global.common;

import lombok.Getter;

/**
 * 모든 API의 공통 응답 포맷.
 * { "success": true/false, "message": "...", "data": {...} }
 *
 * 정적 팩토리 메서드(success/error)로 생성한다.
 *
 * @param <T> 응답 데이터 타입
 */
@Getter
public class ApiResponse<T> {

    private final boolean success;
    private final String message;
    private final T data;

    private ApiResponse(boolean success, String message, T data) {
        this.success = success;
        this.message = message;
        this.data = data;
    }

    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>(true, null, data);
    }

    public static <T> ApiResponse<T> success(String message, T data) {
        return new ApiResponse<>(true, message, data);
    }

    public static <T> ApiResponse<T> error(String message) {
        return new ApiResponse<>(false, message, null);
    }
}
