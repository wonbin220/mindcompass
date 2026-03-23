package com.mindcompass.api.calendar.dto.response;

import com.mindcompass.api.diary.domain.PrimaryEmotion;
import com.mindcompass.api.diary.dto.response.EmotionTagResponse;
import java.time.LocalDate;
import java.util.List;

// 캘린더 하루 셀에 필요한 감정 요약 정보를 담는 응답 DTO입니다.
public record CalendarDayEmotionResponse(
        LocalDate date,
        boolean hasDiary,
        int diaryCount,
        PrimaryEmotion primaryEmotion,
        Integer averageIntensity,
        List<EmotionTagResponse> emotionTags
) {

    public static CalendarDayEmotionResponse empty(LocalDate date) {
        return new CalendarDayEmotionResponse(date, false, 0, null, null, List.of());
    }
}
