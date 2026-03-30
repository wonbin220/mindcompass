// active registry와 FastAPI serving runtime 경로 정합성 점검 결과를 담는 DTO입니다.
package com.mindcompass.aiapi.registry.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "active registry와 FastAPI serving runtime 경로 정합성 점검 응답 DTO")
public record EmotionModelRegistryRuntimeAlignmentResponse(
        @Schema(description = "active registry id", example = "1")
        Long activeRegistryId,
        @Schema(description = "active experiment 이름", example = "cpu_compare_medium_relabel_weighted")
        String activeExperimentName,
        @Schema(description = "registry에 저장된 active artifact 경로", example = "C:/programing/mindcompass/ai-api-fastapi/training/emotion_classifier/artifacts/cpu_compare_medium_relabel_weighted_active5")
        String registryArtifactDir,
        @Schema(description = "FastAPI base URL", example = "http://localhost:8002")
        String fastApiBaseUrl,
        @Schema(description = "FastAPI runtime의 model dir 원본 설정값", example = "ai-api-fastapi/training/emotion_classifier/artifacts/best")
        String runtimeModelDirConfigured,
        @Schema(description = "FastAPI runtime의 model dir 절대 경로", example = "C:/programing/mindcompass/ai-api-fastapi/training/emotion_classifier/artifacts/cpu_compare_medium_relabel_weighted_active5")
        String runtimeModelDirResolved,
        @Schema(description = "FastAPI runtime model dir 존재 여부", example = "true")
        boolean runtimeModelDirExists,
        @Schema(description = "FastAPI runtime이 실제 로드에 사용할 source", example = "C:/programing/mindcompass/ai-api-fastapi/training/emotion_classifier/artifacts/cpu_compare_medium_relabel_weighted_active5")
        String runtimeModelLoadSource,
        @Schema(description = "FastAPI runtime label map 원본 설정값", example = "ai-api-fastapi/training/emotion_classifier/configs/label_map.json")
        String runtimeLabelMapPathConfigured,
        @Schema(description = "FastAPI runtime label map 절대 경로", example = "C:/programing/mindcompass/ai-api-fastapi/training/emotion_classifier/configs/label_map.json")
        String runtimeLabelMapPathResolved,
        @Schema(description = "FastAPI runtime label map 존재 여부", example = "true")
        boolean runtimeLabelMapPathExists,
        @Schema(description = "FastAPI runtime model name", example = "beomi/KcELECTRA-base")
        String runtimeModelName,
        @Schema(description = "FastAPI runtime max length", example = "128")
        int runtimeMaxLength,
        @Schema(description = "registry artifact path와 runtime model dir 절대 경로 일치 여부", example = "true")
        boolean artifactDirAligned,
        @Schema(description = "전체 정합성 통과 여부. 현재는 artifact dir 일치와 runtime model dir 존재를 함께 본다.", example = "true")
        boolean overallAligned,
        @Schema(description = "정합성 요약 메시지", example = "Active registry artifact dir matches FastAPI runtime model dir")
        String detail
) {
}
