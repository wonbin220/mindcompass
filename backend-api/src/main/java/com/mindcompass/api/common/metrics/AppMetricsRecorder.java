package com.mindcompass.api.common.metrics;

// 핵심 사용자 흐름의 성공/실패 카운터를 Micrometer에 기록하는 메트릭 기록기다.

import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Component;

@Component
public class AppMetricsRecorder {

    private final MeterRegistry meterRegistry;

    public AppMetricsRecorder(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
    }

    public void incrementChatResponse(String responseType) {
        meterRegistry.counter("mindcompass.chat.responses", "type", normalize(responseType)).increment();
    }

    public void incrementChatAiFailure() {
        meterRegistry.counter("mindcompass.chat.ai.failures").increment();
    }

    public void incrementDiaryCreated() {
        meterRegistry.counter("mindcompass.diary.created").increment();
    }

    public void incrementDiaryUpdated() {
        meterRegistry.counter("mindcompass.diary.updated").increment();
    }

    public void incrementDiaryDeleted() {
        meterRegistry.counter("mindcompass.diary.deleted").increment();
    }

    public void incrementDiaryAiAnalysisFailure() {
        meterRegistry.counter("mindcompass.diary.ai.failures", "type", "analysis").increment();
    }

    public void incrementDiaryRiskFailure() {
        meterRegistry.counter("mindcompass.diary.ai.failures", "type", "risk").increment();
    }

    public void incrementReportQuery(String reportType) {
        meterRegistry.counter("mindcompass.report.queries", "type", normalize(reportType)).increment();
    }

    private String normalize(String value) {
        return value == null || value.isBlank() ? "unknown" : value.toLowerCase();
    }
}
