package com.mindcompass.api.diary.repository;

import com.mindcompass.api.diary.domain.Diary;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

// 일기 본문 엔티티를 사용자 소유권 기준으로 조회하는 저장소 인터페이스입니다.
public interface DiaryRepository extends JpaRepository<Diary, Long> {

    Optional<Diary> findByIdAndUserIdAndDeletedAtIsNull(Long diaryId, Long userId);
}
