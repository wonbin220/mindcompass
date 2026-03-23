package com.mindcompass.api.report.repository;

// 월간 리포트와 위험도 추이를 JPQL로 조회하는 QueryRepository 구현체다.

import com.mindcompass.api.report.dto.response.EmotionCountResponse;
import com.mindcompass.api.report.dto.response.RiskSummaryResponse;
import com.mindcompass.api.report.dto.response.RiskTrendEntryResponse;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.stereotype.Repository;

@Repository
public class ReportQueryRepositoryImpl implements ReportQueryRepository {

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    public long countMonthlyDiaries(Long userId, LocalDate startDate, LocalDate endDate) {
        return entityManager.createQuery("""
                select count(d)
                from Diary d
                where d.user.id = :userId
                  and d.deletedAt is null
                  and d.writtenAt >= :startDateTime
                  and d.writtenAt < :endDateTime
                """, Long.class)
                .setParameter("userId", userId)
                .setParameter("startDateTime", toStartDateTime(startDate))
                .setParameter("endDateTime", toEndExclusiveDateTime(endDate))
                .getSingleResult();
    }

    @Override
    public BigDecimal findAverageMonthlyEmotionIntensity(Long userId, LocalDate startDate, LocalDate endDate) {
        Double average = entityManager.createQuery("""
                select avg(d.emotionIntensity)
                from Diary d
                where d.user.id = :userId
                  and d.deletedAt is null
                  and d.writtenAt >= :startDateTime
                  and d.writtenAt < :endDateTime
                """, Double.class)
                .setParameter("userId", userId)
                .setParameter("startDateTime", toStartDateTime(startDate))
                .setParameter("endDateTime", toEndExclusiveDateTime(endDate))
                .getSingleResult();

        return average == null ? null : BigDecimal.valueOf(average).setScale(2, RoundingMode.HALF_UP);
    }

    @Override
    public List<EmotionCountResponse> findTopPrimaryEmotions(
            Long userId,
            LocalDate startDate,
            LocalDate endDate,
            int limit
    ) {
        return entityManager.createQuery("""
                select new com.mindcompass.api.report.dto.response.EmotionCountResponse(
                    d.primaryEmotion,
                    count(d)
                )
                from Diary d
                where d.user.id = :userId
                  and d.deletedAt is null
                  and d.writtenAt >= :startDateTime
                  and d.writtenAt < :endDateTime
                group by d.primaryEmotion
                order by count(d) desc, d.primaryEmotion asc
                """, EmotionCountResponse.class)
                .setParameter("userId", userId)
                .setParameter("startDateTime", toStartDateTime(startDate))
                .setParameter("endDateTime", toEndExclusiveDateTime(endDate))
                .setMaxResults(limit)
                .getResultList();
    }

    @Override
    public RiskSummaryResponse findMonthlyRiskSummary(Long userId, LocalDate startDate, LocalDate endDate) {
        List<Object[]> rows = entityManager.createQuery("""
                select a.riskLevel, count(a)
                from DiaryAiAnalysis a
                join a.diary d
                where d.user.id = :userId
                  and d.deletedAt is null
                  and d.writtenAt >= :startDateTime
                  and d.writtenAt < :endDateTime
                  and a.riskLevel in ('MEDIUM', 'HIGH')
                group by a.riskLevel
                """, Object[].class)
                .setParameter("userId", userId)
                .setParameter("startDateTime", toStartDateTime(startDate))
                .setParameter("endDateTime", toEndExclusiveDateTime(endDate))
                .getResultList();

        long mediumCount = 0L;
        long highCount = 0L;

        for (Object[] row : rows) {
            String riskLevel = (String) row[0];
            long count = (Long) row[1];
            if ("MEDIUM".equalsIgnoreCase(riskLevel)) {
                mediumCount = count;
            } else if ("HIGH".equalsIgnoreCase(riskLevel)) {
                highCount = count;
            }
        }

        return new RiskSummaryResponse(mediumCount, highCount);
    }

    @Override
    public List<RiskTrendEntryResponse> findMonthlyRiskEntries(Long userId, LocalDate startDate, LocalDate endDate) {
        return entityManager.createQuery("""
                select new com.mindcompass.api.report.dto.response.RiskTrendEntryResponse(
                    a.riskLevel,
                    d.writtenAt
                )
                from DiaryAiAnalysis a
                join a.diary d
                where d.user.id = :userId
                  and d.deletedAt is null
                  and d.writtenAt >= :startDateTime
                  and d.writtenAt < :endDateTime
                  and a.riskLevel in ('MEDIUM', 'HIGH')
                order by d.writtenAt asc
                """, RiskTrendEntryResponse.class)
                .setParameter("userId", userId)
                .setParameter("startDateTime", toStartDateTime(startDate))
                .setParameter("endDateTime", toEndExclusiveDateTime(endDate))
                .getResultList();
    }

    private LocalDateTime toStartDateTime(LocalDate startDate) {
        return startDate.atStartOfDay();
    }

    private LocalDateTime toEndExclusiveDateTime(LocalDate endDate) {
        return endDate.plusDays(1).atStartOfDay();
    }
}
