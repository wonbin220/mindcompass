package com.mindcompass.api.diary.dto.request;

import com.mindcompass.api.diary.domain.PrimaryEmotion;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;
import java.util.List;

// 일기 생성 시 본문과 감정 정보를 받는 요청 DTO입니다.
public record CreateDiaryRequest(
        @NotBlank(message = "title is required")
        @Size(max = 100, message = "title must be 100 characters or less")
        String title,

        @NotBlank(message = "content is required")
        @Size(max = 10000, message = "content must be 10000 characters or less")
        String content,

        PrimaryEmotion primaryEmotion,

        @Max(value = 5, message = "emotionIntensity must be 5 or less")
        Integer emotionIntensity,

        List<EmotionTagRequest> emotionTags,

        @NotNull(message = "writtenAt is required")
        LocalDateTime writtenAt
) {
}
