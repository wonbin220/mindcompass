# 웹 연동 핸드오프

이 파일은 기존 파일명을 유지한 문서이며, 현재 기준으로는 반응형 웹 연동 핸드오프 문서로 사용한다.

이 문서는 현재 완료된 `backend-api` 기준으로 반응형 웹 연동을 시작할 때 필요한 최소 체크리스트와 엔드포인트 계약을 정리한 문서다.

중요 원칙:
- 반응형 웹은 `backend-api`만 호출한다.
- 반응형 웹은 `ai-api`나 `ai-api-fastapi`를 직접 호출하지 않는다.
- 내부 AI 흐름은 `backend-api -> ai-api -> ai-api-fastapi` 순서로 유지한다.

---

# 1. 연동 시작 전 체크리스트

1. `backend-api` 실행
2. `ai-api` 실행
3. Swagger 최종 확인
4. Postman 핵심 흐름 확인
5. `AI_API_BASE_URL` / `JWT_SECRET` / `DB_PASSWORD` 환경변수 확인

---

# 2. 우선 연동 화면 순서

1. Auth
- 회원가입
- 로그인
- 내 정보

2. Diary
- 일기 생성
- 일기 상세
- 날짜별 일기 목록

3. Calendar
- 월간 감정 캘린더
- 일별 감정 요약

4. Chat
- 세션 생성
- 세션 상세
- 메시지 전송

5. Report
- 월간 요약
- 주간 감정 추이
- 월간 위험도 추이

---

# 3. 핵심 API 계약

## 3-1. 로그인
`POST /api/v1/auth/login`

웹 클라이언트 저장:
- `accessToken`
- `refreshToken`

## 3-2. Diary 생성
`POST /api/v1/diaries`

핵심 응답:
- `diaryId`
- `emotionTags`
- `riskLevel`
- `riskScore`
- `recommendedAction`

## 3-3. Calendar daily-summary
`GET /api/v1/calendar/daily-summary?date=YYYY-MM-DD`

빈 날짜도 `200` 응답:
- `hasDiary=false`
- `diaryCount=0`

## 3-4. Chat message
`POST /api/v1/chat/sessions/{sessionId}/messages`

핵심 응답:
- `assistantReply`
- `responseType`

`responseType` 해석:
- `NORMAL`
- `SUPPORTIVE`
- `SAFETY`
- `FALLBACK`

## 3-5. Report
- `GET /api/v1/reports/monthly-summary?year=YYYY&month=MM`
- `GET /api/v1/reports/emotions/weekly`
- `GET /api/v1/reports/risks/monthly?year=YYYY&month=MM`

---

# 4. 웹 연동 시 바로 주의할 점

- path/query 변수명 오타에 주의
  - `sessionId`
  - `diaryId`
- Diary enum 값은 서버 기준 enum 사용
  - 예: `ANXIOUS`
- Chat 경로는 복수형
  - `/api/v1/chat/sessions`
- 잘못된 `year/month`는 `400`
- AI 실패가 나도 저장 기능은 대부분 유지된다
- 브라우저 환경에서는 `WEB_ALLOWED_ORIGINS`와 CORS 설정을 같이 확인한다

---

# 5. 현재 연동 가능 상태

- Auth/User 완료
- Diary/Calendar 완료
- Chat/Safety Net 완료
- Report 1차/2차 완료
- Swagger/Postman 실호출 검증 완료

즉 반응형 웹은 지금부터 `backend-api` 기준으로 연동을 시작할 수 있다.
