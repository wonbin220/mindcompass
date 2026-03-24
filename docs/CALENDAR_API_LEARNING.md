# Calendar API 학습 문서

이 문서는 `Calendar` 도메인을 학습하기 위한 문서다.  
Calendar는 단순히 “날짜를 보여주는 기능”이 아니라, 사용자의 감정 기록을 **시간 축으로 시각화하는 핵심 조회 도메인**이다.

Diary가 “기록”의 중심이라면, Calendar는 “보기”의 중심이다.

---

# 1. Calendar 도메인이 왜 중요한가

감정일기 앱에서 사용자는 단순히 글을 저장하는 것만 원하지 않는다.  
보통 이런 질문을 하고 싶어 한다.

- 이번 달 나는 전반적으로 어땠지?
- 언제 불안이 심했지?
- 어느 날은 괜찮았고 어느 날은 힘들었지?
- 기록을 꾸준히 했는지 한눈에 볼 수 있을까?

이 질문에 답하는 것이 Calendar 도메인이다.

즉:
- Diary는 기록하는 기능
- Calendar는 기록을 시간 흐름으로 보여주는 기능

---

# 2. Calendar가 다루는 데이터

Calendar API는 직접 새로운 데이터를 많이 저장하기보다,  
주로 Diary 데이터를 **조회/가공**해서 화면에 맞게 내려준다.

예:
- 날짜별 대표 감정
- 날짜별 감정 강도
- 기록 여부
- 특정 날짜의 감정 요약
- 최근 7일/30일의 감정 추이

---

# 3. Calendar 패키지 예시 구조

```text
calendar/
├─ controller/
│  └─ CalendarController.java
├─ service/
│  └─ CalendarQueryService.java
└─ dto/
   └─ response/
      ├─ MonthlyCalendarResponse.java
      ├─ DailyEmotionSummaryResponse.java
      ├─ WeeklyEmotionTrendResponse.java
      └─ MonthlyEmotionTrendResponse.java

diary/
└─ repository/
   └─ DiaryQueryRepository.java
```

Calendar는 보통 자체 테이블보다 **Diary 조회 로직**을 재사용하는 경우가 많다.

---

# 4. Calendar 도메인을 이해할 때 중요한 포인트

## 4-1. Calendar는 조회 최적화 도메인이다
작성/수정보다 조회가 핵심이다.

## 4-2. 화면 기준 응답이 중요하다
DB 구조를 그대로 내리는 것이 아니라,
캘린더 UI가 바로 사용할 수 있는 형태로 가공해야 한다.

## 4-3. 범위 조회가 많다
- 한 달치 조회
- 하루 요약 조회
- 최근 7일 추이
- 최근 30일 추이

즉, 날짜 계산과 범위 쿼리가 중요하다.

---

# 5. API 1 - 월간 감정 캘린더 조회

## 5-1. 엔드포인트
`GET /api/v1/calendar/monthly?year=2026&month=3`

## 5-2. 왜 필요한가
앱 메인 화면이 캘린더 기반이라면 가장 중요한 조회 API다.  
사용자는 한 달의 감정 상태를 한눈에 보고 싶어 한다.

## 5-3. 어느 화면에서 쓰는가
- 메인 캘린더 화면
- 월간 감정 요약 화면

## 5-4. 응답 예시
```json
{
  "year": 2026,
  "month": 3,
  "days": [
    {
      "date": "2026-03-01",
      "hasDiary": true,
      "primaryEmotion": "CALM",
      "emotionIntensity": 2
    },
    {
      "date": "2026-03-02",
      "hasDiary": false,
      "primaryEmotion": null,
      "emotionIntensity": null
    }
  ]
}
```

## 5-5. 관련 파일
- `CalendarController.java`
- `CalendarQueryService.java`
- `DiaryQueryRepository.java`
- `MonthlyCalendarResponse.java`

## 5-6. 실행 순서
1. 앱이 year, month를 query parameter로 요청한다.
2. JWT 필터가 인증을 확인한다.
3. `CalendarController.getMonthlyCalendar()`가 요청을 받는다.
4. Controller가 현재 사용자 ID를 확인한다.
5. `CalendarQueryService.getMonthlyCalendar(userId, year, month)`를 호출한다.
6. Service가 해당 월의 시작일과 종료일을 계산한다.
7. `DiaryQueryRepository.findMonthlyDiarySummaries(userId, startDate, endDate)`를 호출한다.
8. Repository가 DB에서 해당 기간의 일기를 조회한다.
9. Service가 날짜별로 결과를 맵핑한다.
10. 기록이 없는 날짜도 캘린더 응답에는 포함할지 정책을 적용한다.
11. `MonthlyCalendarResponse`를 만든다.
12. Controller가 응답을 반환한다.

## 5-7. 학습 포인트
이 API의 핵심은 “DB 그대로 응답”이 아니라  
**캘린더 화면에 맞는 응답 조립**이다.

예를 들어 DB에는 8일치 기록만 있어도,
응답에는 31일 전체를 내려줄 수도 있다.

---

# 6. API 2 - 일별 감정 요약 조회

## 6-1. 엔드포인트
`GET /api/v1/calendar/daily?date=2026-03-18`

## 6-2. 왜 필요한가
사용자가 특정 날짜를 눌렀을 때,
그날의 전체 감정 요약을 보여주기 위해 필요하다.

## 6-3. 어느 화면에서 쓰는가
- 캘린더에서 날짜 클릭 후 요약 모달/상세 화면
- 하루 감정 리뷰 화면

## 6-4. 응답 예시
```json
{
  "date": "2026-03-18",
  "hasDiary": true,
  "primaryEmotion": "TIRED",
  "emotionIntensity": 4,
  "diaryCount": 1,
  "summary": "업무 스트레스가 있었지만 저녁 산책 후 안정감을 회복함"
}
```

## 6-5. 관련 파일
- `CalendarController.java`
- `CalendarQueryService.java`
- `DiaryQueryRepository.java`
- `DailyEmotionSummaryResponse.java`

## 6-6. 실행 순서
1. 앱이 날짜를 query parameter로 보낸다.
2. JWT 필터가 인증을 확인한다.
3. Controller가 사용자 ID와 날짜를 받는다.
4. `CalendarQueryService.getDailySummary(userId, date)`를 호출한다.
5. Repository가 해당 날짜의 diary 데이터를 조회한다.
6. Service가 대표 감정, 평균 강도, 요약 문장을 계산/조합한다.
7. 응답 DTO를 만든다.
8. Controller가 응답을 반환한다.

## 6-7. 학습 포인트
이 API는 Diary 상세 조회와 다르다.
- Diary 상세 조회 = 특정 기록 1개를 자세히 보기
- 일별 감정 요약 = 하루 전체를 압축해서 보기

즉, 같은 Diary 데이터라도 응답 목적이 다르면 별도 API가 필요할 수 있다.

---

# 7. API 3 - 주간 감정 추이 조회

## 7-1. 엔드포인트
`GET /api/v1/reports/emotions/weekly`

## 7-2. 왜 필요한가
최근 7일 동안 사용자의 감정 변화 흐름을 선 그래프나 막대 그래프로 보여주기 위해 필요하다.

## 7-3. 어느 화면에서 쓰는가
- 통계/리포트 화면
- 메인 홈 인사이트 카드

## 7-4. 응답 예시
```json
{
  "range": "LAST_7_DAYS",
  "items": [
    {"date": "2026-03-12", "primaryEmotion": "ANXIOUS", "emotionIntensity": 4},
    {"date": "2026-03-13", "primaryEmotion": "CALM", "emotionIntensity": 2}
  ]
}
```

## 7-5. 관련 파일
- `ReportController.java` 또는 `CalendarController.java`
- `CalendarQueryService.java`
- `DiaryQueryRepository.java`
- `WeeklyEmotionTrendResponse.java`

## 7-6. 실행 순서
1. 앱이 주간 감정 추이 요청을 보낸다.
2. JWT 필터가 인증을 확인한다.
3. Controller가 현재 사용자 ID를 받는다.
4. Service가 오늘 기준 최근 7일 범위를 계산한다.
5. Repository가 해당 기간의 diary 감정 데이터를 조회한다.
6. Service가 날짜 순으로 정렬하고 누락 날짜를 처리한다.
7. 응답 DTO를 만든다.
8. Controller가 응답을 반환한다.

## 7-7. 학습 포인트
추이 API에서는 “단건 정확성”보다  
**시간 축 정렬, 누락 날짜 처리, 그래프 친화적 구조**가 중요하다.

---

# 8. API 4 - 월간 감정 추이 조회

## 8-1. 엔드포인트
`GET /api/v1/reports/emotions/monthly`

## 8-2. 왜 필요한가
한 달 단위 감정 변화, 강도 평균, 긍정/부정 비율 등을 보여주기 위해 필요하다.

## 8-3. 어느 화면에서 쓰는가
- 월간 리포트 화면
- 감정 인사이트 화면

## 8-4. 관련 파일
- `ReportController.java`
- `CalendarQueryService.java` 또는 `ReportService.java`
- `DiaryQueryRepository.java`
- `MonthlyEmotionTrendResponse.java`

## 8-5. 실행 순서
1. 앱이 월간 추이 API를 호출한다.
2. JWT 필터가 인증을 확인한다.
3. Controller가 사용자 ID를 가져온다.
4. Service가 최근 30일 또는 현재 월 범위를 계산한다.
5. Repository가 해당 범위의 일기 데이터를 조회한다.
6. 감정 강도 평균, 대표 감정 빈도 등을 계산한다.
7. `MonthlyEmotionTrendResponse`를 만든다.
8. Controller가 응답을 반환한다.

## 8-6. 학습 포인트
여기서는 단순 조회를 넘어서  
**집계(aggregation)** 개념이 들어간다.

즉:
- count
- average
- group by emotion
같은 사고가 필요하다.

---

# 9. Calendar 도메인에서 Repository를 어떻게 생각해야 하는가

Calendar는 대부분 조회 도메인이라서  
일반 `DiaryRepository`만으로는 부족할 수 있다.

예:
- 특정 월 범위 조회
- 날짜별 대표 감정 집계
- 최근 7일 감정 흐름
- 통계용 집계 쿼리

이런 경우엔 `DiaryQueryRepository`를 따로 두는 것이 좋다.

즉:
- `DiaryRepository` = 일반 CRUD
- `DiaryQueryRepository` = 복잡한 조회/집계

---

# 10. Calendar 전체 흐름 한 번에 보기

## 월간 캘린더
앱 → JWT 필터 → Controller → QueryService → QueryRepository → 기간 조회 → 날짜별 응답 조립 → 앱

## 일별 요약
앱 → JWT 필터 → Controller → QueryService → QueryRepository → 하루 데이터 조회 → 요약 가공 → 앱

## 추이 조회
앱 → JWT 필터 → Controller → QueryService → QueryRepository → 범위 집계 → 그래프용 DTO 변환 → 앱

---

# 11. Calendar 도메인을 이해할 때 꼭 기억할 문장

- Calendar는 저장보다 조회가 핵심이다.
- Calendar 응답은 DB 모양이 아니라 화면 모양에 맞춰야 한다.
- 범위 조회와 날짜 계산이 중요하다.
- 복잡한 조회는 QueryRepository로 분리하면 이해와 유지보수가 쉬워진다.

---

# 12. Diary AI 결과 노출 원칙

Calendar 화면은 **빠르게 훑어보는 타임라인 화면**이기 때문에,
Diary 상세에 들어가는 AI 분석 결과를 그대로 다 노출하지 않는 편이 좋다.

현재 MVP 기준 원칙:

- 월간 캘린더
  - 유지: `hasDiary`, `primaryEmotion`, `emotionIntensity`
  - 보류: `aiSummary`, `aiConfidence`, `riskSignals`
- 일별 요약
  - 유지: 날짜 대표 감정, 최신 diary preview, emotion tags
  - 보류: 개별 diary의 `aiSummary` 전문 노출
  - 허용 가능: 추후 필요 시 `riskLevel` 정도만 보조 배지로 추가 검토

이렇게 정하는 이유:

- Calendar는 한눈에 보는 화면이라 정보 밀도가 너무 높아지면 오히려 해석이 어려워진다.
- `aiSummary`는 문장형 정보라서 상세 화면에 더 적합하다.
- mental health 서비스에서는 요약 문장을 반복 노출할수록 해석 부담이 커질 수 있다.

즉, Calendar는 **“언제 어떤 감정 흐름이 있었는지”**까지 보여주고,
**“AI가 그 기록을 어떻게 해석했는지”**는 Diary 상세 화면으로 넘기는 구성이 MVP에 맞다.

## 12-1. riskLevel 배지 검토 결과

`riskLevel`을 Calendar에 붙이는 것은 기술적으로 가능하다.
하지만 현재 MVP 기준으로는 **바로 추가하지 않는 것이 좋다.**

검토 결과:
- 월간 캘린더 셀
  - 비추천
  - 날짜 칸이 작아서 감정 + 기록 여부 + 위험도까지 같이 넣으면 정보가 과밀해진다.
- 일별 요약 카드
  - 추후 후보
  - 정말 필요하면 `HIGH` 또는 `MEDIUM`만 단순 배지로 보여주는 정도는 가능하다.
  - 다만 지금 단계에서는 Diary 상세 화면으로 들어가서 확인하는 흐름이 더 안전하다.

보류 이유:
- Calendar는 탐색 화면이라 사용자가 빠르게 훑어보는 경험이 우선이다.
- 위험도는 잘못 읽히면 불안을 키울 수 있어서, 문맥 없이 반복 노출하는 것은 조심해야 한다.
- Safety 관련 정보는 Diary 상세나 Chat/Safety 흐름처럼 맥락이 있는 화면에서 더 신중하게 보여주는 편이 좋다.

현재 결정:
- Calendar API와 화면에는 `riskLevel` 배지를 추가하지 않는다.
- 나중에 필요하면 **월간 캘린더 전체가 아니라 일별 요약 카드 한정**으로 다시 검토한다.
