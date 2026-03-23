package com.mindcompass.api.report.repository;

// 월간 리포트와 위험도 추이 집계를 조회하는 QueryRepository 인터페이스다.

import com.mindcompass.api.report.dto.response.EmotionCountResponse;
import com.mindcompass.api.report.dto.response.RiskSummaryResponse;
import com.mindcompass.api.report.dto.response.RiskTrendEntryResponse;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public interface ReportQueryRepository {

    long countMonthlyDiaries(Long userId, LocalDate startDate, LocalDate endDate);

    BigDecimal findAverageMonthlyEmotionIntensity(Long userId, LocalDate startDate, LocalDate endDate);

    List<EmotionCountResponse> findTopPrimaryEmotions(Long userId, LocalDate startDate, LocalDate endDate, int limit);

    RiskSummaryResponse findMonthlyRiskSummary(Long userId, LocalDate startDate, LocalDate endDate);

    List<RiskTrendEntryResponse> findMonthlyRiskEntries(Long userId, LocalDate startDate, LocalDate endDate);
}
