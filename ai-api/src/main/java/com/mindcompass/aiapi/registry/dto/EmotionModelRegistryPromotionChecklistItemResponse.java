// 감정 분류 모델 promotion checklist 개별 점검 결과를 담는 응답 DTO다.
package com.mindcompass.aiapi.registry.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "감정 모델 promotion checklist 개별 점검 응답 DTO")
public record EmotionModelRegistryPromotionChecklistItemResponse(
        @Schema(description = "점검 항목 이름", example = "happyF1Gate")
        String name,
        @Schema(description = "점검 통과 여부", example = "false")
        boolean passed,
        @Schema(description = "점검 설명", example = "candidate HAPPY F1 0.2000 is lower than baseline 0.6146")
        String detail
) {
}
