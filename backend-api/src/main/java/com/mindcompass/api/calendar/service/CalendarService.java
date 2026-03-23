package com.mindcompass.api.calendar.service;

import com.mindcompass.api.auth.domain.User;
import com.mindcompass.api.auth.repository.UserRepository;
import com.mindcompass.api.calendar.dto.response.CalendarDayEmotionResponse;
import com.mindcompass.api.calendar.dto.response.DailyEmotionSummaryResponse;
import com.mindcompass.api.calendar.dto.response.MonthlyEmotionCalendarResponse;
import com.mindcompass.api.common.exception.InvalidCalendarRequestException;
import com.mindcompass.api.common.exception.ResourceNotFoundException;
import com.mindcompass.api.diary.dto.response.DiarySummaryResponse;
import com.mindcompass.api.diary.dto.response.EmotionTagResponse;
import com.mindcompass.api.diary.repository.DiaryEmotionRepository;
import com.mindcompass.api.diary.repository.DiaryQueryRepository;
import java.time.DateTimeException;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
// 월간 캘린더 화면에 맞는 감정 조회 응답을 조합하는 조회 서비스입니다.
public class CalendarService {

    private final DiaryQueryRepository diaryQueryRepository;
    private final DiaryEmotionRepository diaryEmotionRepository;
    private final UserRepository userRepository;

    public CalendarService(
            DiaryQueryRepository diaryQueryRepository,
            DiaryEmotionRepository diaryEmotionRepository,
            UserRepository userRepository
    ) {
        this.diaryQueryRepository = diaryQueryRepository;
        this.diaryEmotionRepository = diaryEmotionRepository;
        this.userRepository = userRepository;
    }

    public MonthlyEmotionCalendarResponse getMonthlyEmotions(Long userId, int year, int month) {
        User user = getActiveUser(userId);
        YearMonth yearMonth = toYearMonth(year, month);
        LocalDate startDate = yearMonth.atDay(1);
        LocalDate endDate = yearMonth.atEndOfMonth();

        List<DiarySummaryResponse> monthlySummaries = diaryQueryRepository.findMonthlySummaries(
                user.getId(),
                startDate,
                endDate
        );

        Map<Long, List<EmotionTagResponse>> emotionTagsByDiaryId = loadEmotionTagsByDiaryIds(
                monthlySummaries.stream().map(DiarySummaryResponse::diaryId).toList()
        );

        Map<LocalDate, List<DiarySummaryResponse>> summariesByDate = monthlySummaries.stream()
                .map(summary -> summary.withEmotionTags(
                        emotionTagsByDiaryId.getOrDefault(summary.diaryId(), Collections.emptyList())
                ))
                .collect(Collectors.groupingBy(
                        summary -> summary.writtenAt().toLocalDate(),
                        LinkedHashMap::new,
                        Collectors.toList()
                ));

        List<CalendarDayEmotionResponse> days = startDate.datesUntil(endDate.plusDays(1))
                .map(date -> buildDayResponse(date, summariesByDate.getOrDefault(date, List.of())))
                .toList();

        return new MonthlyEmotionCalendarResponse(year, month, days);
    }

    public DailyEmotionSummaryResponse getDailySummary(Long userId, LocalDate date) {
        User user = getActiveUser(userId);
        List<DiarySummaryResponse> dailySummaries = diaryQueryRepository.findDailySummaries(user.getId(), date);
        if (dailySummaries.isEmpty()) {
            return DailyEmotionSummaryResponse.empty(date);
        }

        Map<Long, List<EmotionTagResponse>> emotionTagsByDiaryId = loadEmotionTagsByDiaryIds(
                dailySummaries.stream().map(DiarySummaryResponse::diaryId).toList()
        );

        List<DiarySummaryResponse> enrichedSummaries = dailySummaries.stream()
                .map(summary -> summary.withEmotionTags(
                        emotionTagsByDiaryId.getOrDefault(summary.diaryId(), Collections.emptyList())
                ))
                .toList();

        DiarySummaryResponse latestDiary = enrichedSummaries.stream()
                .max(Comparator.comparing(DiarySummaryResponse::writtenAt))
                .orElseThrow();

        CalendarDayEmotionResponse dayResponse = buildDayResponse(date, enrichedSummaries);

        return new DailyEmotionSummaryResponse(
                date,
                dayResponse.hasDiary(),
                dayResponse.diaryCount(),
                dayResponse.primaryEmotion(),
                dayResponse.averageIntensity(),
                dayResponse.emotionTags(),
                latestDiary
        );
    }

    private User getActiveUser(Long userId) {
        return userRepository.findById(userId)
                .filter(User::isLoginAllowed)
                .orElseThrow(() -> new ResourceNotFoundException("사용자를 찾을 수 없습니다."));
    }

    private YearMonth toYearMonth(int year, int month) {
        try {
            return YearMonth.of(year, month);
        } catch (DateTimeException exception) {
            throw new InvalidCalendarRequestException("조회할 연월 값이 올바르지 않습니다.");
        }
    }

    private CalendarDayEmotionResponse buildDayResponse(LocalDate date, List<DiarySummaryResponse> summaries) {
        if (summaries.isEmpty()) {
            return CalendarDayEmotionResponse.empty(date);
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

        List<EmotionTagResponse> emotionTags = summaries.stream()
                .flatMap(summary -> summary.emotionTags().stream())
                .collect(Collectors.toMap(
                        tag -> tag.emotionCode().name(),
                        tag -> tag,
                        this::preferHigherIntensity,
                        LinkedHashMap::new
                ))
                .values()
                .stream()
                .toList();

        return new CalendarDayEmotionResponse(
                date,
                true,
                summaries.size(),
                latestDiary.primaryEmotion(),
                averageIntensity,
                emotionTags
        );
    }

    private EmotionTagResponse preferHigherIntensity(EmotionTagResponse left, EmotionTagResponse right) {
        Integer leftIntensity = left.intensity();
        Integer rightIntensity = right.intensity();

        if (leftIntensity == null) {
            return right;
        }
        if (rightIntensity == null) {
            return left;
        }
        return leftIntensity >= rightIntensity ? left : right;
    }

    private Map<Long, List<EmotionTagResponse>> loadEmotionTagsByDiaryIds(List<Long> diaryIds) {
        if (diaryIds.isEmpty()) {
            return Collections.emptyMap();
        }

        return diaryEmotionRepository.findAllByDiaryIdInOrderByDiaryIdAscCreatedAtAsc(diaryIds)
                .stream()
                .collect(Collectors.groupingBy(
                        tag -> tag.getDiary().getId(),
                        LinkedHashMap::new,
                        Collectors.mapping(
                                tag -> new EmotionTagResponse(
                                        tag.getEmotionCode(),
                                        tag.getIntensity(),
                                        tag.getSourceType()
                                ),
                                Collectors.toList()
                        )
                ));
    }
}
