// 감정 분류 모델 promotion checklist 검증 요약을 담는 응답 DTO다.
package com.mindcompass.aiapi.registry.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

@Schema(description = "감정 모델 promotion checklist 검증 응답 DTO")
public record EmotionModelRegistryPromotionChecklistResponse(
        @Schema(description = "registry id", example = "2")
        Long registryId,
        @Schema(description = "experiment 이름", example = "cpu_compare_medium_manual_seed_v2")
        String experimentName,
        @Schema(description = "현재 registry status", example = "APPROVED")
        String status,
        @Schema(description = "현재 active baseline registry id", example = "1", nullable = true)
        Long baselineRegistryId,
        @Schema(description = "현재 active baseline experiment 이름", example = "cpu_compare_medium_relabel_weighted", nullable = true)
        String baselineExperimentName,
        @Schema(description = "운영 추천 결과", example = "SHADOW_ONLY")
        String recommendation,
        @Schema(description = "shadow 후보로는 진행 가능한지 여부", example = "true")
        boolean readyForShadow,
        @Schema(description = "active 승격 후보로는 진행 가능한지 여부", example = "false")
        boolean readyForActive,
        @Schema(description = "체크리스트 통과 개수", example = "6")
        int passedCount,
        @Schema(description = "체크리스트 총 개수", example = "9")
        int totalCount,
        @Schema(description = "체크리스트 개별 결과 목록")
        List<EmotionModelRegistryPromotionChecklistItemResponse> items
) {
}
