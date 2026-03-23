package com.mindcompass.api.diary.dto.response;

import java.time.LocalDate;
import java.util.List;

// 특정 날짜의 일기 목록을 감싸서 내려주는 응답 DTO입니다.
public record DiaryListResponse(
        LocalDate date,
        int count,
        List<DiarySummaryResponse> diaries
) {

    public static DiaryListResponse of(LocalDate date, List<DiarySummaryResponse> diaries) {
        return new DiaryListResponse(date, diaries.size(), diaries);
    }
}
