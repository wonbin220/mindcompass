# Mind Compass Postman API 명세서

## 1. Goal

이 문서는 업로드한 API 명세서 이미지 스타일을 참고해서, 현재 `mindcompass` 프로젝트에서 Postman 컬렉션으로 바로 옮기기 쉬운 형태로 정리한 API 명세 초안이다.

정리 기준:
- 현재 `backend-api`에 실제 구현된 공개 API를 우선 작성한다.
- 이미지에 있었지만 아직 프로젝트에 없는 도메인은 `미구현 후보 API`로 분리한다.
- 반응형 웹 또는 앱은 `backend-api`만 호출한다.
- 내부 `ai-api`, `ai-api-fastapi`는 Postman 공개 컬렉션 기본 범위에서 제외한다.

---

## 2. Base URL / 인증

### Base URL

- local: `http://localhost:8080`
- prefix: `/api/v1`

### 기본 헤더

```http
Content-Type: application/json
Authorization: Bearer {{accessToken}}
```

### Postman Environment 추천 변수

- `baseUrl` = `http://localhost:8080`
- `accessToken` = 로그인 후 저장
- `refreshToken` = 로그인 후 저장
- `diaryId`
- `sessionId`

---

## 3. Postman 컬렉션 폴더 구조 추천

### 1) Authentication

- `POST {{baseUrl}}/api/v1/auth/signup`
- `POST {{baseUrl}}/api/v1/auth/login`
- `POST {{baseUrl}}/api/v1/auth/refresh`

### 2) Users

- `GET {{baseUrl}}/api/v1/users/me`

### 3) Diaries

- `POST {{baseUrl}}/api/v1/diaries`
- `GET {{baseUrl}}/api/v1/diaries/{{diaryId}}`
- `PATCH {{baseUrl}}/api/v1/diaries/{{diaryId}}`
- `DELETE {{baseUrl}}/api/v1/diaries/{{diaryId}}`
- `GET {{baseUrl}}/api/v1/diaries?date=2026-03-29`

### 4) Calendar

- `GET {{baseUrl}}/api/v1/calendar/monthly-emotions?year=2026&month=3`
- `GET {{baseUrl}}/api/v1/calendar/daily-summary?date=2026-03-29`

### 5) Chat

- `POST {{baseUrl}}/api/v1/chat/sessions`
- `GET {{baseUrl}}/api/v1/chat/sessions`
- `GET {{baseUrl}}/api/v1/chat/sessions/{{sessionId}}`
- `POST {{baseUrl}}/api/v1/chat/sessions/{{sessionId}}/messages`

### 6) Reports

- `GET {{baseUrl}}/api/v1/reports/monthly-summary?year=2026&month=3`
- `GET {{baseUrl}}/api/v1/reports/emotions/weekly`
- `GET {{baseUrl}}/api/v1/reports/risks/monthly?year=2026&month=3`

### 7) Future APIs

- Personas
- Contents
- Payments

---

## 4. 구현 완료 API 명세

## 4-1. Authentication

### POST `/api/v1/auth/signup`

- 목적: 회원가입
- 인증: 불필요
- 사용 화면: 회원가입 화면

Request
```json
{
  "email": "user@example.com",
  "password": "Abcd1234!",
  "nickname": "냥집사"
}
```

Response `201 Created`
```json
{
  "userId": 1,
  "email": "user@example.com",
  "nickname": "냥집사",
  "createdAt": "2026-03-29T10:00:00"
}
```

주요 예외
- `400 Bad Request`: 요청값 검증 실패
- `409 Conflict`: 이메일 중복

### POST `/api/v1/auth/login`

- 목적: 로그인 및 토큰 발급
- 인증: 불필요
- 사용 화면: 로그인 화면

Request
```json
{
  "email": "user@example.com",
  "password": "Abcd1234!"
}
```

Response `200 OK`
```json
{
  "accessToken": "jwt-access-token",
  "refreshToken": "jwt-refresh-token",
  "user": {
    "userId": 1,
    "nickname": "냥집사"
  }
}
```

주요 예외
- `400 Bad Request`: 요청값 검증 실패
- `401 Unauthorized`: 이메일 또는 비밀번호 불일치

### POST `/api/v1/auth/refresh`

- 목적: access token 재발급
- 인증: 불필요
- 사용 화면: 앱 내부 자동 갱신

Request
```json
{
  "refreshToken": "jwt-refresh-token"
}
```

Response `200 OK`
```json
{
  "accessToken": "new-access-token",
  "refreshToken": "new-refresh-token"
}
```

주요 예외
- `401 Unauthorized`: 유효하지 않은 refresh token

---

## 4-2. Users

### GET `/api/v1/users/me`

- 목적: 내 정보 조회
- 인증: 필요
- 사용 화면: 마이페이지, 설정 화면

Response `200 OK`
```json
{
  "userId": 1,
  "email": "user@example.com",
  "nickname": "냥집사",
  "status": "ACTIVE",
  "settings": {
    "appLockEnabled": false,
    "notificationEnabled": true,
    "dailyReminderTime": "22:00:00",
    "responseMode": "EMPATHETIC"
  }
}
```

주요 예외
- `401 Unauthorized`: 토큰 누락 또는 만료

---

## 4-3. Diaries

### POST `/api/v1/diaries`

- 목적: 일기 작성
- 인증: 필요
- 사용 화면: 일기 작성 화면

Request
```json
{
  "title": "퇴근 후 기록",
  "content": "오늘은 일이 많아서 지쳤지만 산책 후 조금 안정되었다.",
  "primaryEmotion": "TIRED",
  "emotionIntensity": 4,
  "writtenAt": "2026-03-29T21:30:00"
}
```

Response `201 Created`
```json
{
  "diaryId": 101,
  "title": "퇴근 후 기록",
  "content": "오늘은 일이 많아서 지쳤지만 산책 후 조금 안정되었다.",
  "primaryEmotion": "TIRED",
  "emotionIntensity": 4,
  "writtenAt": "2026-03-29T21:30:00",
  "aiPrimaryEmotion": "CALM",
  "aiEmotionIntensity": 1,
  "aiSummary": "보수적인 기본 감정 결과로 처리했습니다.",
  "aiConfidence": 0.10,
  "riskLevel": "LOW",
  "riskScore": 0.10,
  "riskSignals": null,
  "recommendedAction": "NORMAL_RESPONSE"
}
```

주요 예외
- `400 Bad Request`: 본문, 감정 강도 등 입력 오류
- `401 Unauthorized`: 토큰 누락 또는 만료

### GET `/api/v1/diaries/{diaryId}`

- 목적: 일기 상세 조회
- 인증: 필요
- 사용 화면: 일기 상세 화면

Response `200 OK`
```json
{
  "diaryId": 101,
  "title": "퇴근 후 기록",
  "content": "오늘은 일이 많아서 지쳤지만 산책 후 조금 안정되었다.",
  "primaryEmotion": "TIRED",
  "emotionIntensity": 4,
  "writtenAt": "2026-03-29T21:30:00",
  "emotionTags": [
    {
      "emotionCode": "TIRED",
      "intensity": 4,
      "sourceType": "USER"
    }
  ],
  "riskLevel": "LOW",
  "riskScore": 0.10,
  "recommendedAction": "NORMAL_RESPONSE"
}
```

주요 예외
- `404 Not Found`: 존재하지 않는 일기 또는 소유권 없음

### PATCH `/api/v1/diaries/{diaryId}`

- 목적: 일기 수정
- 인증: 필요
- 사용 화면: 일기 수정 화면

Request
```json
{
  "title": "수정된 기록",
  "content": "오늘은 힘들었지만 저녁에는 조금 진정됐다.",
  "primaryEmotion": "CALM",
  "emotionIntensity": 3
}
```

Response `200 OK`
```json
{
  "diaryId": 101,
  "title": "수정된 기록",
  "content": "오늘은 힘들었지만 저녁에는 조금 진정됐다.",
  "primaryEmotion": "CALM",
  "emotionIntensity": 3
}
```

주요 예외
- `404 Not Found`: 존재하지 않는 일기 또는 소유권 없음

### DELETE `/api/v1/diaries/{diaryId}`

- 목적: 일기 삭제
- 인증: 필요
- 사용 화면: 일기 상세 화면

Response `204 No Content`

주요 예외
- `404 Not Found`: 존재하지 않는 일기 또는 소유권 없음

### GET `/api/v1/diaries?date=2026-03-29`

- 목적: 특정 날짜 일기 목록 조회
- 인증: 필요
- 사용 화면: 캘린더 날짜 선택 후 목록

Response `200 OK`
```json
{
  "date": "2026-03-29",
  "items": [
    {
      "diaryId": 101,
      "title": "퇴근 후 기록",
      "primaryEmotion": "TIRED",
      "emotionIntensity": 4,
      "writtenAt": "2026-03-29T21:30:00"
    }
  ]
}
```

---

## 4-4. Calendar

### GET `/api/v1/calendar/monthly-emotions?year=2026&month=3`

- 목적: 월간 감정 캘린더 조회
- 인증: 필요
- 사용 화면: 캘린더 메인 화면

Response `200 OK`
```json
{
  "year": 2026,
  "month": 3,
  "days": [
    {
      "date": "2026-03-29",
      "hasDiary": true,
      "primaryEmotion": "CALM",
      "emotionIntensity": 3
    }
  ]
}
```

주요 예외
- `400 Bad Request`: year, month 범위 오류

### GET `/api/v1/calendar/daily-summary?date=2026-03-29`

- 목적: 특정 날짜 감정 요약 조회
- 인증: 필요
- 사용 화면: 캘린더 날짜 상세 카드

Response `200 OK`
```json
{
  "date": "2026-03-29",
  "hasDiary": true,
  "diaryCount": 1,
  "primaryEmotion": "CALM",
  "emotionIntensity": 3,
  "summary": "하루 전체 흐름은 피곤했지만 저녁에 안정감을 회복한 상태입니다."
}
```

---

## 4-5. Chat

### POST `/api/v1/chat/sessions`

- 목적: 채팅 세션 생성
- 인증: 필요
- 사용 화면: AI 상담 시작

Request
```json
{
  "title": "오늘 감정 상담",
  "sourceDiaryId": 101
}
```

Response `201 Created`
```json
{
  "sessionId": 501,
  "title": "오늘 감정 상담",
  "sourceDiaryId": 101,
  "createdAt": "2026-03-29T22:00:00",
  "updatedAt": "2026-03-29T22:00:00"
}
```

### GET `/api/v1/chat/sessions`

- 목적: 내 채팅 세션 목록 조회
- 인증: 필요
- 사용 화면: 채팅방 목록

Response `200 OK`
```json
{
  "items": [
    {
      "sessionId": 501,
      "title": "오늘 감정 상담",
      "sourceDiaryId": 101,
      "createdAt": "2026-03-29T22:00:00",
      "updatedAt": "2026-03-29T22:10:00"
    }
  ]
}
```

### GET `/api/v1/chat/sessions/{sessionId}`

- 목적: 채팅 세션 상세 조회
- 인증: 필요
- 사용 화면: 채팅방 상세 화면

Response `200 OK`
```json
{
  "sessionId": 501,
  "title": "오늘 감정 상담",
  "sourceDiaryId": 101,
  "messages": [
    {
      "messageId": 1001,
      "role": "USER",
      "content": "오늘 하루가 너무 불안했어요.",
      "createdAt": "2026-03-29T22:01:00"
    },
    {
      "messageId": 1002,
      "role": "ASSISTANT",
      "content": "오늘 많이 불안하셨겠어요.",
      "createdAt": "2026-03-29T22:01:02"
    }
  ]
}
```

### POST `/api/v1/chat/sessions/{sessionId}/messages`

- 목적: 메시지 전송 및 AI 응답 생성
- 인증: 필요
- 사용 화면: 채팅 화면

Request
```json
{
  "message": "오늘 하루가 너무 불안했고 잠이 안 올 것 같아요."
}
```

Response `201 Created`
```json
{
  "userMessageId": 1003,
  "assistantMessageId": 1004,
  "assistantReply": "오늘 많이 불안하셨겠어요. 지금 가장 크게 느껴지는 불안이 어떤 상황과 연결되는지 함께 정리해볼까요?",
  "responseType": "SUPPORTIVE"
}
```

주요 예외
- `404 Not Found`: 존재하지 않는 세션 또는 소유권 없음
- `503 Service Unavailable` 또는 `200/201 + FALLBACK`: 내부 AI 실패 시 fallback 응답 정책

---

## 4-6. Reports

### GET `/api/v1/reports/monthly-summary?year=2026&month=3`

- 목적: 월간 리포트 요약 조회
- 인증: 필요
- 사용 화면: 리포트 메인 화면

Response `200 OK`
```json
{
  "year": 2026,
  "month": 3,
  "diaryCount": 12,
  "averageEmotionIntensity": 3.2,
  "topPrimaryEmotions": [
    {
      "emotion": "CALM",
      "count": 5
    }
  ],
  "riskSummary": {
    "mediumCount": 1,
    "highCount": 0
  }
}
```

### GET `/api/v1/reports/emotions/weekly`

- 목적: 최근 7일 감정 추이 조회
- 인증: 필요
- 사용 화면: 주간 감정 그래프

Query
- optional: `date=2026-03-29`

Response `200 OK`
```json
{
  "startDate": "2026-03-23",
  "endDate": "2026-03-29",
  "items": [
    {
      "date": "2026-03-29",
      "primaryEmotion": "CALM",
      "emotionIntensity": 3
    }
  ]
}
```

### GET `/api/v1/reports/risks/monthly?year=2026&month=3`

- 목적: 월간 위험도 추이 조회
- 인증: 필요
- 사용 화면: 위험 신호 리포트

Response `200 OK`
```json
{
  "year": 2026,
  "month": 3,
  "items": [
    {
      "date": "2026-03-29",
      "riskLevel": "MEDIUM",
      "count": 1
    }
  ]
}
```

---

## 5. 이미지 기준 미구현 후보 API

업로드한 이미지에 있었지만, 현재 프로젝트에는 아직 없는 도메인이다.

## 5-1. Users 확장 후보

- `PATCH /api/v1/users/me/settings`
- `GET /api/v1/users/me/statistics`

## 5-2. Personas 후보

- `GET /api/v1/personas`
- `GET /api/v1/personas/{personaId}`
- `GET /api/v1/personas/recommendations`
- `GET /api/v1/personas/recommendations/today`

## 5-3. Contents 후보

- `GET /api/v1/contents/recommendations`
- `GET /api/v1/contents/recommendations/realtime`
- `POST /api/v1/contents/bookmarks`
- `GET /api/v1/contents/bookmarks`
- `DELETE /api/v1/contents/bookmarks/{bookmarkId}`

## 5-4. Payments 후보

- `POST /api/v1/payments/subscription-link`
- `GET /api/v1/payments/subscription-status`
- `GET /api/v1/payments/customer-portal`
- `POST /api/v1/payments/polar/webhook`

중요:
- 이 후보 API들은 현재 `backend-api`에 구현되어 있지 않다.
- Postman 컬렉션에는 `Future APIs` 폴더로만 넣고 실제 호출 테스트는 비활성화하는 것이 안전하다.

---

## 6. Postman 테스트 플로우 추천

### 1단계. 인증

1. `POST /auth/signup`
2. `POST /auth/login`
3. `accessToken`, `refreshToken` 환경 변수 저장

### 2단계. 사용자/일기

1. `GET /users/me`
2. `POST /diaries`
3. 응답의 `diaryId`를 환경 변수 저장
4. `GET /diaries/{diaryId}`
5. `GET /diaries?date=...`

### 3단계. 캘린더/리포트

1. `GET /calendar/monthly-emotions`
2. `GET /calendar/daily-summary`
3. `GET /reports/monthly-summary`
4. `GET /reports/emotions/weekly`
5. `GET /reports/risks/monthly`

### 4단계. 채팅

1. `POST /chat/sessions`
2. 응답의 `sessionId`를 환경 변수 저장
3. `GET /chat/sessions`
4. `GET /chat/sessions/{sessionId}`
5. `POST /chat/sessions/{sessionId}/messages`

---

## 7. 화면 기준 매핑

### 인증 화면

- 회원가입
- 로그인
- 토큰 재발급

### 마이/설정 화면

- 내 정보 조회
- 사용자 설정 수정(후보)

### 일기 화면

- 일기 작성
- 일기 상세 조회
- 일기 수정
- 일기 삭제
- 날짜별 일기 목록 조회

### 캘린더 화면

- 월간 감정 캘린더 조회
- 일별 감정 요약 조회

### 채팅 화면

- 채팅 세션 생성
- 채팅 세션 목록 조회
- 채팅 세션 상세 조회
- 메시지 전송

### 리포트 화면

- 월간 요약 조회
- 주간 감정 추이 조회
- 월간 위험도 추이 조회

---

## 8. Next Step

추천 다음 작업:

1. 이 문서를 기준으로 Postman Collection JSON 초안 만들기
2. 로그인 응답에서 Postman script로 `accessToken` 자동 저장 설정하기
3. `PATCH /api/v1/users/me/settings`를 다음 구현 후보로 확정하기
