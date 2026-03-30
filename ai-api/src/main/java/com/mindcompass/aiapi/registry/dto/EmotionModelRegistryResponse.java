// 감정분류 모델 registry 응답 DTO이다.
package com.mindcompass.aiapi.registry.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Schema(description = "감정 모델 registry 응답 DTO")
public record EmotionModelRegistryResponse(
        @Schema(description = "registry id", example = "1")
        Long id,
        @Schema(description = "experiment 이름", example = "cpu_compare_medium_relabel_weighted")
        String experimentName,
        @Schema(description = "model 이름", example = "cpu_compare_medium_relabel_weighted_active5")
        String modelName,
        @Schema(description = "base model 이름", example = "beomi/KcELECTRA-base")
        String baseModelName,
        @Schema(description = "artifact 디렉터리 경로", example = "C:/programing/mindcompass/ai-api-fastapi/training/emotion_classifier/artifacts/cpu_compare_medium_relabel_weighted_active5")
        String artifactDir,
        @Schema(description = "metric JSON 경로", example = "C:/programing/mindcompass/ai-api-fastapi/training/emotion_classifier/artifacts/evaluation/valid_metrics_cpu_compare_medium_relabel_weighted.json")
        String metricsJsonPath,
        @Schema(description = "label metadata JSON 경로", example = "C:/programing/mindcompass/ai-api-fastapi/training/emotion_classifier/artifacts/cpu_compare_medium_relabel_weighted_active5/best/label_metadata.json")
        String labelMetadataPath,
        @Schema(description = "training config JSON 경로", example = "C:/programing/mindcompass/ai-api-fastapi/training/emotion_classifier/configs/training_config_cpu.json")
        String trainingConfigPath,
        @Schema(description = "label map JSON 경로", example = "C:/programing/mindcompass/ai-api-fastapi/training/emotion_classifier/configs/label_map.json")
        String labelMapPath,
        @Schema(description = "훈련 데이터셋 태그", example = "emotion_mvp_relabel_weighted")
        String trainingDatasetTag,
        @Schema(description = "검증 데이터셋 태그", example = "emotion_mvp_cpu_compare_medium")
        String validationDatasetTag,
        @Schema(description = "fallback 정책", example = "TIRED_FALLBACK_ONLY")
        String fallbackPolicy,
        @Schema(description = "현재 registry status", example = "ACTIVE")
        String status,
        @Schema(description = "현재 active serving row 여부", example = "true")
        boolean isActive,
        @Schema(description = "shadow candidate lineage/source 여부", example = "false")
        boolean isShadow,
        @Schema(description = "accuracy", example = "0.4267")
        BigDecimal accuracy,
        @Schema(description = "macro F1", example = "0.3645")
        BigDecimal macroF1,
        @Schema(description = "HAPPY F1", example = "0.6146")
        BigDecimal happyF1,
        @Schema(description = "CALM F1", example = "0.2941")
        BigDecimal calmF1,
        @Schema(description = "ANXIOUS F1", example = "0.4021")
        BigDecimal anxiousF1,
        @Schema(description = "SAD F1", example = "0.3564")
        BigDecimal sadF1,
        @Schema(description = "ANGRY F1", example = "0.4682")
        BigDecimal angryF1,
        @Schema(description = "serving 운영 메모", example = "current baseline serving model")
        String servingNotes,
        @Schema(description = "승인 메모", example = "approved for serving")
        String approvalNote,
        @Schema(description = "반려 사유", example = "fixed compare gate regression")
        String rejectionReason,
        @Schema(description = "APPROVED 시각", example = "2026-03-29T10:15:30")
        LocalDateTime approvedAt,
        @Schema(description = "REJECTED 시각", example = "2026-03-29T11:30:00")
        LocalDateTime rejectedAt,
        @Schema(description = "ACTIVE 전환 시각", example = "2026-03-29T12:00:00")
        LocalDateTime activatedAt,
        @Schema(description = "생성 시각", example = "2026-03-29T09:50:00")
        LocalDateTime createdAt,
        @Schema(description = "최종 수정 시각", example = "2026-03-29T12:00:00")
        LocalDateTime updatedAt
) {
}
