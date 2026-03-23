package com.mindcompass.api.report;

// ReportService의 월간 요약 조립과 파라미터 검증을 확인하는 테스트다.

import com.mindcompass.api.common.exception.InvalidReportRequestException;
import com.mindcompass.api.common.metrics.AppMetricsRecorder;
import com.mindcompass.api.diary.domain.PrimaryEmotion;
import com.mindcompass.api.diary.dto.response.DiarySummaryResponse;
import com.mindcompass.api.diary.repository.DiaryQueryRepository;
import com.mindcompass.api.report.dto.response.EmotionCountResponse;
import com.mindcompass.api.report.dto.response.MonthlyRiskTrendResponse;
import com.mindcompass.api.report.dto.response.MonthlyReportResponse;
import com.mindcompass.api.report.dto.response.RiskSummaryResponse;
import com.mindcompass.api.report.dto.response.RiskTrendEntryResponse;
import com.mindcompass.api.report.dto.response.WeeklyEmotionTrendResponse;
import com.mindcompass.api.report.repository.ReportQueryRepository;
import com.mindcompass.api.report.service.ReportService;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReportServiceTest {

    @Mock
    private ReportQueryRepository reportQueryRepository;

    @Mock
    private DiaryQueryRepository diaryQueryRepository;

    @Mock
    private AppMetricsRecorder appMetricsRecorder;

    @InjectMocks
    private ReportService reportService;

    @Test
    void getMonthlySummaryBuildsResponseFromQueryResults() {
        when(reportQueryRepository.countMonthlyDiaries(1L, LocalDate.of(2026, 3, 1), LocalDate.of(2026, 3, 31)))
                .thenReturn(12L);
        when(reportQueryRepository.findAverageMonthlyEmotionIntensity(1L, LocalDate.of(2026, 3, 1), LocalDate.of(2026, 3, 31)))
                .thenReturn(BigDecimal.valueOf(3.58));
        when(reportQueryRepository.findTopPrimaryEmotions(1L, LocalDate.of(2026, 3, 1), LocalDate.of(2026, 3, 31), 3))
                .thenReturn(List.of(
                        new EmotionCountResponse(PrimaryEmotion.ANXIOUS, 4),
                        new EmotionCountResponse(PrimaryEmotion.CALM, 3)
                ));
        when(reportQueryRepository.findMonthlyRiskSummary(1L, LocalDate.of(2026, 3, 1), LocalDate.of(2026, 3, 31)))
                .thenReturn(new RiskSummaryResponse(2, 1));

        MonthlyReportResponse response = reportService.getMonthlySummary(1L, 2026, 3);

        assertThat(response.diaryCount()).isEqualTo(12L);
        assertThat(response.averageEmotionIntensity()).isEqualByComparingTo("3.58");
        assertThat(response.topPrimaryEmotions()).hasSize(2);
        assertThat(response.riskSummary().mediumCount()).isEqualTo(2);
        assertThat(response.riskSummary().highCount()).isEqualTo(1);
    }

    @Test
    void getMonthlySummaryRejectsInvalidMonth() {
        assertThatThrownBy(() -> reportService.getMonthlySummary(1L, 2026, 13))
                .isInstanceOf(InvalidReportRequestException.class);
    }

    @Test
    void getMonthlySummaryUsesMonthRange() {
        when(reportQueryRepository.countMonthlyDiaries(1L, LocalDate.of(2026, 4, 1), LocalDate.of(2026, 4, 30)))
                .thenReturn(0L);
        when(reportQueryRepository.findAverageMonthlyEmotionIntensity(1L, LocalDate.of(2026, 4, 1), LocalDate.of(2026, 4, 30)))
                .thenReturn(null);
        when(reportQueryRepository.findTopPrimaryEmotions(1L, LocalDate.of(2026, 4, 1), LocalDate.of(2026, 4, 30), 3))
                .thenReturn(List.of());
        when(reportQueryRepository.findMonthlyRiskSummary(1L, LocalDate.of(2026, 4, 1), LocalDate.of(2026, 4, 30)))
                .thenReturn(new RiskSummaryResponse(0, 0));

        reportService.getMonthlySummary(1L, 2026, 4);

        verify(reportQueryRepository).countMonthlyDiaries(1L, LocalDate.of(2026, 4, 1), LocalDate.of(2026, 4, 30));
    }

    @Test
    void getWeeklyEmotionTrendReturnsSevenItems() {
        when(diaryQueryRepository.findMonthlySummaries(1L, LocalDate.now().minusDays(6), LocalDate.now()))
                .thenReturn(List.of(
                        new DiarySummaryResponse(
                                1L,
                                "테스트",
                                "본문",
                                PrimaryEmotion.ANXIOUS,
                                4,
                                LocalDateTime.now().minusDays(1)
                        )
                ));

        WeeklyEmotionTrendResponse response = reportService.getWeeklyEmotionTrend(1L);

        assertThat(response.items()).hasSize(7);
    }

    @Test
    void getMonthlyRiskTrendAggregatesDailyCounts() {
        when(reportQueryRepository.findMonthlyRiskEntries(1L, LocalDate.of(2026, 3, 1), LocalDate.of(2026, 3, 31)))
                .thenReturn(List.of(
                        new RiskTrendEntryResponse("MEDIUM", LocalDateTime.of(2026, 3, 5, 10, 0)),
                        new RiskTrendEntryResponse("HIGH", LocalDateTime.of(2026, 3, 5, 11, 0))
                ));

        MonthlyRiskTrendResponse response = reportService.getMonthlyRiskTrend(1L, 2026, 3);

        assertThat(response.items()).hasSize(31);
        assertThat(response.items().stream().filter(item -> item.date().equals(LocalDate.of(2026, 3, 5))).findFirst().orElseThrow().mediumCount())
                .isEqualTo(1);
        assertThat(response.items().stream().filter(item -> item.date().equals(LocalDate.of(2026, 3, 5))).findFirst().orElseThrow().highCount())
                .isEqualTo(1);
    }
}
