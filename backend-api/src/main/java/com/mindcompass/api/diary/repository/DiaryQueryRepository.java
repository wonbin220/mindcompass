package com.mindcompass.api.diary.repository;

import com.mindcompass.api.diary.dto.response.DiarySummaryResponse;
import java.time.LocalDate;
import java.util.List;

// 날짜별 목록과 월간 캘린더처럼 화면 친화적인 일기 조회 모델을 제공하는 조회 저장소 인터페이스입니다.
public interface DiaryQueryRepository {

    List<DiarySummaryResponse> findDailySummaries(Long userId, LocalDate date);

    List<DiarySummaryResponse> findMonthlySummaries(Long userId, LocalDate startDate, LocalDate endDate);
}
