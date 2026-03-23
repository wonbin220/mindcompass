package com.mindcompass.api.common.logging;

// 모든 요청에 requestId를 부여하고 시작/종료 로그를 남기는 추적 필터다.

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class RequestTracingFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(RequestTracingFilter.class);

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        String requestId = resolveRequestId(request);
        long startedAt = System.currentTimeMillis();

        MDC.put(RequestTraceContext.REQUEST_ID_KEY, requestId);
        response.setHeader(RequestTraceContext.REQUEST_ID_HEADER, requestId);

        log.info(
                "Request started. requestId={}, method={}, path={}",
                requestId,
                request.getMethod(),
                request.getRequestURI()
        );

        try {
            filterChain.doFilter(request, response);
        } finally {
            long elapsedMs = System.currentTimeMillis() - startedAt;
            log.info(
                    "Request completed. requestId={}, method={}, path={}, status={}, elapsedMs={}",
                    requestId,
                    request.getMethod(),
                    request.getRequestURI(),
                    response.getStatus(),
                    elapsedMs
            );
            MDC.remove(RequestTraceContext.REQUEST_ID_KEY);
        }
    }

    private String resolveRequestId(HttpServletRequest request) {
        String requestIdHeader = request.getHeader(RequestTraceContext.REQUEST_ID_HEADER);
        if (StringUtils.hasText(requestIdHeader)) {
            return requestIdHeader.trim();
        }
        return UUID.randomUUID().toString();
    }
}
