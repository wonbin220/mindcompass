package com.mindcompass.api.common.exception;

import org.springframework.http.HttpStatus;

// 이미 가입된 이메일로 회원가입할 때 사용하는 예외입니다.
public class DuplicateEmailException extends BusinessException {

    public DuplicateEmailException(String message) {
        super(HttpStatus.CONFLICT, message);
    }
}
