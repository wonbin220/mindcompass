# Mind Compass 구현 상태

이 문서는 새 채팅에서도 현재 구현 상태를 빠르게 이어가기 위한 기준 문서다.
문서 인코딩은 UTF-8 기준으로 유지한다.

## 현재 기준
- 기준 날짜: 2026-03-24
- 현재 단계 판단: MVP 주요 백엔드 기능 구현 완료, 웹/모바일 연동과 운영 안정화 단계 진입
- 전체 구조: `backend-api`는 Spring Boot 공개 API, `ai-api`는 FastAPI 내부 AI 서버, 클라이언트는 `backend-api`만 호출

## 현재 완료 범위

### 1. backend-api
- Auth
  - `POST /api/v1/auth/signup`
  - `POST /api/v1/auth/login`
  - `POST /api/v1/auth/refresh`
- User
  - `GET /api/v1/users/me`
- Diary
  - `POST /api/v1/diaries`
  - `GET /api/v1/diaries/{diaryId}`
  - `PATCH /api/v1/diaries/{diaryId}`
  - `DELETE /api/v1/diaries/{diaryId}`
  - `GET /api/v1/diaries?date=YYYY-MM-DD`
- Calendar
  - `GET /api/v1/calendar/monthly-emotions?year=YYYY&month=MM`
  - `GET /api/v1/calendar/daily-summary?date=YYYY-MM-DD`
- Chat
  - `POST /api/v1/chat/sessions`
  - `GET /api/v1/chat/sessions`
  - `GET /api/v1/chat/sessions/{sessionId}`
  - `POST /api/v1/chat/sessions/{sessionId}/messages`
- Report
  - `GET /api/v1/reports/monthly-summary?year=YYYY&month=MM`
  - `GET /api/v1/reports/emotions/weekly`
  - `GET /api/v1/reports/risks/monthly?year=YYYY&month=MM`

### 2. ai-api
- `POST /internal/ai/analyze-diary`
- `POST /internal/ai/generate-reply`
- `POST /internal/ai/risk-score`
- `GET /health`

### 3. AI 연계 동작
- Diary 저장 후 `analyze-diary`, `risk-score` 후처리 연결 완료
- Chat 메시지 전송 시 `risk-score` 선호출 후 `NORMAL`, `SUPPORTIVE`, `SAFETY`, `FALLBACK` 분기 처리 완료
- AI 실패 시에도 Diary/Chat 핵심 저장 흐름은 유지하도록 fallback 정책 적용 완료

### 4. 문서화 / 운영 보강
- API 학습 문서 정리
  - `docs/AUTH_API_LEARNING.md`
  - `docs/DIARY_API_LEARNING.md`
  - `docs/CALENDAR_API_LEARNING.md`
  - `docs/CHAT_API_LEARNING.md`
  - `docs/REPORT_API_LEARNING.md`
- 운영 가이드 정리
  - `docs/OPERATIONS_GUIDE.md`
  - `docs/MOBILE_INTEGRATION_HANDOFF.md`
  - `docs/SCREEN_TO_API_MAPPING.md`
  - `docs/DB_TABLE_SPECIFICATION.md`

### 5. web-app
- `Next.js + Tailwind CSS` 기반 웹 클라이언트 구조 존재
- `login`, `calendar`, `diary`, `chat`, `report` 페이지와 공통 레이아웃 구성 완료
- `backend-api` 호출용 API 클라이언트 파일 존재
- 인증 복구, 보호 라우트, 토큰 저장 흐름 코드 존재

## 현재 검증 상태

### backend-api
- 서비스 테스트
  - `AuthServiceTest`
  - `UserServiceTest`
  - `DiaryServiceTest`
  - `ChatServiceTest`
  - `ReportServiceTest`
  - `CalendarServiceTest`
- 컨트롤러 테스트
  - `AuthControllerTest`
  - `UserControllerTest`
  - `DiaryControllerTest`
  - `ChatControllerTest`
  - `ReportControllerTest`
  - `CalendarControllerTest`
- 보안 / 소유권 테스트
  - `SecurityIntegrationTest`
  - `OwnershipIntegrationTest`
- 엔드투엔드 테스트
  - `PublicApiDiaryReportE2ETest`
  - `PublicApiChatE2ETest`
  - `PublicApiDiaryFallbackE2ETest`
  - `PublicApiChatFallbackE2ETest`

### 현재까지 확인된 핵심 계약
- Diary create
  - 저장 우선
  - `analyze-diary` 실패 fallback
  - `risk-score` 실패 fallback
  - 두 AI 호출이 모두 실패해도 HTTP `201 Created` 유지
  - 컨트롤러 레벨에서도 AI 필드가 비어 있는 fallback 응답 계약 검증
- Chat send-message
  - 사용자 메시지 저장 우선
  - 고위험 `SAFETY`, 중위험 `SUPPORTIVE`, 일반 `NORMAL`, 장애 시 `FALLBACK`
  - 내부 AI 실패 시에도 HTTP `201 Created` 유지
- Calendar
  - 월간 감정 캘린더 응답 계약 검증
  - 일별 감정 요약 응답 계약 검증
  - 잘못된 연월 입력 검증
- Report
  - 월간 요약, 주간 감정 추이, 월간 위험도 추이의 컨트롤러 계약 검증
  - E2E 기준으로 월간 요약뿐 아니라 주간 감정 / 월간 위험도 조회까지 회귀 범위 확장

### Swagger / Postman
- Auth, User, Diary, Calendar, Chat, Report 주요 흐름 실호출 검증 기록 존재
- Diary AI 반영, Chat assistant 저장, Safety/Supportive 분기, Report 조회 응답까지 문서상 검증 완료

### web-app
- `node_modules` 설치 완료
- `.env.local` 생성 완료
- `npm run build` 재검증 통과
- `npm run dev`는 장시간 실행 프로세스 특성상 확인용 실행 중 타임아웃 종료됐지만, 의존성/빌드 기준으로는 로컬 부팅 가능한 상태로 판단

## 현재 리스크 / 주의 사항
- `backend-api`와 `ai-api`는 환경변수 의존도가 높다.
  - `JWT_SECRET`
  - `DB_PASSWORD`
  - `AI_API_BASE_URL`
  - `WEB_ALLOWED_ORIGINS`
- PowerShell 실행 환경과 IntelliJ Run Configuration 환경변수가 다를 수 있으므로 실행 경로별 설정 확인이 필요하다.
- `application.yaml` 기본 전략은 `ddl-auto: validate`라서 migration 누락 시 로컬 부팅이 바로 막힌다.
- 새 도메인 추가 시 엔티티만 만들지 말고 `db/migration`과 실제 DB 테이블 반영 여부를 같이 확인해야 한다.
- 문서상 웹 빌드 성공 기록이 있으나, 현재 로컬 재검증 결과는 의존성 미설치 상태라 그대로 신뢰하면 안 된다.
- Diary / Chat AI 후처리는 현재 요청-응답 안에서 `try/catch` fallback으로 보호되고 있으며, 아직 비동기 분리나 재시도 큐까지는 도입하지 않았다.

## 최근 hardening 메모

### 2026-03-24 diary create API hardening
- Completed API: `POST /api/v1/diaries`
- Verification status:
  - `DiaryServiceTest`가 성공, `analyze-diary` 실패 fallback, `risk-score` 실패 fallback을 검증
  - `PublicApiFlowE2ETest`가 두 AI 호출이 모두 실패해도 `201 Created`가 유지되는 것을 검증
  - `DiaryControllerTest`가 AI 필드가 없는 fallback 응답 직렬화를 검증
- Confirmed behavior:
  - diary 저장은 AI 분석 실패 때문에 롤백되지 않는다
  - 응답은 저장된 diary 데이터 중심으로 반환되고, 성공한 AI 필드만 선택적으로 포함된다

### 2026-03-24 cross-domain hardening
- Completed API:
  - `POST /api/v1/chat/sessions/{sessionId}/messages`
  - `GET /api/v1/calendar/monthly-emotions`
  - `GET /api/v1/calendar/daily-summary`
- Verification status:
  - `ChatServiceTest`가 AI orchestration 실패 시 `FALLBACK` 응답을 검증
  - `PublicApiFlowE2ETest`가 chat send-message에서 AI 실패 시에도 `201 Created`와 `FALLBACK` 응답을 검증
  - `CalendarServiceTest`, `CalendarControllerTest`가 월간/일별 조회 계약을 검증
- Confirmed behavior:
  - chat은 사용자 메시지를 저장한 뒤 AI 실패가 나도 보조 응답으로 흐름을 유지한다
  - calendar는 화면 친화적인 집계 응답을 안정적으로 반환한다

### 2026-03-24 controller and report regression expansion
- Completed API:
  - `POST /api/v1/chat/sessions/{sessionId}/messages`
  - `GET /api/v1/reports/monthly-summary`
  - `GET /api/v1/reports/emotions/weekly`
  - `GET /api/v1/reports/risks/monthly`
- Verification status:
  - `ChatControllerTest`에 `FALLBACK` 응답 계약 케이스 추가
  - `PublicApiFlowE2ETest`에서 monthly summary뿐 아니라 weekly emotions / monthly risks 조회까지 실제 사용자 흐름에 포함
- Confirmed behavior:
  - chat fallback 응답도 컨트롤러 레벨에서 안정적으로 직렬화된다
  - report 조회는 핵심 3개 화면 API가 컨트롤러 / E2E 양쪽에서 계속 회귀 검증된다

### 2026-03-24 E2E scenario split
- Completed change:
  - 기존 `PublicApiFlowE2ETest`를 기능별 시나리오로 분리
  - `PublicApiDiaryReportE2ETest`
  - `PublicApiChatE2ETest`
  - `PublicApiDiaryFallbackE2ETest`
  - `PublicApiChatFallbackE2ETest`
- Verification status:
  - 분리된 E2E 시나리오별 Gradle 테스트 재실행 통과
- Confirmed behavior:
  - 실패 시 어느 흐름이 깨졌는지 `Diary/Report`, `Chat`, `Diary fallback`, `Chat fallback` 단위로 바로 식별 가능

## 지금 바로 다음에 할 일
1. 필요 시 `Report` E2E도 월간 요약 / 주간 감정 / 월간 위험도 단위로 더 잘게 분리
2. `Auth`와 `User`도 E2E 진입 시나리오를 별도 파일로 만들지 검토
3. `web-app` 실행 환경 복구
   - `npm install` 상태 복구
   - `.env.local` 또는 실행 환경 변수에 `NEXT_PUBLIC_BACKEND_API_BASE_URL` 확인
   - `npm run build`, `npm run dev` 재검증
4. `ai-api` 운영 점검 절차 고정
   - `/health` 확인
   - `analyze-diary`, `generate-reply`, `risk-score` 장애 징후 체크리스트 재검증

## 다음 추천 우선순위
1. Report E2E 세분화와 Auth/User E2E 진입 시나리오 검토
2. 웹 빌드/실행 상태 복구와 환경변수 템플릿 정리
3. 운영 체크리스트를 실제 실행 절차로 검증
4. Safety 규칙 보정과 샘플 데이터 확장
5. 이후 필요 시 Report 고도화 또는 모바일 실제 연동 진행

## 최근 확인 메모
- `ChatControllerTest`, `ReportControllerTest` 통과
- `AuthControllerTest`, `UserControllerTest` 통과
- `DiaryServiceTest` 통과
- `PublicApiDiaryReportE2ETest`, `PublicApiChatE2ETest`, `PublicApiDiaryFallbackE2ETest`, `PublicApiChatFallbackE2ETest` 통과
- `ChatServiceTest`, `CalendarServiceTest`, `CalendarControllerTest` 통과
- `backend-api` 핵심 fallback / 조회 계약은 점차 서비스 테스트에서 컨트롤러 / E2E 테스트까지 확장 중
- `web-app`은 2026-03-24 기준 `npm install`, `.env.local` 생성, `npm run build` 통과까지 복구 완료
