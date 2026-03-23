package com.mindcompass.api.common.exception;

import org.springframework.http.HttpStatus;

// 서비스 전반에서 공통 HTTP 상태를 담아 던지는 기본 비즈니스 예외입니다.
public class BusinessException extends RuntimeException {

    private final HttpStatus status;

    public BusinessException(HttpStatus status, String message) {
        super(message);
        this.status = status;
    }

    public HttpStatus getStatus() {
        return status;
    }
}
