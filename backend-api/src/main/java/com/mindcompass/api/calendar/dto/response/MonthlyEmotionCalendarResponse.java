package com.mindcompass.api.calendar.dto.response;

import java.util.List;

// 한 달치 감정 캘린더 셀 데이터를 내려주는 응답 DTO입니다.
public record MonthlyEmotionCalendarResponse(
        int year,
        int month,
        List<CalendarDayEmotionResponse> days
) {
}
