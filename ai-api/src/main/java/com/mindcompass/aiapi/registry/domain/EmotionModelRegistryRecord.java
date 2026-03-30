// 감정분류 모델 registry 한 행을 표현하는 내부 도메인 객체이다.
package com.mindcompass.aiapi.registry.domain;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record EmotionModelRegistryRecord(
        Long id,
        String experimentName,
        String modelName,
        String baseModelName,
        String artifactDir,
        String metricsJsonPath,
        String labelMetadataPath,
        String trainingConfigPath,
        String labelMapPath,
        String trainingDatasetTag,
        String validationDatasetTag,
        String fallbackPolicy,
        EmotionModelRegistryStatus status,
        boolean isActive,
        boolean isShadow,
        BigDecimal accuracy,
        BigDecimal macroF1,
        BigDecimal happyF1,
        BigDecimal calmF1,
        BigDecimal anxiousF1,
        BigDecimal sadF1,
        BigDecimal angryF1,
        String servingNotes,
        String approvalNote,
        String rejectionReason,
        LocalDateTime approvedAt,
        LocalDateTime rejectedAt,
        LocalDateTime activatedAt,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
