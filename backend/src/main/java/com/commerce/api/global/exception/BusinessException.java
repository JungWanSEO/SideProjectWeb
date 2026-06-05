package com.commerce.api.global.exception;

import org.springframework.http.HttpStatus;
import lombok.Getter;

/**
 * 비즈니스 규칙 위반 시 던지는 예외.
 * 예: 이미 가입된 이메일, 존재하지 않는 회원 조회 등.
 *
 * HttpStatus를 함께 들고 있어서, 전역 핸들러가 적절한 상태코드로 응답할 수 있다.
 */
@Getter
public class BusinessException extends RuntimeException {

    private final HttpStatus status;

    public BusinessException(HttpStatus status, String message) {
        super(message);
        this.status = status;
    }
}
