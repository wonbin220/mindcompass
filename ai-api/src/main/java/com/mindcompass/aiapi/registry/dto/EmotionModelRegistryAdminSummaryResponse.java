// 감정 분류 모델 registry 운영 요약 정보를 담는 응답 DTO다.
package com.mindcompass.aiapi.registry.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "감정 모델 registry 운영 요약 응답 DTO")
public record EmotionModelRegistryAdminSummaryResponse(
        @Schema(description = "전체 registry row 수", example = "4")
        long totalCount,
        @Schema(description = "TRAINED 상태 row 수", example = "1")
        long trainedCount,
        @Schema(description = "APPROVED 상태 row 수", example = "1")
        long approvedCount,
        @Schema(description = "REJECTED 상태 row 수", example = "1")
        long rejectedCount,
        @Schema(description = "SHADOW 상태 row 수", example = "1")
        long shadowCount,
        @Schema(description = "ACTIVE 상태 row 수", example = "1")
        long activeCount,
        @Schema(description = "ARCHIVED 상태 row 수", example = "0")
        long archivedCount,
        @Schema(description = "shadow lineage/source 플래그가 true 인 row 수", example = "3")
        long shadowLineageCount,
        @Schema(description = "현재 active row id, 없으면 null", example = "1", nullable = true)
        Long activeRegistryId,
        @Schema(description = "현재 active experiment 이름, 없으면 null", example = "cpu_compare_medium_relabel_weighted", nullable = true)
        String activeExperimentName
) {
}
