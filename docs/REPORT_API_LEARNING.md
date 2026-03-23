# Report API 학습 문서

이 문서는 `Report / statistics` 도메인의 역할과 현재 API 구조를 빠르게 이해하기 위한 문서다.  
Report는 새 데이터를 저장하는 도메인이 아니라, 이미 저장된 Diary/AI 결과를 기간 기준으로 집계해서 보여주는 조회 도메인이다.

## 1. 왜 필요한가
- 사용자가 한 달 동안 몇 번 기록했는지 보고 싶다.
- 어떤 감정이 많이 나왔는지 보고 싶다.
- 감정 강도가 평균적으로 어느 정도였는지 보고 싶다.
- 위험 신호가 특정 기간에 얼마나 있었는지 보고 싶다.

즉 Report는 “기록하기”보다 “돌아보기”를 담당한다.

## 2. 현재 API

### 월간 요약
- `GET /api/v1/reports/monthly-summary?year=2026&month=3`

응답 예시:
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

### 선택 날짜 기준 최근 7일 감정 추이
- `GET /api/v1/reports/emotions/weekly`
- `GET /api/v1/reports/emotions/weekly?date=2026-03-24`

설명:
- `date` query가 없으면 오늘 기준 최근 7일을 내려준다.
- `date` query가 있으면 그 날짜를 포함한 최근 7일 범위를 내려준다.

### 월간 위험도 추이
- `GET /api/v1/reports/risks/monthly?year=2026&month=3`

## 3. 어느 화면에서 쓰는가
- 리포트 화면
- 주간 감정 추이 카드
- 월간 위험도 추이 카드

## 4. 관련 파일
- `backend-api/src/main/java/com/mindcompass/api/report/controller/ReportController.java`
- `backend-api/src/main/java/com/mindcompass/api/report/service/ReportService.java`
- `backend-api/src/main/java/com/mindcompass/api/report/repository/ReportQueryRepository.java`
- `backend-api/src/main/java/com/mindcompass/api/report/repository/ReportQueryRepositoryImpl.java`
- `backend-api/src/main/java/com/mindcompass/api/report/dto/response/*`

프론트:
- `web-app/src/lib/api/report.ts`
- `web-app/src/components/report/report-dashboard.tsx`

## 5. 실행 순서

### 월간 요약
1. 클라이언트가 `year`, `month` query를 보낸다.
2. JWT 필터가 인증을 확인한다.
3. `ReportController.getMonthlySummary()`가 요청을 받는다.
4. `ReportService.getMonthlySummary()`가 기간 검증과 범위 계산을 한다.
5. `ReportQueryRepository`가 diary 수, 평균 감정 강도, 대표 감정, 위험도 요약을 조회한다.
6. `MonthlyReportResponse`를 조립해서 반환한다.

### 주간 감정 추이
1. 클라이언트가 `date` query를 보내거나 생략한다.
2. JWT 필터가 인증을 확인한다.
3. `ReportController.getWeeklyEmotionTrend()`가 요청을 받는다.
4. `ReportService.getWeeklyEmotionTrend()`가 기준 날짜를 정한다.
5. `DiaryQueryRepository.findMonthlySummaries()`로 기준 날짜 포함 최근 7일 데이터를 조회한다.
6. 비어 있는 날짜까지 포함해서 `WeeklyEmotionTrendResponse`를 만든다.

### 월간 위험도 추이
1. 클라이언트가 `year`, `month` query를 보낸다.
2. JWT 필터가 인증을 확인한다.
3. `ReportController.getMonthlyRiskTrend()`가 요청을 받는다.
4. `ReportService.getMonthlyRiskTrend()`가 월 범위를 계산한다.
5. `ReportQueryRepository.findMonthlyRiskEntries()`로 위험도 기록을 조회한다.
6. 날짜별 `mediumCount`, `highCount`를 집계해서 반환한다.

## 6. 각 계층 역할
- `ReportController`
  - HTTP 요청을 받고 파라미터를 넘긴다.
- `ReportService`
  - 기간 계산, 파라미터 검증, DTO 조립을 한다.
- `ReportQueryRepository`
  - 실제 집계 쿼리를 담당한다.
- `DiaryQueryRepository`
  - 주간 감정 추이에 필요한 diary 요약 조회를 담당한다.
- `Response DTO`
  - 리포트 화면이 바로 쓸 수 있는 구조를 제공한다.

## 7. DB read/write 영향
- write 없음
- read만 수행
  - `diaries`
  - `diary_ai_analyses`

## 8. 예외 상황
- `year` 범위 오류 -> `400`
- `month` 범위 오류 -> `400`
- diary가 없는 기간 -> `200` + 빈 집계

## 9. AI fallback과의 관계
Report API는 ai-api를 직접 호출하지 않는다.  
다만 Diary 저장 시점의 AI 분석/위험도 값이 비어 있으면, 리포트 집계 결과도 일부 비어 보일 수 있다.

## 10. 최근 UI 연동 메모
- 웹 리포트 화면에서 기준 날짜 input으로 주간 감정 추이를 바꿀 수 있다.
- 월간 위험도 추이 카드는 내부 스크롤을 두어 긴 리스트가 화면을 과도하게 늘리지 않도록 했다.
