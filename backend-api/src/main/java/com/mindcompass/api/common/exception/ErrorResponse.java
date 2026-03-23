package com.mindcompass.api.common.exception;

import java.time.LocalDateTime;

// 예외 발생 시 공통 형식으로 내려주는 에러 응답 DTO입니다.
public record ErrorResponse(
        LocalDateTime timestamp,
        int status,
        String error,
        String message,
        String path
) {
}
