package com.mindcompass.api.common.exception;

import org.springframework.http.HttpStatus;

// 조회 대상 리소스가 없을 때 사용하는 공통 예외입니다.
public class ResourceNotFoundException extends BusinessException {

    public ResourceNotFoundException(String message) {
        super(HttpStatus.NOT_FOUND, message);
    }
}
