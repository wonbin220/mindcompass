package com.mindcompass.api.calendar;

// Calendar 조회 집계와 빈 날짜 응답을 검증하는 서비스 테스트다.

import com.mindcompass.api.auth.domain.User;
import com.mindcompass.api.auth.repository.UserRepository;
import com.mindcompass.api.calendar.dto.response.DailyEmotionSummaryResponse;
import com.mindcompass.api.calendar.dto.response.MonthlyEmotionCalendarResponse;
import com.mindcompass.api.calendar.service.CalendarService;
import com.mindcompass.api.common.exception.InvalidCalendarRequestException;
import com.mindcompass.api.diary.domain.PrimaryEmotion;
import com.mindcompass.api.diary.dto.response.DiarySummaryResponse;
import com.mindcompass.api.diary.repository.DiaryEmotionRepository;
import com.mindcompass.api.diary.repository.DiaryQueryRepository;
import java.lang.reflect.Field;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CalendarServiceTest {

    @Mock
    private DiaryQueryRepository diaryQueryRepository;

    @Mock
    private DiaryEmotionRepository diaryEmotionRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private CalendarService calendarService;

    @Test
    void getMonthlyEmotionsBuildsDaySummariesAcrossMonth() throws Exception {
        User user = createUser(1L);
        LocalDateTime firstWrittenAt = LocalDateTime.of(2026, 3, 5, 9, 0);
        LocalDateTime latestWrittenAt = LocalDateTime.of(2026, 3, 5, 22, 0);

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(diaryQueryRepository.findMonthlySummaries(1L, LocalDate.of(2026, 3, 1), LocalDate.of(2026, 3, 31)))
                .thenReturn(List.of(
                        new DiarySummaryResponse(10L, "Morning diary", "preview", PrimaryEmotion.SAD, 2, firstWrittenAt),
                        new DiarySummaryResponse(11L, "Night diary", "preview", PrimaryEmotion.CALM, 4, latestWrittenAt)
                ));
        when(diaryEmotionRepository.findAllByDiaryIdInOrderByDiaryIdAscCreatedAtAsc(List.of(10L, 11L)))
                .thenReturn(List.of());

        MonthlyEmotionCalendarResponse response = calendarService.getMonthlyEmotions(1L, 2026, 3);

        assertThat(response.year()).isEqualTo(2026);
        assertThat(response.month()).isEqualTo(3);
        assertThat(response.days()).hasSize(31);
        assertThat(response.days().get(4).date()).isEqualTo(LocalDate.of(2026, 3, 5));
        assertThat(response.days().get(4).hasDiary()).isTrue();
        assertThat(response.days().get(4).diaryCount()).isEqualTo(2);
        assertThat(response.days().get(4).primaryEmotion()).isEqualTo(PrimaryEmotion.CALM);
        assertThat(response.days().get(4).averageIntensity()).isEqualTo(3);
        assertThat(response.days().get(0).hasDiary()).isFalse();
    }

    @Test
    void getDailySummaryReturnsEmptyWhenNoDiaryExists() throws Exception {
        User user = createUser(1L);
        LocalDate date = LocalDate.of(2026, 3, 6);

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(diaryQueryRepository.findDailySummaries(1L, date)).thenReturn(List.of());

        DailyEmotionSummaryResponse response = calendarService.getDailySummary(1L, date);

        assertThat(response.date()).isEqualTo(date);
        assertThat(response.hasDiary()).isFalse();
        assertThat(response.diaryCount()).isZero();
        assertThat(response.latestDiary()).isNull();
    }

    @Test
    void getMonthlyEmotionsRejectsInvalidMonth() throws Exception {
        User user = createUser(1L);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> calendarService.getMonthlyEmotions(1L, 2026, 13))
                .isInstanceOf(InvalidCalendarRequestException.class);
    }

    private User createUser(Long id) throws Exception {
        User user = User.create("calendar@example.com", "encoded-password", "tester");
        setField(user, "id", id);
        return user;
    }

    private void setField(Object target, String fieldName, Object value) throws Exception {
        Field field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);
    }
}
