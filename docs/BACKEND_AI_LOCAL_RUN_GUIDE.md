# backend-api 로컬 AI 연동 실행 가이드

이 문서는 `backend-api`를 로컬에서 실행할 때 `ai-api`와 어떤 방식으로 연결해야 하는지 정리한 운영 가이드다.

핵심 원칙은 하나다.

- 기본 로컬 개발은 zero-cost dev
- 실제 OpenAI 품질 검증은 manual에서만 수행

`backend-api` 자체는 OpenAI를 직접 호출하지 않는다. 비용이 생기는 지점은 `backend-api -> ai-api -> OpenAI` 흐름이므로, `backend-api`가 어떤 `ai-api` 프로필을 바라보는지가 중요하다.

## 왜 이 가이드가 필요한가

`backend-api`는 아래 기능에서 내부적으로 `ai-api`를 호출한다.

- Diary 저장 후 `analyze-diary`
- Diary 저장 후 필요 시 `risk-score`
- Chat 메시지 전송 시 `risk-score`
- Chat 답변 생성 시 `generate-reply`

즉, 로컬에서 `backend-api`만 계속 테스트해도, 연결된 `ai-api`가 `manual` 또는 `prod`처럼 OpenAI 활성 모드라면 비용이 발생할 수 있다.

그래서 로컬 기본값은 아래처럼 맞춘다.

1. `ai-api`는 기본 `dev`로 실행한다.
2. `backend-api`는 그 `dev ai-api`를 기본 URL로 바라본다.
3. 실제 OpenAI 응답을 보고 싶은 순간에만 `manual ai-api`를 따로 켠다.

## 관련 파일

- `backend-api/scripts/start-backend-api.ps1`
- `backend-api/src/main/resources/application.yaml`
- `ai-api/scripts/start-ai-api-with-openai.ps1`
- `docs/ai-api/OPENAI_USAGE_AND_PROFILE_GUIDE.md`

## 실행 모드 1. 기본 zero-cost dev

목적:
- 프론트/백엔드 API 연동 확인
- Diary/Chat fallback 흐름 검증
- OpenAI 비용 없이 로컬 개발

실행 순서:
1. `ai-api`를 기본 실행한다.
2. `backend-api`를 기본 스크립트로 실행한다.

예시:

```powershell
cd C:\programing\mindcompass\ai-api
.\gradlew.bat bootRun
```

```powershell
cd C:\programing\mindcompass\backend-api
powershell -ExecutionPolicy Bypass -File .\scripts\start-backend-api.ps1
```

이 모드에서 기대하는 동작:
- `backend-api`는 `http://localhost:8001`의 `ai-api`를 호출한다.
- `ai-api`는 기본 `dev` 프로필이라 OpenAI를 직접 호출하지 않는다.
- Diary/Chat은 fallback 중심으로 계속 동작한다.

## 실행 모드 2. manual OpenAI 검증

목적:
- 실제 LLM 응답 품질 확인
- 프롬프트 결과와 응답 문체 확인

실행 순서:
1. `ai-api`를 `manual` 프로필로 실행한다.
2. `backend-api`를 같은 방식으로 실행한다.

예시:

```powershell
cd C:\programing\mindcompass\ai-api
powershell -ExecutionPolicy Bypass -File .\scripts\start-ai-api-with-openai.ps1 -OpenAiApiKey "sk-..."
```

```powershell
cd C:\programing\mindcompass\backend-api
powershell -ExecutionPolicy Bypass -File .\scripts\start-backend-api.ps1 -AiApiSpringBaseUrl "http://localhost:8001"
```

이 모드에서 기대하는 동작:
- `backend-api`는 여전히 `ai-api`만 호출한다.
- 차이는 `ai-api`가 `manual` 프로필이라 OpenAI를 실제 호출한다는 점이다.

## backend-api 실행 스크립트 설명

파일:
- `backend-api/scripts/start-backend-api.ps1`

이 스크립트는 아래 환경변수를 명시적으로 설정한다.

- `DB_PASSWORD`
- `JWT_SECRET`
- `AI_PROVIDER`
- `AI_API_SPRING_BASE_URL`
- `AI_API_FASTAPI_BASE_URL`

기본값:
- `AI_PROVIDER = spring-ai`
- `AI_API_SPRING_BASE_URL = http://localhost:8001`
- `AI_API_FASTAPI_BASE_URL = http://localhost:8002`

즉 기본 실행만 해도 `backend-api -> ai-api` 경로가 분명하게 보인다.

## 요청이 들어왔을 때 실행 흐름

Diary:
- Client request
  - 반응형 웹이 `backend-api` diary API 호출
- Security / JWT filter
  - 사용자 인증 확인
- Controller
  - Diary 요청 수신
- Service
  - diary 저장 후 `AiDiaryAnalysisClient`, `AiSafetyClient` 호출
- Internal AI client
  - `backend-api`가 `ai-api` 내부 API 호출
- ai-api response
  - `dev`면 fallback 중심
  - `manual`이면 실제 OpenAI 응답 또는 실패 시 fallback
- Response DTO
  - 최종 diary 응답 반환

Chat:
- Client request
  - 반응형 웹이 채팅 메시지 전송 API 호출
- Security / JWT filter
  - 세션 소유권과 사용자 확인
- Controller
  - 메시지 요청 수신
- Service
  - `AiSafetyClient`로 `risk-score`, `AiChatClient`로 `generate-reply` 호출
- Internal AI client
  - `backend-api`가 `ai-api` 내부 API 호출
- Repository / DB
  - 사용자 메시지와 assistant 메시지 저장
- Response DTO
  - 최종 채팅 응답 반환

## 예외 / fallback 동작

- `ai-api`가 꺼져 있으면
  - `backend-api`에서 warning이 나고 diary/chat은 fallback 또는 부분 성공으로 유지될 수 있다.
- `ai-api`가 `dev`이면
  - 내부 AI 호출은 되지만 OpenAI는 실제로 타지 않는다.
- `ai-api`가 `manual`인데 키가 없거나 실패하면
  - `ai-api`가 fallback을 반환하고, `backend-api`는 그 결과를 받아 흐름을 유지한다.

## 다음 시작점

다음 세션에서 이어갈 때는 아래 순서로 보면 된다.

1. `docs/ai-api/OPENAI_USAGE_AND_PROFILE_GUIDE.md`
2. 이 문서
3. `backend-api/scripts/start-backend-api.ps1`
4. `ai-api/scripts/start-ai-api-with-openai.ps1`
