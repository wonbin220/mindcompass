// 감정분류 모델 registry 등록 요청 DTO이다.
package com.mindcompass.aiapi.registry.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import java.math.BigDecimal;

@Schema(description = "감정 모델 registry 등록 요청 DTO")
public record CreateEmotionModelRegistryRequest(
        @Schema(description = "registry에서 유니크하게 관리하는 experiment 이름", example = "cpu_compare_medium_manual_seed_v2")
        @NotBlank String experimentName,
        @Schema(description = "serving 또는 비교 대상 model 이름", example = "cpu_compare_medium_manual_seed_v2_active5")
        @NotBlank String modelName,
        @Schema(description = "fine-tuning에 사용한 base model 이름", example = "beomi/KcELECTRA-base")
        @NotBlank String baseModelName,
        @Schema(description = "artifact 디렉터리 절대 경로", example = "C:/programing/mindcompass/ai-api-fastapi/training/emotion_classifier/artifacts/cpu_compare_medium_manual_seed_v2")
        @NotBlank String artifactDir,
        @Schema(description = "평가 metric JSON 절대 경로", example = "C:/programing/mindcompass/ai-api-fastapi/training/emotion_classifier/artifacts/evaluation/valid_metrics_cpu_compare_medium_manual_seed_v2.json")
        @NotBlank String metricsJsonPath,
        @Schema(description = "label metadata JSON 절대 경로", example = "C:/programing/mindcompass/ai-api-fastapi/training/emotion_classifier/artifacts/cpu_compare_medium_manual_seed_v2/best/label_metadata.json")
        String labelMetadataPath,
        @Schema(description = "훈련 config JSON 절대 경로", example = "C:/programing/mindcompass/ai-api-fastapi/training/emotion_classifier/configs/training_config_cpu.json")
        String trainingConfigPath,
        @Schema(description = "label map JSON 절대 경로", example = "C:/programing/mindcompass/ai-api-fastapi/training/emotion_classifier/configs/label_map.json")
        String labelMapPath,
        @Schema(description = "훈련 데이터셋 태그", example = "emotion_mvp_manual_seed_v2")
        @NotBlank String trainingDatasetTag,
        @Schema(description = "검증 데이터셋 태그", example = "emotion_mvp_cpu_compare_medium")
        @NotBlank String validationDatasetTag,
        @Schema(description = "현재 serving fallback 정책", example = "TIRED_FALLBACK_ONLY")
        @NotBlank String fallbackPolicy,
        @Schema(description = "등록 시 초기 status, 비우면 TRAINED", example = "TRAINED", allowableValues = {"TRAINED", "APPROVED", "REJECTED", "SHADOW", "ARCHIVED"})
        String status,
        @Schema(description = "shadow candidate lineage/source 여부", example = "true")
        Boolean isShadow,
        @Schema(description = "전체 accuracy", example = "0.4427")
        BigDecimal accuracy,
        @Schema(description = "전체 macro F1", example = "0.3888")
        BigDecimal macroF1,
        @Schema(description = "HAPPY F1", example = "0.6250")
        BigDecimal happyF1,
        @Schema(description = "CALM F1", example = "0.3182")
        BigDecimal calmF1,
        @Schema(description = "ANXIOUS F1", example = "0.4021")
        BigDecimal anxiousF1,
        @Schema(description = "SAD F1", example = "0.3564")
        BigDecimal sadF1,
        @Schema(description = "ANGRY F1", example = "0.2423")
        BigDecimal angryF1,
        @Schema(description = "serving 판단 메모", example = "best macro F1 but watch HAPPY->CALM collapse")
        String servingNotes,
        @Schema(description = "승인 판단 메모", example = "approved as shadow candidate only")
        String approvalNote,
        @Schema(description = "반려 사유", example = "fixed compare gate did not beat current baseline")
        String rejectionReason
) {
}
