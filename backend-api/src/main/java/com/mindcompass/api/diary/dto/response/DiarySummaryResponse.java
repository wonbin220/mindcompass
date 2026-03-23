package com.mindcompass.api.diary.dto.response;

import com.mindcompass.api.diary.domain.PrimaryEmotion;
import java.time.LocalDateTime;
import java.util.List;

// 날짜별 목록이나 캘린더 상세에서 쓰는 일기 요약 응답 DTO입니다.
public record DiarySummaryResponse(
        Long diaryId,
        String title,
        String preview,
        PrimaryEmotion primaryEmotion,
        Integer emotionIntensity,
        List<EmotionTagResponse> emotionTags,
        LocalDateTime writtenAt
) {

    public DiarySummaryResponse(
            Long diaryId,
            String title,
            String preview,
            PrimaryEmotion primaryEmotion,
            Integer emotionIntensity,
            LocalDateTime writtenAt
    ) {
        this(diaryId, title, preview, primaryEmotion, emotionIntensity, List.of(), writtenAt);
    }

    public DiarySummaryResponse withEmotionTags(List<EmotionTagResponse> emotionTags) {
        return new DiarySummaryResponse(
                diaryId,
                title,
                preview,
                primaryEmotion,
                emotionIntensity,
                emotionTags,
                writtenAt
        );
    }
}
