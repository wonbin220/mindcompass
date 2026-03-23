package com.mindcompass.api.security;

// 유효한 JWT로 로그인해도 다른 사용자 리소스에는 접근할 수 없음을 검증하는 통합 테스트다.

import com.mindcompass.api.auth.domain.User;
import com.mindcompass.api.auth.repository.UserRepository;
import com.mindcompass.api.auth.security.JwtTokenProvider;
import com.mindcompass.api.chat.domain.ChatSession;
import com.mindcompass.api.chat.repository.ChatSessionRepository;
import com.mindcompass.api.diary.domain.Diary;
import com.mindcompass.api.diary.domain.PrimaryEmotion;
import com.mindcompass.api.diary.repository.DiaryRepository;
import java.time.LocalDate;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureMockMvc
class OwnershipIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private DiaryRepository diaryRepository;

    @Autowired
    private ChatSessionRepository chatSessionRepository;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @Test
    void getDiaryReturnsNotFoundForOtherUsersDiary() throws Exception {
        User owner = userRepository.save(User.create("owner-diary@test.com", "hash", "ownerDiary"));
        User viewer = userRepository.save(User.create("viewer-diary@test.com", "hash", "viewerDiary"));

        Diary diary = diaryRepository.save(Diary.create(
                owner,
                "owner diary",
                "private diary",
                PrimaryEmotion.ANXIOUS,
                4,
                LocalDateTime.now().minusDays(1)
        ));

        mockMvc.perform(get("/api/v1/diaries/{diaryId}", diary.getId())
                        .header("Authorization", bearer(viewer))
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("일기를 찾을 수 없습니다."));
    }

    @Test
    void getChatSessionReturnsNotFoundForOtherUsersSession() throws Exception {
        User owner = userRepository.save(User.create("owner-chat@test.com", "hash", "ownerChat"));
        User viewer = userRepository.save(User.create("viewer-chat@test.com", "hash", "viewerChat"));

        ChatSession session = chatSessionRepository.save(ChatSession.create(owner, null, "owner session"));

        mockMvc.perform(get("/api/v1/chat/sessions/{sessionId}", session.getId())
                        .header("Authorization", bearer(viewer))
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("채팅 세션을 찾을 수 없습니다."));
    }

    @Test
    void monthlyReportCountsOnlyAuthenticatedUsersDiaries() throws Exception {
        LocalDateTime writtenAt = LocalDate.now().atTime(10, 0);
        int year = writtenAt.getYear();
        int month = writtenAt.getMonthValue();

        User owner = userRepository.save(User.create("owner-report@test.com", "hash", "ownerReport"));
        User other = userRepository.save(User.create("other-report@test.com", "hash", "otherReport"));

        diaryRepository.save(Diary.create(
                owner,
                "owner report diary",
                "owner content",
                PrimaryEmotion.CALM,
                2,
                writtenAt
        ));
        diaryRepository.save(Diary.create(
                other,
                "other report diary",
                "other content",
                PrimaryEmotion.SAD,
                5,
                writtenAt
        ));

        mockMvc.perform(get("/api/v1/reports/monthly-summary")
                        .header("Authorization", bearer(owner))
                        .param("year", String.valueOf(year))
                        .param("month", String.valueOf(month))
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.year").value(year))
                .andExpect(jsonPath("$.month").value(month))
                .andExpect(jsonPath("$.diaryCount").value(1));
    }

    private String bearer(User user) {
        return "Bearer " + jwtTokenProvider.generateAccessToken(user).token();
    }
}
