package com.mindcompass.api.diary.dto.response;

import com.mindcompass.api.diary.domain.DiaryEmotionSourceType;
import com.mindcompass.api.diary.domain.PrimaryEmotion;

// 감정 태그 하나를 화면에 내려주기 위한 응답 DTO입니다.
public record EmotionTagResponse(
        PrimaryEmotion emotionCode,
        Integer intensity,
        DiaryEmotionSourceType sourceType
) {
}
