# 운영 로그 / 장애 대응 가이드

이 문서는 Mind Compass MVP의 운영 로그 확인 포인트와 1차 장애 대응 순서를 정리한다.

## 1. 운영 원칙

- 모바일 앱은 항상 `backend-api`만 호출한다.
- `ai-api` 장애가 나도 Diary/Chat 저장 자체는 가능한 한 유지한다.
- AI 실패는 `fallback`, `partial success`, `warning log`로 흡수한다.
- 정신건강 도메인 특성상 생성 실패보다 안전 분기와 기록 보존을 우선한다.

## 2. 서버 역할

### backend-api
- 인증 / JWT
- Diary CRUD
- Calendar / Report 조회
- Chat 세션 / 메시지 저장
- ai-api 내부 호출
- fallback / safety / supportive 분기

### ai-api
- `analyze-diary`
- `risk-score`
- `generate-reply`
- 향후 retrieval / context / embeddings

## 3. 공통 1차 확인 순서

1. 어떤 API가 실패했는지 확인
2. `backend-api` warning / error 로그 확인
3. `GET http://localhost:8001/health` 확인
4. ai-api 내부 endpoint 직접 호출 확인
5. `AI_API_BASE_URL` 확인
6. DB 저장은 성공했고 AI 후처리만 실패했는지 분리 판단

## 4. 요청 흐름 식별 기준

- `backend-api`는 모든 요청 시작/종료 시 `requestId`를 로그에 남긴다.
- 클라이언트가 `X-Request-Id` 헤더를 보내면 그 값을 재사용한다.
- 헤더가 없으면 서버가 UUID를 생성한다.
- 응답 헤더에도 `X-Request-Id`를 내려준다.
- 예외 로그와 AI warning 로그에도 같은 `requestId`를 같이 남긴다.

예시:
```text
X-Request-Id: 5c1d0c2f-7b5a-4f42-9870-1ef1bc3b6f1d
```

## 5. 로그 키워드

### 공통
- `requestId=...`
- `Request started`
- `Request completed`

### Chat
- `Chat AI orchestration failed`
- `responseType=FALLBACK`
- `responseType=SAFETY`
- `responseType=SUPPORTIVE`

### Diary
- `Diary AI analysis failed`
- `Diary AI risk scoring failed`

### DB / 부팅
- `Schema validation: missing table`
- `Flyway`
- `Unable to determine Dialect`

## 5-1. 주요 메트릭 이름

- `mindcompass.chat.responses{type=normal|supportive|safety|fallback}`
- `mindcompass.chat.ai.failures`
- `mindcompass.diary.created`
- `mindcompass.diary.updated`
- `mindcompass.diary.deleted`
- `mindcompass.diary.ai.failures{type=analysis|risk}`
- `mindcompass.report.queries{type=monthly_summary|weekly_emotions|monthly_risks}`

## 6. warning / error 기준

### warning
- 잘못된 요청
  - validation 실패
  - 잘못된 query/path/body
- AI 후처리 실패
  - `Diary AI analysis failed`
  - `Diary AI risk scoring failed`
  - `Chat AI orchestration failed`
- 찾을 수 없는 리소스

### error
- 예상하지 못한 서버 예외
- 서버 부팅 실패
- DB 연결 실패
- Flyway / JPA 스키마 불일치

## 7. 장애 분류 기준

### 사용자 요청 오류
- `400`
- `404`
- 입력값, 경로, 권한 범위 문제

### 부분 장애
- Diary/Chat 저장은 성공
- AI 후처리만 실패
- fallback 또는 일부 필드 누락

### 시스템 장애
- 서버 부팅 실패
- DB 연결 실패
- 전역 예외로 `500`

## 8. 기능별 대응

### Diary analyze-diary

징후:
- diary 저장은 됐는데 `AI_ANALYSIS` 태그가 없음
- `diary_ai_analyses` 분석 필드가 비어 있음

확인:
1. `GET http://localhost:8001/health`
2. `POST /internal/ai/analyze-diary` 직접 호출
3. `AI_API_BASE_URL` 확인
4. `Diary AI analysis failed` 로그 확인

판단:
- diary 저장 성공 + AI 분석 실패면 부분 성공이다.

### Diary risk-score

징후:
- `riskLevel`, `riskScore`, `recommendedAction`이 비어 있음

확인:
1. `GET http://localhost:8001/health`
2. `POST /internal/ai/risk-score` 직접 호출
3. `Diary AI risk scoring failed` 로그 확인

판단:
- diary 저장은 유지되고 위험도만 누락될 수 있다.

### Chat generate-reply

징후:
- `responseType=FALLBACK`
- assistant 메시지가 fallback 문구

확인:
1. `GET http://localhost:8001/health`
2. `POST /internal/ai/generate-reply` 직접 호출
3. `Chat AI orchestration failed` 로그 확인

판단:
- 사용자 메시지 저장이 됐으면 부분 성공이다.

### Safety Net

징후:
- 고위험 문장인데 `SAFETY`가 아님
- 중위험 문장인데 `SUPPORTIVE`가 아님

확인:
1. `POST /internal/ai/risk-score` 직접 호출
2. ai-api 최신 코드 재시작 여부 확인

기준:
- `HIGH -> SAFETY`
- `MEDIUM -> SUPPORTIVE`
- `LOW -> NORMAL`

### Report

징후:
- `monthly-summary`, `weekly`, `risks/monthly` 구조 이상
- 잘못된 year/month에서 400이 안 남

확인:
1. backend-api 로그 확인
2. year/month 파라미터 확인
3. diary/risk 집계 데이터 존재 여부 확인

판단:
- Report는 조회 전용이라 ai-api 직접 장애 영향은 없다.

## 9. 자주 나오는 이슈

### ai-api 미기동

징후:
- Chat `FALLBACK`
- Diary AI 필드 누락

대응:
1. ai-api 실행
2. `/health` 확인
3. 내부 endpoint 직접 호출

### ai-api 코드 반영 누락

징후:
- 기대 규칙과 다른 `risk-score`
- 예전 응답이 계속 나옴

대응:
1. ai-api 완전 종료
2. 재시작
3. endpoint 재검증

### DB 스키마 불일치

징후:
- 부팅 실패
- `Schema validation: missing table`

대응:
1. Flyway migration 존재 확인
2. 실제 접속 DB 확인
3. local 프로필 / datasource 확인

메모:
- `V4__chat.sql`은 신규 DB용 생성 migration
- `V5__chat_backfill.sql`은 개발 DB 드리프트 복구용 migration
- 둘은 역할이 달라 현재는 둘 다 유지한다

### Swagger 표시 이상

징후:
- query parameter가 일부만 보임

대응:
1. 브라우저 새로고침
2. backend-api 재시작
3. Postman으로 실제 API 먼저 확인

## 10. 운영 체크리스트

1. `backend-api` 부팅 성공
2. `ai-api` `/health` 성공
3. 요청 로그에 `requestId` 확인
4. Diary 생성 성공
5. Chat 메시지 전송 성공
6. 고위험 문장 `SAFETY`
7. 중위험 문장 `SUPPORTIVE`
8. Report 조회 성공
9. `./gradlew.bat test` 통과 확인
