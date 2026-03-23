package com.mindcompass.api.common.exception;

// 리포트 조회 파라미터가 잘못됐을 때 사용하는 예외다.

import org.springframework.http.HttpStatus;

public class InvalidReportRequestException extends BusinessException {

    public InvalidReportRequestException(String message) {
        super(HttpStatus.BAD_REQUEST, message);
    }
}
