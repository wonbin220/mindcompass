package com.mindcompass.api.diary.repository;

import com.mindcompass.api.diary.dto.response.DiarySummaryResponse;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.stereotype.Repository;

@Repository
// 날짜 범위 기준으로 일기 요약 목록을 조회하는 QueryRepository 구현체입니다.
public class DiaryQueryRepositoryImpl implements DiaryQueryRepository {

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    public List<DiarySummaryResponse> findDailySummaries(Long userId, LocalDate date) {
        return findSummariesBetween(userId, date.atStartOfDay(), date.plusDays(1).atStartOfDay());
    }

    @Override
    public List<DiarySummaryResponse> findMonthlySummaries(Long userId, LocalDate startDate, LocalDate endDate) {
        return findSummariesBetween(userId, startDate.atStartOfDay(), endDate.plusDays(1).atStartOfDay());
    }

    private List<DiarySummaryResponse> findSummariesBetween(
            Long userId,
            LocalDateTime startDateTime,
            LocalDateTime endDateTime
    ) {
        // 일기 목록/캘린더 조회에서 공통으로 재사용하는 일기 요약 JPQL입니다.
        return entityManager.createQuery("""
                select new com.mindcompass.api.diary.dto.response.DiarySummaryResponse(
                    d.id,
                    d.title,
                    substring(d.content, 1, 80),
                    d.primaryEmotion,
                    d.emotionIntensity,
                    d.writtenAt
                )
                from Diary d
                where d.user.id = :userId
                  and d.deletedAt is null
                  and d.writtenAt >= :startDateTime
                  and d.writtenAt < :endDateTime
                order by d.writtenAt desc
                """, DiarySummaryResponse.class)
                .setParameter("userId", userId)
                .setParameter("startDateTime", startDateTime)
                .setParameter("endDateTime", endDateTime)
                .getResultList();
    }
}
