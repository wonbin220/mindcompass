package com.mindcompass.api.common.exception;

import org.springframework.http.HttpStatus;

// refresh token 검증에 실패했을 때 사용하는 인증 예외입니다.
public class InvalidRefreshTokenException extends BusinessException {

    public InvalidRefreshTokenException(String message) {
        super(HttpStatus.UNAUTHORIZED, message);
    }
}
