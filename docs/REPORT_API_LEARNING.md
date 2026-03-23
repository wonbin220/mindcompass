# Report API 학습 문서

이 문서는 `Report / statistics` 도메인을 학습하기 위한 문서다.  
Report는 새로운 데이터를 저장하는 도메인이 아니라, 이미 쌓인 Diary와 Safety 데이터를 기간 단위로 요약해서 보여주는 조회 도메인이다.

---

# 1. 왜 Report API가 필요한가

사용자는 감정일기를 하나씩 읽는 것만으로는 전체 흐름을 파악하기 어렵다.  
보통은 아래 같은 질문을 하게 된다.

- 이번 달 일기를 몇 번 썼지?
- 어떤 감정이 가장 많이 나왔지?
- 감정 강도는 평균적으로 어땠지?
- 위험 신호가 있었던 달이었나?

이 질문에 답하는 것이 Report API다.

---

# 2. MVP 1차 엔드포인트

`GET /api/v1/reports/monthly-summary?year=2026&month=3`

MVP 2차에서 아래 엔드포인트를 추가했다.

- `GET /api/v1/reports/emotions/weekly`
- `GET /api/v1/reports/risks/monthly?year=2026&month=3`

---

# 3. 어떤 화면에서 쓰는가

- 월간 리포트 화면
- 통계 탭
- 홈 인사이트 카드의 상세 보기

---

# 4. 응답 예시

```json
{
  "year": 2026,
  "month": 3,
  "diaryCount": 12,
  "averageEmotionIntensity": 3.58,
  "topPrimaryEmotions": [
    { "emotion": "ANXIOUS", "count": 4 },
    { "emotion": "CALM", "count": 3 }
  ],
  "riskSummary": {
    "mediumCount": 2,
    "highCount": 1
  }
}
```

---

# 5. 관련 파일

- `backend-api/src/main/java/com/mindcompass/api/report/controller/ReportController.java`
- `backend-api/src/main/java/com/mindcompass/api/report/service/ReportService.java`
- `backend-api/src/main/java/com/mindcompass/api/report/repository/ReportQueryRepository.java`
- `backend-api/src/main/java/com/mindcompass/api/report/repository/ReportQueryRepositoryImpl.java`
- `backend-api/src/main/java/com/mindcompass/api/report/dto/response/MonthlyReportResponse.java`
- `backend-api/src/main/java/com/mindcompass/api/report/dto/response/EmotionCountResponse.java`
- `backend-api/src/main/java/com/mindcompass/api/report/dto/response/RiskSummaryResponse.java`
- `backend-api/src/main/java/com/mindcompass/api/report/dto/response/WeeklyEmotionTrendResponse.java`
- `backend-api/src/main/java/com/mindcompass/api/report/dto/response/EmotionTrendPointResponse.java`
- `backend-api/src/main/java/com/mindcompass/api/report/dto/response/MonthlyRiskTrendResponse.java`
- `backend-api/src/main/java/com/mindcompass/api/report/dto/response/RiskTrendPointResponse.java`

재사용 데이터:
- `Diary`
- `DiaryAiAnalysis`

---

# 6. 실행 순서

1. 앱이 `year`, `month`를 query parameter로 보낸다.
2. JWT 필터가 인증을 확인한다.
3. `ReportController.getMonthlySummary()`가 요청을 받는다.
4. Controller가 현재 사용자 ID를 확인한다.
5. `ReportService.getMonthlySummary(userId, year, month)`를 호출한다.
6. Service가 연/월 파라미터를 검증한다.
7. Service가 해당 월의 시작일과 종료일을 계산한다.
8. `ReportQueryRepository`가 diary 수, 평균 감정 강도, 대표 감정 빈도, 위험도 집계를 조회한다.
9. Service가 `MonthlyReportResponse`를 조립한다.
10. Controller가 응답을 반환한다.

---

# 7. 파일별 역할

## ReportController
- 리포트 API 엔드포인트 진입점

## ReportService
- 파라미터 검증
- 월 범위 계산
- 여러 집계 결과를 하나의 응답으로 조립

## ReportQueryRepository
- 월간 통계 조회 계약 정의

## ReportQueryRepositoryImpl
- JPQL로 실제 집계 수행

## MonthlyReportResponse
- 월간 리포트 화면이 바로 사용할 수 있는 응답 구조

---

# 8. DB read/write 영향

쓰기 없음.  
이 API는 조회 전용이다.

읽는 대상:
- `diaries`
- `diary_ai_analyses`

---

# 9. 예외 상황

- `year` 범위 오류
- `month` 범위 오류
- diary가 없는 달

정책:
- 잘못된 파라미터는 `400`
- 데이터가 없는 달은 `200` + 빈 집계

---

# 10. AI fallback과의 관계

이 API는 ai-api를 직접 호출하지 않는다.  
즉 Report 조회는 ai-api 장애에 직접 영향받지 않는다.

다만 이전 Diary/Chat 저장 시점에 AI 분석이 실패했다면:
- 위험도 집계가 일부 비어 있을 수 있다
- 그 경우도 Report API는 정상 응답을 반환한다

---

# 11. 학습 포인트

- Report는 저장 기능이 아니라 집계 조회 기능이다.
- Calendar가 날짜 중심 조회라면, Report는 기간 중심 요약 조회다.
- MVP에서는 한 번에 큰 통계 시스템을 만들기보다, `monthly-summary` 같은 작은 API부터 시작하는 게 유지보수에 유리하다.
- 2차 확장에서는 `weekly emotion trend`, `monthly risk trend`처럼 그래프 친화적 응답을 별도 API로 분리하는 것이 읽기 쉽다.

---

# 12. 실호출 확인 메모

- 정상 요청
  - `GET /api/v1/reports/monthly-summary?year=2026&month=3`
- 확인 항목
  - `diaryCount`
  - `averageEmotionIntensity`
  - `topPrimaryEmotions`
  - `riskSummary.mediumCount`
  - `riskSummary.highCount`
- 오류 요청
  - `GET /api/v1/reports/monthly-summary?year=2026&month=13`
- 확인 결과
  - `400 Bad Request`
  - `month 값 범위가 올바르지 않습니다.`

---

# 13. Report 2차 확장 메모

## 13-1. 주간 감정 추이
`GET /api/v1/reports/emotions/weekly`

- 최근 7일 범위를 응답한다.
- 날짜별로
  - 기록 여부
  - diary 수
  - 대표 감정
  - 평균 감정 강도
  를 내려준다.

## 13-2. 월간 위험도 추이
`GET /api/v1/reports/risks/monthly?year=2026&month=3`

- 해당 월 전체 날짜를 응답한다.
- 날짜별로
  - `mediumCount`
  - `highCount`
  를 내려준다.

## 13-3. 테스트 코드 보강
- `ChatServiceTest`
  - `HIGH -> SAFETY`
  - `MEDIUM -> SUPPORTIVE`
  - `LOW -> NORMAL`
- `DiaryServiceTest`
  - diary 저장 후 위험도 필드 반영
- `ReportServiceTest`
  - 월간 요약 조립
  - 잘못된 month 검증
  - 주간 감정 추이 7일 응답
  - 월간 위험도 추이 집계

검증:
- `./gradlew.bat test` 통과

## 13-4. 실호출 확인 메모
- `GET /api/v1/reports/emotions/weekly`
  - Swagger / Postman 실호출 확인 완료
  - 최근 7일 범위 응답과 빈 날짜 구조 확인 완료
- `GET /api/v1/reports/risks/monthly?year=2026&month=3`
  - Swagger / Postman 실호출 확인 완료
  - 월 전체 날짜 수와 날짜별 `mediumCount`, `highCount` 구조 확인 완료
- `GET /api/v1/reports/risks/monthly?year=2026&month=13`
  - `400 Bad Request` 확인 완료
