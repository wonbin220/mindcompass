package com.mindcompass.api.report.service;

// 리포트 화면의 월간 요약과 추이 응답을 조립하고 조회 메트릭을 기록하는 서비스다.

import com.mindcompass.api.common.exception.InvalidReportRequestException;
import com.mindcompass.api.common.logging.RequestTraceContext;
import com.mindcompass.api.common.metrics.AppMetricsRecorder;
import com.mindcompass.api.diary.dto.response.DiarySummaryResponse;
import com.mindcompass.api.diary.repository.DiaryQueryRepository;
import com.mindcompass.api.report.dto.response.EmotionTrendPointResponse;
import com.mindcompass.api.report.dto.response.MonthlyReportResponse;
import com.mindcompass.api.report.dto.response.MonthlyRiskTrendResponse;
import com.mindcompass.api.report.dto.response.RiskSummaryResponse;
import com.mindcompass.api.report.dto.response.RiskTrendEntryResponse;
import com.mindcompass.api.report.dto.response.RiskTrendPointResponse;
import com.mindcompass.api.report.dto.response.WeeklyEmotionTrendResponse;
import com.mindcompass.api.report.repository.ReportQueryRepository;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class ReportService {

    private static final Logger log = LoggerFactory.getLogger(ReportService.class);

    private final ReportQueryRepository reportQueryRepository;
    private final DiaryQueryRepository diaryQueryRepository;
    private final AppMetricsRecorder appMetricsRecorder;

    public ReportService(
            ReportQueryRepository reportQueryRepository,
            DiaryQueryRepository diaryQueryRepository,
            AppMetricsRecorder appMetricsRecorder
    ) {
        this.reportQueryRepository = reportQueryRepository;
        this.diaryQueryRepository = diaryQueryRepository;
        this.appMetricsRecorder = appMetricsRecorder;
    }

    public MonthlyReportResponse getMonthlySummary(Long userId, int year, int month) {
        validateYearMonth(year, month);

        LocalDate startDate = LocalDate.of(year, month, 1);
        LocalDate endDate = startDate.withDayOfMonth(startDate.lengthOfMonth());

        long diaryCount = reportQueryRepository.countMonthlyDiaries(userId, startDate, endDate);
        RiskSummaryResponse riskSummary = reportQueryRepository.findMonthlyRiskSummary(userId, startDate, endDate);

        MonthlyReportResponse response = new MonthlyReportResponse(
                year,
                month,
                diaryCount,
                reportQueryRepository.findAverageMonthlyEmotionIntensity(userId, startDate, endDate),
                reportQueryRepository.findTopPrimaryEmotions(userId, startDate, endDate, 3),
                riskSummary
        );

        log.info(
                "Monthly report loaded. requestId={}, userId={}, year={}, month={}, diaryCount={}",
                RequestTraceContext.currentRequestId(),
                userId,
                year,
                month,
                diaryCount
        );
        appMetricsRecorder.incrementReportQuery("monthly_summary");

        return response;
    }

    public WeeklyEmotionTrendResponse getWeeklyEmotionTrend(Long userId) {
        LocalDate endDate = LocalDate.now();
        LocalDate startDate = endDate.minusDays(6);

        Map<LocalDate, List<DiarySummaryResponse>> summariesByDate = diaryQueryRepository
                .findMonthlySummaries(userId, startDate, endDate)
                .stream()
                .collect(Collectors.groupingBy(
                        summary -> summary.writtenAt().toLocalDate(),
                        LinkedHashMap::new,
                        Collectors.toList()
                ));

        List<EmotionTrendPointResponse> items = startDate.datesUntil(endDate.plusDays(1))
                .map(date -> buildEmotionTrendPoint(date, summariesByDate.get(date)))
                .toList();

        WeeklyEmotionTrendResponse response = new WeeklyEmotionTrendResponse(startDate, endDate, items);

        log.info(
                "Weekly emotion trend loaded. requestId={}, userId={}, startDate={}, endDate={}, itemCount={}",
                RequestTraceContext.currentRequestId(),
                userId,
                startDate,
                endDate,
                items.size()
        );
        appMetricsRecorder.incrementReportQuery("weekly_emotions");

        return response;
    }

    public MonthlyRiskTrendResponse getMonthlyRiskTrend(Long userId, int year, int month) {
        validateYearMonth(year, month);

        YearMonth yearMonth = YearMonth.of(year, month);
        LocalDate startDate = yearMonth.atDay(1);
        LocalDate endDate = yearMonth.atEndOfMonth();

        Map<LocalDate, List<RiskTrendEntryResponse>> entriesByDate = reportQueryRepository
                .findMonthlyRiskEntries(userId, startDate, endDate)
                .stream()
                .collect(Collectors.groupingBy(
                        entry -> entry.writtenAt().toLocalDate(),
                        LinkedHashMap::new,
                        Collectors.toList()
                ));

        List<RiskTrendPointResponse> items = startDate.datesUntil(endDate.plusDays(1))
                .map(date -> buildRiskTrendPoint(date, entriesByDate.get(date)))
                .toList();

        MonthlyRiskTrendResponse response = new MonthlyRiskTrendResponse(year, month, items);

        log.info(
                "Monthly risk trend loaded. requestId={}, userId={}, year={}, month={}, itemCount={}",
                RequestTraceContext.currentRequestId(),
                userId,
                year,
                month,
                items.size()
        );
        appMetricsRecorder.incrementReportQuery("monthly_risks");

        return response;
    }

    private EmotionTrendPointResponse buildEmotionTrendPoint(LocalDate date, List<DiarySummaryResponse> summaries) {
        if (summaries == null || summaries.isEmpty()) {
            return EmotionTrendPointResponse.empty(date);
        }

        DiarySummaryResponse latestDiary = summaries.stream()
                .max(Comparator.comparing(DiarySummaryResponse::writtenAt))
                .orElseThrow();

        Integer averageIntensity = summaries.stream()
                .map(DiarySummaryResponse::emotionIntensity)
                .filter(intensity -> intensity != null)
                .collect(Collectors.collectingAndThen(Collectors.toList(), intensities -> {
                    if (intensities.isEmpty()) {
                        return null;
                    }

                    int sum = intensities.stream().mapToInt(Integer::intValue).sum();
                    return Math.round((float) sum / intensities.size());
                }));

        return new EmotionTrendPointResponse(
                date,
                true,
                summaries.size(),
                latestDiary.primaryEmotion(),
                averageIntensity
        );
    }

    private RiskTrendPointResponse buildRiskTrendPoint(LocalDate date, List<RiskTrendEntryResponse> entries) {
        if (entries == null || entries.isEmpty()) {
            return new RiskTrendPointResponse(date, 0, 0);
        }

        long mediumCount = entries.stream()
                .filter(entry -> "MEDIUM".equalsIgnoreCase(entry.riskLevel()))
                .count();
        long highCount = entries.stream()
                .filter(entry -> "HIGH".equalsIgnoreCase(entry.riskLevel()))
                .count();

        return new RiskTrendPointResponse(date, mediumCount, highCount);
    }

    private void validateYearMonth(int year, int month) {
        if (year < 2000 || year > 2100) {
            throw new InvalidReportRequestException("year 값 범위가 올바르지 않습니다.");
        }
        if (month < 1 || month > 12) {
            throw new InvalidReportRequestException("month 값 범위가 올바르지 않습니다.");
        }
    }
}
