package com.mindcompass.api.common.exception;

// 전역 예외를 공통 에러 응답과 requestId 포함 운영 로그로 정리하는 핸들러다.

import com.mindcompass.api.common.logging.RequestTraceContext;
import jakarta.servlet.http.HttpServletRequest;
import java.time.LocalDateTime;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ErrorResponse> handleBusinessException(
            BusinessException exception,
            HttpServletRequest request
    ) {
        log.warn(
                "Business exception. requestId={}, path={}, status={}, message={}",
                RequestTraceContext.currentRequestId(),
                request.getRequestURI(),
                exception.getStatus().value(),
                exception.getMessage()
        );
        return buildResponse(exception.getStatus(), exception.getMessage(), request.getRequestURI());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationException(
            MethodArgumentNotValidException exception,
            HttpServletRequest request
    ) {
        String message = exception.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.joining(", "));

        log.warn(
                "Validation exception. requestId={}, path={}, message={}",
                RequestTraceContext.currentRequestId(),
                request.getRequestURI(),
                message
        );
        return buildResponse(HttpStatus.BAD_REQUEST, message, request.getRequestURI());
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleHttpMessageNotReadableException(
            HttpMessageNotReadableException exception,
            HttpServletRequest request
    ) {
        String message = exception.getMostSpecificCause() == null
                ? "요청 본문 형식이 올바르지 않습니다."
                : exception.getMostSpecificCause().getMessage();

        log.warn(
                "Request body parse exception. requestId={}, path={}, message={}",
                RequestTraceContext.currentRequestId(),
                request.getRequestURI(),
                message
        );
        return buildResponse(HttpStatus.BAD_REQUEST, message, request.getRequestURI());
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorResponse> handleMethodArgumentTypeMismatchException(
            MethodArgumentTypeMismatchException exception,
            HttpServletRequest request
    ) {
        String message = exception.getName() + " 값 형식이 올바르지 않습니다.";
        log.warn(
                "Method argument type mismatch. requestId={}, path={}, message={}",
                RequestTraceContext.currentRequestId(),
                request.getRequestURI(),
                message
        );
        return buildResponse(HttpStatus.BAD_REQUEST, message, request.getRequestURI());
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ErrorResponse> handleMissingServletRequestParameterException(
            MissingServletRequestParameterException exception,
            HttpServletRequest request
    ) {
        String message = exception.getParameterName() + " 파라미터가 필요합니다.";
        log.warn(
                "Missing request parameter. requestId={}, path={}, message={}",
                RequestTraceContext.currentRequestId(),
                request.getRequestURI(),
                message
        );
        return buildResponse(HttpStatus.BAD_REQUEST, message, request.getRequestURI());
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ErrorResponse> handleNoResourceFoundException(
            NoResourceFoundException exception,
            HttpServletRequest request
    ) {
        log.warn(
                "API path not found. requestId={}, path={}",
                RequestTraceContext.currentRequestId(),
                request.getRequestURI()
        );
        return buildResponse(HttpStatus.NOT_FOUND, "요청한 API 경로를 찾을 수 없습니다.", request.getRequestURI());
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleUnexpectedException(
            Exception exception,
            HttpServletRequest request
    ) {
        log.error(
                "Unexpected exception. requestId={}, path={}",
                RequestTraceContext.currentRequestId(),
                request.getRequestURI(),
                exception
        );
        return buildResponse(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "예상하지 못한 오류가 발생했습니다.",
                request.getRequestURI()
        );
    }

    private ResponseEntity<ErrorResponse> buildResponse(HttpStatus status, String message, String path) {
        ErrorResponse errorResponse = new ErrorResponse(
                LocalDateTime.now(),
                status.value(),
                status.getReasonPhrase(),
                message,
                path
        );
        return ResponseEntity.status(status).body(errorResponse);
    }
}
