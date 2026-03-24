# Spring AI 연동 전략 문서

이 문서는 `backend-api`가 `ai-api`(Spring AI)와 `ai-api-fastapi`(FastAPI 비교 서버) 사이를
어떻게 안전하게 전환할지 정리한 운영 기준 문서다.

## 1. 목표
- 기본 내부 AI 서버는 `ai-api`(Spring AI)로 둔다.
- 필요할 때만 설정값으로 `ai-api-fastapi`를 선택할 수 있게 한다.
- 모바일 / 웹 클라이언트는 계속 `backend-api`만 호출하게 유지한다.
- Diary / Chat fallback 정책은 AI 서버 구현체가 바뀌어도 그대로 유지한다.

## 2. 현재 전략
- `backend-api`는 `app.ai.provider` 값을 읽는다.
- `spring-ai`면 `app.ai.spring-base-url`을 사용한다.
- `fastapi`면 `app.ai.fastapi-base-url`을 사용한다.
- 실제 호출 엔드포인트 계약은 그대로 유지한다.
  - `POST /internal/ai/analyze-diary`
  - `POST /internal/ai/risk-score`
  - `POST /internal/ai/generate-reply`

## 3. 관련 파일
- `backend-api/src/main/java/com/mindcompass/api/infra/config/AiEndpointProperties.java`
- `backend-api/src/main/java/com/mindcompass/api/infra/config/AiClientConfig.java`
- `backend-api/src/main/resources/application.yaml`
- `backend-api/src/main/java/com/mindcompass/api/diary/client/AiDiaryAnalysisClient.java`
- `backend-api/src/main/java/com/mindcompass/api/chat/client/AiSafetyClient.java`
- `backend-api/src/main/java/com/mindcompass/api/chat/client/AiChatClient.java`

## 4. 요청 흐름
1. 모바일/web 클라이언트가 `backend-api` 공개 API를 호출한다.
2. `backend-api` Service가 AI 후처리가 필요한지 판단한다.
3. 내부 AI client가 `WebClient`를 사용해 `ai-api`를 호출한다.
4. `AiClientConfig`는 `app.ai.provider`에 맞는 base URL을 선택한다.
5. 선택된 내부 AI 서버가 구조화된 응답을 반환한다.
6. `backend-api`가 결과를 저장하거나 fallback 응답을 조립한다.

## 5. 왜 이 전략이 필요한가
- Spring AI를 기본값으로 올려도 FastAPI 비교 경로를 완전히 잃지 않는다.
- 같은 공개 API를 유지한 채 내부 추론 구현체만 바꿀 수 있다.
- 장애 시점, 응답 품질, 운영 편의성을 비교하기 쉽다.
- 앱은 어떤 내부 AI 서버가 선택됐는지 몰라도 된다.

## 6. 환경변수 예시
```yaml
AI_PROVIDER=spring-ai
AI_API_SPRING_BASE_URL=http://localhost:8001
AI_API_FASTAPI_BASE_URL=http://localhost:8002
```

FastAPI 비교가 필요할 때만 아래처럼 바꾼다.

```yaml
AI_PROVIDER=fastapi
AI_API_FASTAPI_BASE_URL=http://localhost:8002
```

## 7. 운영 원칙
- 기본 provider는 `spring-ai`로 유지한다.
- FastAPI는 내가 명시적으로 비교 작업을 요청할 때만 전환한다.
- Diary 저장 성공은 AI 서버 선택과 무관하게 보장해야 한다.
- Chat은 AI 실패 시에도 fallback 응답을 유지해야 한다.
- mental health 서비스이므로 위험도 분기와 안전 응답 우선순위를 유지한다.
