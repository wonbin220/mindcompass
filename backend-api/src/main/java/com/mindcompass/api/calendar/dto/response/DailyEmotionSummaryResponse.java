package com.mindcompass.api.calendar.dto.response;

import com.mindcompass.api.diary.domain.PrimaryEmotion;
import com.mindcompass.api.diary.dto.response.DiarySummaryResponse;
import com.mindcompass.api.diary.dto.response.EmotionTagResponse;
import java.time.LocalDate;
import java.util.List;

// 캘린더에서 특정 날짜를 눌렀을 때 보여줄 하루 감정 요약 응답 DTO입니다.
public record DailyEmotionSummaryResponse(
        LocalDate date,
        boolean hasDiary,
        int diaryCount,
        PrimaryEmotion primaryEmotion,
        Integer averageIntensity,
        List<EmotionTagResponse> emotionTags,
        DiarySummaryResponse latestDiary
) {

    public static DailyEmotionSummaryResponse empty(LocalDate date) {
        return new DailyEmotionSummaryResponse(date, false, 0, null, null, List.of(), null);
    }
}
