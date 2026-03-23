package com.mindcompass.api.diary.dto.response;

// 일기 상세 화면에 본문, 감정 태그, 위험도 결과를 내려주는 응답 DTO다.

import com.mindcompass.api.diary.domain.Diary;
import com.mindcompass.api.diary.domain.DiaryAiAnalysis;
import com.mindcompass.api.diary.domain.PrimaryEmotion;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record DiaryDetailResponse(
        Long diaryId,
        Long userId,
        String title,
        String content,
        PrimaryEmotion primaryEmotion,
        Integer emotionIntensity,
        List<EmotionTagResponse> emotionTags,
        String riskLevel,
        BigDecimal riskScore,
        String riskSignals,
        String recommendedAction,
        LocalDateTime writtenAt,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {

    public static DiaryDetailResponse from(
            Diary diary,
            List<EmotionTagResponse> emotionTags,
            DiaryAiAnalysis diaryAiAnalysis
    ) {
        return new DiaryDetailResponse(
                diary.getId(),
                diary.getUser().getId(),
                diary.getTitle(),
                diary.getContent(),
                diary.getPrimaryEmotion(),
                diary.getEmotionIntensity(),
                emotionTags,
                diaryAiAnalysis == null ? null : diaryAiAnalysis.getRiskLevel(),
                diaryAiAnalysis == null ? null : diaryAiAnalysis.getRiskScore(),
                diaryAiAnalysis == null ? null : diaryAiAnalysis.getRiskSignals(),
                diaryAiAnalysis == null ? null : diaryAiAnalysis.getRecommendedAction(),
                diary.getWrittenAt(),
                diary.getCreatedAt(),
                diary.getUpdatedAt()
        );
    }
}
