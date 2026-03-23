package com.mindcompass.api.common.exception;

import org.springframework.http.HttpStatus;

// 일기 입력값이나 저장 정책이 맞지 않을 때 사용하는 예외입니다.
public class InvalidDiaryRequestException extends BusinessException {

    public InvalidDiaryRequestException(String message) {
        super(HttpStatus.BAD_REQUEST, message);
    }
}
