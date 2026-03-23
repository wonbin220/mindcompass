package com.mindcompass.api.common.exception;

import org.springframework.http.HttpStatus;

// 로그인 정보가 올바르지 않을 때 사용하는 인증 예외입니다.
public class InvalidCredentialsException extends BusinessException {

    public InvalidCredentialsException(String message) {
        super(HttpStatus.UNAUTHORIZED, message);
    }
}
