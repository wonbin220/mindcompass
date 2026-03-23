package com.mindcompass.api.common.logging;

// 요청 추적용 requestId 키와 조회 헬퍼를 모아두는 유틸리티다.

import org.slf4j.MDC;

public final class RequestTraceContext {

    public static final String REQUEST_ID_KEY = "requestId";
    public static final String REQUEST_ID_HEADER = "X-Request-Id";

    private RequestTraceContext() {
    }

    public static String currentRequestId() {
        String requestId = MDC.get(REQUEST_ID_KEY);
        return requestId == null ? "N/A" : requestId;
    }
}
