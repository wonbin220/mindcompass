// 감정분류 모델 registry 상태 변경 요청 DTO이다.
package com.mindcompass.aiapi.registry.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "감정 모델 registry 상태 변경 요청 DTO")
public record UpdateEmotionModelRegistryStatusRequest(
        @Schema(description = "변경할 목표 status", example = "APPROVED", allowableValues = {"TRAINED", "APPROVED", "REJECTED", "SHADOW", "ARCHIVED"})
        @NotBlank String status,
        @Schema(description = "승인 메모", example = "fixed compare gate reviewed and approved for manual activation")
        String approvalNote,
        @Schema(description = "반려 사유", example = "fixed compare gate did not beat current baseline")
        String rejectionReason,
        @Schema(description = "serving 운영 메모", example = "keep TIRED fallback-only")
        String servingNotes
) {
}
