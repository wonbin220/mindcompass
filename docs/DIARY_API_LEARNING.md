# Diary API 학습 문서

이 문서는 `Diary` 도메인을 학습하기 위한 문서다.  
이 프로젝트의 핵심은 AI 상담도 중요하지만, 그보다 먼저 **감정 기록이 안정적으로 저장되는 것**이다.  
그래서 Diary는 MVP에서 가장 중요한 도메인 중 하나다.

---

# 1. Diary 도메인이 왜 핵심인가

이 앱의 가장 중요한 질문은 이것이다.

**“사용자가 오늘의 감정을 기록할 수 있는가?”**

상담 AI는 잠시 실패할 수 있다.  
하지만 일기 기록까지 실패하면 서비스의 중심 가치가 무너진다.

그래서 Diary 도메인의 핵심 원칙은 다음과 같다.

- 감정일기 기록은 반드시 가능해야 한다.
- AI 분석은 나중에 붙을 수 있어도 저장은 먼저 안정적이어야 한다.
- 하루 단위 조회와 캘린더 연동이 쉬워야 한다.
- 감정 태그, 강도, 시간대 같은 확장 포인트가 있어야 한다.

---

# 2. Diary 도메인에서 다루는 것

Diary 도메인은 아래 데이터를 다룬다.

- 일기 본문
- 대표 감정
- 감정 강도
- 작성 일시
- 수정 일시
- 선택 감정 목록(확장 가능)
- 시간대별 감정(확장 가능)
- AI 분석 결과 연결(후속 확장)

---

# 3. Diary 패키지 예시 구조

```text
diary/
├─ controller/
│  └─ DiaryController.java
├─ service/
│  └─ DiaryService.java
├─ repository/
│  ├─ DiaryRepository.java
│  └─ DiaryQueryRepository.java
├─ domain/
│  ├─ Diary.java
│  └─ DiaryEmotion.java
└─ dto/
   ├─ request/
   │  ├─ CreateDiaryRequest.java
   │  └─ UpdateDiaryRequest.java
   └─ response/
      ├─ DiaryDetailResponse.java
      ├─ DiarySummaryResponse.java
      └─ DiaryListResponse.java
```

---

# 4. Diary를 이해할 때 먼저 알아야 할 구조

## 4-1. Diary Entity
일기의 본체다.

예시 필드:
- id
- userId
- content
- primaryEmotion
- emotionIntensity
- writtenAt
- createdAt
- updatedAt

## 4-2. DiaryEmotion
확장 감정 태그를 따로 분리할 수도 있다.

예:
- anxious
- lonely
- relieved
- calm

초기 MVP에서는 `Diary`에 대표 감정만 두고,  
나중에 여러 감정 태그가 필요해지면 `DiaryEmotion` 테이블을 분리할 수 있다.

## 4-3. DiaryService
일기 저장, 수정, 삭제, 조회의 흐름을 담당한다.

## 4-4. DiaryRepository
DB 저장/조회 담당이다.

---

# 5. API 1 - 일기 작성

## 5-1. 엔드포인트
`POST /api/v1/diaries`

## 5-2. 왜 필요한가
사용자가 하루의 감정을 기록하고 남기기 위해 필요하다.  
이 서비스의 본질은 “기록 가능한 감정일기”이므로, 가장 중요한 핵심 API다.

## 5-3. 어느 화면에서 쓰는가
- 일기 작성 화면
- 메인 화면에서 오늘 기록 시작 버튼
- 추후 음성 입력을 텍스트로 변환한 뒤 저장하는 흐름

## 5-4. 요청 예시
```json
{
  "content": "오늘은 일이 많아서 좀 지쳤지만 저녁 산책 후 조금 안정되었다.",
  "primaryEmotion": "TIRED",
  "emotionIntensity": 4,
  "writtenAt": "2026-03-18T21:30:00"
}
```

## 5-5. 응답 예시
```json
{
  "diaryId": 101,
  "content": "오늘은 일이 많아서 좀 지쳤지만 저녁 산책 후 조금 안정되었다.",
  "primaryEmotion": "TIRED",
  "emotionIntensity": 4,
  "writtenAt": "2026-03-18T21:30:00"
}
```

## 5-6. 관련 파일
- `DiaryController.java`
- `DiaryService.java`
- `DiaryRepository.java`
- `Diary.java`
- `CreateDiaryRequest.java`
- `DiaryDetailResponse.java`

## 5-7. 실행 순서
1. 앱이 access token과 일기 내용을 담아 요청한다.
2. JWT 필터가 사용자 인증을 확인한다.
3. `DiaryController.createDiary()`가 요청을 받는다.
4. body가 `CreateDiaryRequest` DTO로 변환된다.
5. Controller가 현재 로그인한 사용자 ID를 확인한다.
6. `DiaryService.createDiary(userId, request)`를 호출한다.
7. Service가 입력값을 검증한다.
8. `Diary` 엔티티를 생성한다.
9. `DiaryRepository.save()`가 호출된다.
10. DB에 diary 데이터가 저장된다.
11. 저장된 결과를 `DiaryDetailResponse`로 변환한다.
12. Controller가 응답을 반환한다.

## 5-8. DB 영향
- `diary` 테이블 insert
- 감정 태그를 분리했으면 `diary_emotion` insert 가능

## 5-9. 예외 상황
- 로그인하지 않은 사용자
- 본문 누락
- 감정 강도 범위 오류
- 미래 시각 저장 제한 정책 위반

## 5-10. 학습 포인트
이 API의 핵심은:
- **서비스의 본체 기능**
- **저장 안정성이 AI보다 우선**
- **요청 받기 / 로직 처리 / DB 저장의 분리**

---

# 6. API 2 - 일기 상세 조회

## 6-1. 엔드포인트
`GET /api/v1/diaries/{diaryId}`

## 6-2. 왜 필요한가
캘린더나 목록에서 특정 일기를 눌렀을 때, 그 상세 내용을 보여주기 위해 필요하다.

## 6-3. 어느 화면에서 쓰는가
- 일기 상세 화면
- 캘린더에서 하루를 눌렀을 때

## 6-4. 관련 파일
- `DiaryController.java`
- `DiaryService.java`
- `DiaryRepository.java`
- `DiaryDetailResponse.java`

## 6-5. 실행 순서
1. 앱이 `diaryId`를 path variable로 요청한다.
2. JWT 필터가 인증을 확인한다.
3. `DiaryController.getDiary(diaryId)`가 요청을 받는다.
4. Controller가 현재 사용자 ID를 확인한다.
5. `DiaryService.getDiary(userId, diaryId)`를 호출한다.
6. `DiaryRepository.findByIdAndUserId()`로 해당 일기를 조회한다.
7. Service가 존재 여부를 확인한다.
8. 결과를 `DiaryDetailResponse`로 변환한다.
9. Controller가 응답을 반환한다.

## 6-6. 학습 포인트
조회 API에서 중요한 것은 **내 데이터만 조회되도록 하는 것**이다.  
그래서 `findById()`보다 `findByIdAndUserId()` 같은 조건이 중요할 수 있다.

---

# 7. API 3 - 일기 수정

## 7-1. 엔드포인트
`PATCH /api/v1/diaries/{diaryId}`

## 7-2. 왜 필요한가
사용자가 기록한 감정이나 내용을 나중에 수정하고 싶을 수 있다.  
예를 들어 너무 짧게 썼다가 자세히 보완할 수도 있다.

## 7-3. 어느 화면에서 쓰는가
- 일기 상세 화면의 수정 버튼

## 7-4. 요청 예시
```json
{
  "content": "오늘은 일이 많아서 지쳤지만, 산책 후 훨씬 안정되었다.",
  "primaryEmotion": "CALM",
  "emotionIntensity": 3
}
```

## 7-5. 관련 파일
- `DiaryController.java`
- `DiaryService.java`
- `DiaryRepository.java`
- `UpdateDiaryRequest.java`
- `DiaryDetailResponse.java`

## 7-6. 실행 순서
1. 앱이 수정할 diaryId와 변경 내용을 담아 요청한다.
2. JWT 필터가 인증을 확인한다.
3. `DiaryController.updateDiary()`가 요청을 받는다.
4. `UpdateDiaryRequest` DTO로 body가 변환된다.
5. `DiaryService.updateDiary(userId, diaryId, request)`가 호출된다.
6. `DiaryRepository.findByIdAndUserId()`로 기존 일기를 조회한다.
7. Service가 변경 가능한 필드를 수정한다.
8. JPA dirty checking 또는 `save()`로 업데이트를 반영한다.
9. 결과를 응답 DTO로 만든다.
10. Controller가 응답을 반환한다.

## 7-7. 학습 포인트
수정 API는 “새로 저장”이 아니라  
**기존 엔티티를 찾아서 일부 값을 바꾸는 흐름**이다.

---

# 8. API 4 - 일기 삭제

## 8-1. 엔드포인트
`DELETE /api/v1/diaries/{diaryId}`

## 8-2. 왜 필요한가
사용자가 실수로 쓴 일기나 더 이상 남기고 싶지 않은 기록을 삭제할 수 있어야 한다.

## 8-3. 어느 화면에서 쓰는가
- 일기 상세 화면
- 편집 메뉴

## 8-4. 관련 파일
- `DiaryController.java`
- `DiaryService.java`
- `DiaryRepository.java`

## 8-5. 실행 순서
1. 앱이 diaryId를 담아 삭제 요청을 보낸다.
2. JWT 필터가 인증을 확인한다.
3. Controller가 현재 사용자 ID를 확인한다.
4. `DiaryService.deleteDiary(userId, diaryId)`를 호출한다.
5. `DiaryRepository.findByIdAndUserId()`로 대상 일기를 찾는다.
6. 존재하면 삭제를 수행한다.
7. 물리 삭제 또는 soft delete 정책을 적용한다.
8. Controller가 성공 응답을 반환한다.

## 8-6. 학습 포인트
삭제는 단순해 보여도 정책이 중요하다.
- 실제로 DB에서 지울 것인가?
- 숨김 처리할 것인가?
- 통계에 반영된 데이터는 어떻게 할 것인가?

초기 MVP에서는 단순 삭제로 시작해도 되지만, 확장 시 정책을 분명히 해야 한다.

---

# 9. API 5 - 날짜별 일기 조회

## 9-1. 엔드포인트
`GET /api/v1/diaries?date=2026-03-18`

## 9-2. 왜 필요한가
하루에 일기를 하나만 쓰는 구조일 수도 있고, 여러 개를 쓰는 구조일 수도 있다.  
어쨌든 특정 날짜의 기록을 조회하는 API는 캘린더와 매우 밀접하다.

## 9-3. 어느 화면에서 쓰는가
- 특정 날짜 일기 목록
- 캘린더 날짜 클릭 후 상세 보기

## 9-4. 관련 파일
- `DiaryController.java`
- `DiaryService.java`
- `DiaryQueryRepository.java`
- `DiarySummaryResponse.java`
- `DiaryListResponse.java`

## 9-5. 실행 순서
1. 앱이 query parameter로 date를 보낸다.
2. JWT 필터가 인증을 확인한다.
3. Controller가 `date`를 받는다.
4. `DiaryService.getDiariesByDate(userId, date)`를 호출한다.
5. `DiaryQueryRepository.findAllByUserIdAndDate()`가 DB를 조회한다.
6. Service가 결과를 요약 형태로 가공한다.
7. `DiaryListResponse`를 만든다.
8. Controller가 응답을 반환한다.

## 9-6. 학습 포인트
단건 조회와 목록 조회는 응답 DTO가 다를 수 있다.  
상세 조회에서는 본문 전체가 필요하지만,
목록 조회에서는 대표 감정과 요약만 필요할 수 있다.

---

# 10. Diary 도메인 전체 실행 흐름

## 작성 API
앱 → JWT 필터 → Controller → Service → Repository.save → DB → Response

## 수정 API
앱 → JWT 필터 → Controller → Service → Repository.find → 엔티티 수정 → Response

## 조회 API
앱 → JWT 필터 → Controller → Service → QueryRepository.find → Response DTO 가공

이 차이를 이해하면 CRUD가 각각 어떻게 다른지 보인다.

---

# 11. Diary 도메인을 이해할 때 꼭 기억할 문장

- 이 서비스의 핵심은 AI보다 먼저 감정 기록이다.
- Diary API는 저장 안정성이 최우선이다.
- 조회 API는 화면이 바로 쓰기 좋은 형태로 응답을 가공해야 한다.
- 수정/삭제는 단순 구현보다 정책이 중요하다.

---

# 12. AI 실패 시 저장 정책

Diary API에서 AI 분석은 부가 기능이다.  
그래서 운영 기준은 아래처럼 잡는 것이 안전하다.

- diary 본문 저장이 먼저 성공해야 한다.
- ai-api `analyze-diary`가 실패해도 diary 저장 자체는 실패로 돌리지 않는다.
- AI 분석 결과가 없으면 사용자 입력 감정 태그만 유지한다.
- 서버 로그에는 분석 실패 경고를 남겨 추후 장애 추적이 가능해야 한다.

현재 구현 기준:
- Spring Boot `DiaryService`는 diary 저장 후 ai-api를 호출한다.
- ai-api 실패 시 `Diary AI analysis failed` 경고 로그를 남긴다.
- diary 생성/수정 응답은 그대로 반환하고, AI 분석 결과만 생략될 수 있다.

---

# 13. Diary risk-score 연계

Diary는 저장 자체를 절대 막지 않으면서, 저장 후 위험 신호를 후처리로 붙인다.

## 13-1. 왜 필요한가
- Chat뿐 아니라 Diary 본문에도 위험 신호가 들어올 수 있다.
- Diary는 기록 보존이 우선이라서, 고위험이어도 저장은 유지해야 한다.
- 이후 Report, Safety Event, 상담 유도 배너의 입력 데이터로 재사용할 수 있다.

## 13-2. 관련 파일
- `backend-api/src/main/java/com/mindcompass/api/diary/service/DiaryService.java`
- `backend-api/src/main/java/com/mindcompass/api/diary/domain/DiaryAiAnalysis.java`
- `backend-api/src/main/java/com/mindcompass/api/chat/client/AiSafetyClient.java`
- `backend-api/src/main/resources/db/migration/V6__diary_risk_fields.sql`
- `backend-api/src/main/java/com/mindcompass/api/diary/dto/response/DiaryDetailResponse.java`

## 13-3. 실행 순서
1. 앱이 diary 생성 또는 수정 요청을 보낸다.
2. JWT 필터가 인증을 확인한다.
3. `DiaryController`가 요청을 받고 `DiaryService`를 호출한다.
4. `DiaryService`가 diary와 사용자 감정 태그를 먼저 저장한다.
5. `analyze-diary`를 호출해 감정 분석 결과를 반영한다.
6. `risk-score`를 추가로 호출해 위험도 결과를 반영한다.
7. 두 AI 호출은 서로 독립적으로 실패할 수 있고, 실패해도 diary 저장은 유지한다.
8. 응답 DTO에는 `riskLevel`, `riskScore`, `riskSignals`, `recommendedAction`이 포함된다.

## 13-4. 예외 / fallback
- `analyze-diary` 실패:
  - diary 저장 유지
  - AI 감정 분석만 생략 가능
  - `Diary AI analysis failed` 경고 로그
- `risk-score` 실패:
  - diary 저장 유지
  - 위험도 필드만 비어 있을 수 있음
  - `Diary AI risk scoring failed` 경고 로그

## 13-5. 실호출 확인 메모
- 중위험 diary 본문
  - `"아무도 없고 너무 힘들어서 버티기 힘들어요."`
- 상세 조회 확인값
  - `riskLevel=MEDIUM`
  - `recommendedAction=SUPPORTIVE_RESPONSE`
- 고위험 diary 본문
  - `"다 끝내고 싶고 사라지고 싶어요."`
- 상세 조회 확인값
  - `riskLevel=HIGH`
  - `recommendedAction=SAFETY_RESPONSE`
- 즉 현재 Diary도 Chat과 같은 risk-score를 재사용해 `LOW/MEDIUM/HIGH` 위험도를 후처리로 반영하는 상태다.
