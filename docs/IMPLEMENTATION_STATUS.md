# Mind Compass 구현 상태

이 문서는 새 채팅에서도 현재 구현 상태를 빠르게 이어가기 위한 진행 상황 기록 문서다.

## 현재 기준
- 기준 날짜: 2026-03-20
- 현재 우선순위: `Calendar -> ai-api analyze-diary 연결 보강 -> Chat`
- 구조: `backend-api`는 Spring Boot 공개 API, `ai-api`는 FastAPI 내부 AI 서버

## 완료된 작업

### 1. Spring Boot 기초 설정
- JWT 기반 인증 구조 구성 완료
- Swagger UI 구성 완료
- Flyway migration 파일 복구 및 정리 완료
- DB/JPA 타입 불일치 이슈 1차 정리 완료

### 2. Auth API
- `POST /api/v1/auth/signup`
- `POST /api/v1/auth/login`
- `POST /api/v1/auth/refresh`

완료 기준:
- Postman 테스트 통과
- Swagger 테스트 가능

### 3. User API
- `GET /api/v1/users/me`

완료 기준:
- Postman 테스트 통과
- Swagger 테스트 가능

### 4. Diary API
- `POST /api/v1/diaries`
- `GET /api/v1/diaries/{diaryId}`
- `PATCH /api/v1/diaries/{diaryId}`
- `DELETE /api/v1/diaries/{diaryId}`
- `GET /api/v1/diaries?date=YYYY-MM-DD`

완료 기준:
- Postman 테스트 통과
- Swagger 테스트 가능

### 5. Diary 부가 구조
- `diary_emotions` 테이블 및 엔티티 골격 추가
- `diary_ai_analyses` 테이블 및 엔티티 골격 추가
- FastAPI `POST /internal/ai/analyze-diary` 골격 추가

### 6. Calendar API 1차 구현
- `GET /api/v1/calendar/monthly-emotions?year=2026&month=3`
- `calendar` 패키지 및 월간 응답 DTO 추가
- `DiaryQueryRepository` 월간 범위 조회 메서드 추가

완료 기준:
- `./gradlew compileJava` 통과
- Swagger 테스트 통과
- Postman 테스트 통과

### 7. Calendar API 2차 구현
- `GET /api/v1/calendar/daily-summary?date=2026-03-20`
- 날짜 클릭 후 하루 감정 요약 응답 DTO 추가
- 최신 일기 요약과 하루 감정 태그를 함께 반환

현재 상태:
- 코드 구현 완료
- `./gradlew compileJava` 기준 확인 완료
- Swagger / Postman 실호출 확인 완료
- diary가 없는 날짜에 `hasDiary=false`, `emotionTags=[]`, `latestDiary=null` 응답 확인 완료

### 8. Diary AI 연결 보강
- `Diary create/update -> analyze-diary -> diary_ai_analyses`
- AI 감정 태그를 `diary_emotions(source=AI_ANALYSIS)`로 별도 저장
- raw payload를 JSON 문자열로 저장
- AI 실패 시 저장은 유지하고 서버 로그에 남김

현재 상태:
- 코드 구현 완료
- `./gradlew compileJava` 기준 확인 완료
- Swagger / Postman 실호출 확인 완료
- diary 생성 후 AI 분석 태그가 `emotionTags(sourceType=AI_ANALYSIS)`로 상세 응답에 반영되는 것 확인 완료

### 9. Chat API 1차 구현
- `POST /api/v1/chat/sessions`
- `GET /api/v1/chat/sessions`
- `GET /api/v1/chat/sessions/{sessionId}`
- `POST /api/v1/chat/sessions/{sessionId}/messages`
- ai-api `POST /internal/ai/generate-reply` 골격 추가

현재 상태:
- 코드 구현 완료
- `./gradlew compileJava` 통과
- 로컬 부팅 이슈 정리 완료
- Swagger / Postman 실호출 확인 완료
- 사용자 메시지 저장 후 assistant 메시지 저장까지 확인 완료
- ai-api `generate-reply` 연동 응답이 세션 상세 조회에 반영되는 것 확인 완료

### 10. Safety Net MVP 1차 구현
- ai-api `POST /internal/ai/risk-score`
- Spring Boot `AiSafetyClient` 추가
- `ChatService`에서 risk-score 선호출 후 `HIGH`면 safety 응답으로 분기
- `MEDIUM`이면 supportive 응답으로 분기

현재 상태:
- 코드 구현 완료
- `./gradlew compileJava` 통과
- ai-api Python 문법 확인 완료
- ai-api `risk-score` 직접 호출 확인 완료
- Chat 고위험 메시지 전송 시 safety 문구 저장 확인 완료
- 세션 상세 조회에서 safety assistant 메시지 누적 저장 확인 완료
- Chat 중위험 메시지 전송 시 supportive 문구 저장 확인 완료
- 일반 문장 전송 시 normal 응답 유지 확인 완료

## 현재 테스트 상태

### Swagger
- `signup`, `login`, `refresh`, `users/me`, `diary CRUD`, `date list`, `calendar/monthly-emotions` 테스트 가능
- `Authorize` 버튼 노출되도록 OpenAPI Bearer 설정 반영 완료
- `calendar/daily-summary` 실호출 확인 완료
- `chat session create`, `chat session detail`, `chat message send` 실호출 확인 완료
- `safety risk-score` 및 고위험 Chat 분기 실호출 확인 완료
- `supportive risk-score` 및 중위험 Chat 분기 실호출 확인 완료

### Postman
- `signup`, `login`, `users/me`, `diary create`, `diary detail`, `date list`, `calendar/monthly-emotions` 테스트 통과
- path variable은 `{diaryId}`가 아니라 실제 숫자 또는 `{{diaryId}}` 환경 변수로 호출해야 함
- `calendar/daily-summary` 실호출 확인 완료
- `diary create -> analyze-diary 반영` 실호출 확인 완료
- ai-api `generate-reply` 직접 호출 `NORMAL` 응답 확인 완료
- `chat session create`, `chat session list/detail`, `chat message send` 실호출 확인 완료
- ai-api `risk-score` 직접 호출 `HIGH` 응답 확인 완료
- Chat 고위험 문장 전송 시 safety 문구 저장 확인 완료
- ai-api `risk-score` 직접 호출 `MEDIUM` 응답 확인 완료
- Chat 중위험 문장 전송 시 supportive 문구 저장 확인 완료
- 일반 문장 전송 시 `responseType=NORMAL` 확인 완료

## 현재 주의 사항
- PowerShell과 IntelliJ Run Configuration은 환경변수를 공유하지 않으므로 `JWT_SECRET`, `DB_PASSWORD` 주입 위치를 구분해야 한다.
- `application.yaml`은 datasource를 환경변수 기반으로 읽고, 기본값은 `mindcompass_dev_clean` DB를 바라본다.
- `ddl-auto`는 기본적으로 `validate`이며, `local` 프로필에서는 `application-local.yaml`로 `update`를 사용해 로컬 부팅 막힘을 줄인다.
- Chat 추가 직후 `chat_messages` 누락으로 부팅 실패가 있었고, `V5__chat_backfill.sql`, `FlywayJpaDependencyConfig`, `application-local.yaml`을 추가해 로컬 개발 흐름을 복구했다.
- Chat API 경로는 `/api/v1/chat/session`이 아니라 `/api/v1/chat/sessions`라서 테스트 시 복수형 경로를 써야 한다.
- 새 도메인 테이블 추가 시에는 엔티티만 보지 말고 `db/migration` 반영 여부와 실제 DB 테이블 존재 여부를 먼저 같이 확인해야 한다.
- SQL/YAML 한글 주석은 터미널 출력에서 깨져 보여도 파일 파싱 자체와는 별개일 수 있다.
- `GlobalExceptionHandler`를 보강해 잘못된 path, query, JSON body, enum 값은 500 대신 400/404로 내려가도록 정리했다.
- Chat은 ai-api 실패 시에도 사용자 메시지 저장과 fallback assistant 저장을 유지한다.
- Diary는 ai-api 실패 시에도 diary 저장을 유지하고, AI 분석 결과만 생략될 수 있다.

## 다음 작업 권장 순서
1. ai-api `analyze-diary`, `generate-reply`, `risk-score` 실패 시 로그/운영 가이드 정리
2. Safety Net 중위험/일기 연계 범위 설계
3. 다음 우선순위 도메인인 Report/statistics 범위 결정

## 최근 이슈 메모

### Chat 로컬 부팅 이슈
- 증상: `Schema validation: missing table [chat_messages]`로 `backend-api`가 부팅되지 않음
- 원인:
  - Chat 엔티티는 추가되었지만 실제 DB에는 `chat_messages` 테이블이 없음
  - 로컬 실행에서 Flyway 반영 여부와 JPA validate 순서 확인이 애매해 초기 부팅이 막힘
  - IntelliJ Run Configuration과 datasource 설정이 어긋나면 다른 DB를 보게 될 수 있음
- 조치:
  - `backend-api/src/main/resources/db/migration/V5__chat_backfill.sql` 추가
  - `backend-api/src/main/java/com/mindcompass/api/infra/config/FlywayJpaDependencyConfig.java` 추가
  - `backend-api/src/main/resources/application.yaml`을 환경변수 기반 datasource로 정리
  - `backend-api/src/main/resources/application-local.yaml` 추가로 local 프로필에서만 `ddl-auto: update` 사용
- 재발 방지:
  - 새 도메인 추가 시 migration과 엔티티를 같은 작업에서 같이 반영
  - 로컬 실행 전 `build/resources/main/db/migration` 반영 여부 확인
  - Run Configuration의 DB 관련 env와 실제 기본 datasource가 같은 DB를 가리키는지 확인

### Chat 테스트 메모
- `POST /api/v1/chat/sessions`는 `title`만으로도 생성 가능하며, `sourceDiaryId`는 실제 본인 diary가 있을 때만 연결하는 것이 안전하다.
- Postman에서는 세션 생성 성공 후 `Scripts` 탭의 post-response script로 `sessionId`를 환경변수에 저장해 다음 요청에서 재사용할 수 있다.
- `responseType=FALLBACK`이면 ai-api 연동 실패 가능성을 먼저 보고, `responseType=NORMAL`이면 내부 AI 응답이 정상 저장된 것으로 본다.

### 이번 검증 결과 요약
- `Calendar daily-summary`는 diary가 없는 날짜에도 빈 응답 구조로 안정적으로 반환된다.
- diary 생성 시 사용자 감정 태그와 AI 분석 태그가 함께 저장되는 것을 확인했다.
- ai-api `POST /internal/ai/generate-reply` 직접 호출은 `responseType=NORMAL`로 확인했다.
- Chat 메시지 전송 후 assistant 메시지가 세션 상세 조회에 누적 저장되는 것을 확인했다.

### 예외 응답 정리 메모
- `NoResourceFoundException`은 404로 변환해 잘못된 Chat 경로 호출이 500처럼 보이지 않도록 정리했다.
- `HttpMessageNotReadableException`은 400으로 변환해 Diary enum/JSON 파싱 오류가 500처럼 보이지 않도록 정리했다.
- `MethodArgumentTypeMismatchException`, `MissingServletRequestParameterException`도 400으로 정리했다.

### fallback / 로그 정책 메모
- Chat: ai-api `generate-reply` 실패 시 사용자 메시지는 저장하고, assistant는 fallback 문구와 `responseType=FALLBACK`으로 저장/응답한다.
- Chat: ai-api `risk-score`가 `HIGH`와 `SAFETY_RESPONSE`를 반환하면 assistant는 safety 안내 문구와 `responseType=SAFETY`로 저장/응답한다.
- Chat: ai-api `risk-score`가 `MEDIUM`과 `SUPPORTIVE_RESPONSE`를 반환하면 assistant는 지원형 안내 문구와 `responseType=SUPPORTIVE`로 저장/응답한다.
- Diary: ai-api `analyze-diary` 실패 시 diary 저장은 유지하고, AI 분석 결과 저장만 생략한다.
- 두 흐름 모두 서버 경고 로그를 남겨 운영 중 장애 추적이 가능하도록 했다.

### ai-api 운영 체크리스트 메모
- 공통 1차 점검: `GET http://localhost:8001/health`
- `analyze-diary` 장애 징후: diary 저장은 성공하지만 `AI_ANALYSIS` 태그가 보이지 않음
- `generate-reply` 장애 징후: Chat 응답 `responseType=FALLBACK`
- `risk-score` 장애 징후: 고위험 문장인데 `responseType=SAFETY` 분기가 안 나옴
- backend-api 점검 포인트: `AI_API_BASE_URL`, warning 로그, ai-api 포트(기본 8001)

### Safety Net 검증 메모
- ai-api `POST /internal/ai/risk-score`에 고위험 문장을 보내면 `riskLevel=HIGH`, `recommendedAction=SAFETY_RESPONSE` 응답을 확인했다.
- Chat 메시지 전송에서 고위험 문장을 보내면 일반 reply 대신 safety 안내 문구가 assistant 메시지로 저장되는 것을 확인했다.
- 세션 상세 조회에서 USER 메시지와 SAFETY assistant 메시지가 함께 누적 저장되는 것을 확인했다.
- risk-score 규칙은 정규식 기반으로 보강해 중위험 문장 어미 변화도 일부 허용하도록 정리했다.
- ai-api `POST /internal/ai/risk-score`에 중위험 문장을 보내면 `riskLevel=MEDIUM`, `recommendedAction=SUPPORTIVE_RESPONSE` 응답을 확인했다.
- Chat 메시지 전송에서 중위험 문장을 보내면 `responseType=SUPPORTIVE`와 지원형 assistant 문구 저장을 확인했다.
- 일반 문장 전송에서는 `responseType=NORMAL`로 유지되는 것을 확인했다.
### Diary risk-score 보강 메모
- `DiaryService`가 diary 저장 후 `analyze-diary`, `risk-score`를 각각 후처리로 호출하도록 보강했다.
- `diary_ai_analyses`에 `risk_level`, `risk_score`, `risk_signals`, `recommended_action` 컬럼을 추가하는 `V6__diary_risk_fields.sql`을 추가했다.
- `DiaryDetailResponse`에 위험도 필드를 포함시켜 Swagger / Postman에서 바로 확인할 수 있게 했다.
- `analyze-diary`와 `risk-score`는 서로 독립적으로 실패할 수 있고, 어떤 경우에도 diary 저장 자체는 실패로 돌리지 않는다.
- 기준 확인: `./gradlew compileJava` 통과
- Swagger / Postman 실호출 확인:
  - 중위험 diary -> `riskLevel=MEDIUM`, `recommendedAction=SUPPORTIVE_RESPONSE`
  - 고위험 diary -> `riskLevel=HIGH`, `recommendedAction=SAFETY_RESPONSE`

### Report API 1차 구현
- `GET /api/v1/reports/monthly-summary?year=YYYY&month=MM`
- 월간 diary 수, 평균 감정 강도, 상위 대표 감정, 위험도 집계 응답 추가
- `report` 패키지를 조회 전용 구조로 추가

현재 상태:
- 코드 구현 완료
- `./gradlew compileJava` 통과
- Swagger / Postman 실호출 확인 완료
- 정상 요청 `year=2026, month=3` 응답 구조 확인 완료
- 오류 요청 `month=13` -> `400 Bad Request` 확인 완료

### Report API 2차 구현
- `GET /api/v1/reports/emotions/weekly`
- `GET /api/v1/reports/risks/monthly?year=YYYY&month=MM`
- 최근 7일 감정 추이, 월간 위험도 추이 응답 추가

현재 상태:
- 코드 구현 완료
- `./gradlew compileJava` 통과
- `./gradlew test` 통과
- Swagger / Postman 실호출 확인 완료
- `weekly` 응답 범위와 빈 날짜 구조 확인 완료
- `risks/monthly` 응답 구조와 `month=13 -> 400` 확인 완료

### 테스트 코드 보강
- `ChatServiceTest` 추가
  - `HIGH -> SAFETY`
  - `MEDIUM -> SUPPORTIVE`
  - `LOW -> NORMAL`
- `DiaryServiceTest` 추가
  - diary 저장 후 위험도 필드 반영
- `ReportServiceTest` 추가
  - 월간 요약 조립
  - 주간 감정 추이
  - 월간 위험도 추이
- `ReportControllerTest` 추가
  - `monthly-summary`
  - `emotions/weekly`
  - `risks/monthly`
- `ChatControllerTest` 추가
  - `sessions create/detail`
  - `messages send`
- `DiaryControllerTest` 추가
  - `create/get/update/delete`
  - `date list`
- `SecurityIntegrationTest` 추가
  - 보호 API 무인증 -> `403`
  - 잘못된 Bearer 토큰 -> `403`
  - `X-Request-Id` 응답 헤더 확인
- `OwnershipIntegrationTest` 추가
  - 다른 사용자 diary 접근 -> `404`
  - 다른 사용자 chat session 접근 -> `404`
  - Report 집계는 인증 사용자 diary만 반영
- `PublicApiFlowE2ETest` 추가
  - signup/login -> diary -> report -> chat 정상 흐름
  - high-risk chat -> `SAFETY` 흐름
- WebMvc 경계 케이스 추가
  - Report: required query parameter 누락 -> `400`
  - Chat: blank message -> `400`
  - Diary: invalid enum JSON -> `400`
- 기준 확인: `./gradlew test` 통과

### 운영 로그 / 장애 대응 가이드
- `docs/OPERATIONS_GUIDE.md` 추가
- 기능별 실패 징후, 1차 확인 순서, 로그 키워드, 장애 유형별 대응 절차 정리
- warning / error 기준과 장애 분류 기준 추가
- `requestId` 기반 요청 흐름 식별과 `X-Request-Id` 응답 헤더 규칙 추가
- Chat / Diary / Report 주요 성공 로그에 `requestId`와 핵심 식별자 추가
- Micrometer counter 추가
  - `mindcompass.chat.responses`
  - `mindcompass.chat.ai.failures`
  - `mindcompass.diary.created|updated|deleted`
  - `mindcompass.diary.ai.failures`
  - `mindcompass.report.queries`
- 현재 MVP 운영 기준:
  - AI 실패 시 저장 우선
  - Chat은 fallback / safety / supportive 분기 유지
  - Diary는 저장 후 AI 후처리만 생략 가능

### DB 문서화
- `docs/DB_TABLE_SPECIFICATION.md` 추가
- `docs/sql/erdcloud_current_schema.sql` 추가
- 현재 실제 도메인 테이블 8개 기준으로 컬럼, PK/FK, 인덱스, 관계 요약 정리 완료
- 현재 미구현 후보 테이블(`safety_events`, `monthly_reports`, `ai_response_logs`)도 구분해서 기록

### 모바일 연동 시작 메모
- 모바일 앱 코드는 현재 저장소에 없으므로, 실제 앱 코드 수정 대신 `docs/MOBILE_INTEGRATION_HANDOFF.md`로 연동 핸드오프 문서를 추가했다.
- 화면별 API 연결 기준을 `docs/SCREEN_TO_API_MAPPING.md`에 추가했다.
- 모바일 연동 원칙:
  - 앱은 `backend-api`만 호출
  - `ai-api` 직접 호출 금지
  - Auth -> Diary -> Calendar -> Chat -> Report 순서로 연동

### Flyway 메모
- `V4__chat.sql`은 신규 생성용이다.
- `V5__chat_backfill.sql`은 복구용 히스토리다.
- 둘 다 역할이 달라 현재는 함께 유지하는 것이 맞다.
