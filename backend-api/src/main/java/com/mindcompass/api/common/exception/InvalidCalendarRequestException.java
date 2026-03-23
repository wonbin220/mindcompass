package com.mindcompass.api.common.exception;

import org.springframework.http.HttpStatus;

// 캘린더 조회 파라미터가 잘못됐을 때 사용하는 예외입니다.
public class InvalidCalendarRequestException extends BusinessException {

    public InvalidCalendarRequestException(String message) {
        super(HttpStatus.BAD_REQUEST, message);
    }
}
