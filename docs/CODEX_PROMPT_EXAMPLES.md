# CODEX_PROMPT_EXAMPLES

이 문서는 `mind-compass` 프로젝트에서 Codex에 바로 붙여넣어 사용할 수 있는 **실전 프롬프트 예시 모음**입니다.

이번 업데이트는 업로드된 학습형 README 문서의 핵심 원칙을 반영했습니다.

반영한 원칙:
- API를 만들 때 **왜 필요한지**를 먼저 설명한다
- 요청이 들어왔을 때 **어떤 파일이 어떤 순서로 실행되는지**를 설명한다
- **Controller / Service / Repository / Entity / DTO** 또는 **Router / Schema / Service / Client / RAG** 역할을 구분한다
- 코드만 만들지 말고 **학습 가능한 설명**까지 같이 요구한다
- 시스템 프롬프트와 체크리스트의 역할 차이를 고려해, 한 번에 너무 큰 요청을 피하고 작은 작업 단위로 진행한다

전제:
- 루트 `AGENTS.md`가 존재한다
- 필요하면 `backend-api/AGENTS.md`, `ai-api/AGENTS.md`도 존재한다
- `.agents/skills/` 아래에 Skill 3개가 존재한다
  - `create-spring-api-doc`
  - `create-fastapi-internal-endpoint`
  - `generate-erd-and-sql`
- `docs/`와 `docs/ai-api/` 학습 문서가 프로젝트 안에 존재한다

핵심 원칙:
- `AGENTS.md`는 전체 규칙을 준다
- Skill은 반복 작업 방식을 고정한다
- 프롬프트는 이번 작업 범위를 좁혀준다
- 나는 결과 코드를 읽고 학습할 수 있어야 하므로, 이유와 흐름 설명을 항상 같이 받는다

---

# 1. Spring Boot API 작업용 프롬프트

## 1-1. 회원가입 API

```txt
Use the `create-spring-api-doc` skill.

First read:
- AGENTS.md
- README.md
- docs/README.md
- docs/AUTH_API_LEARNING.md

Task:
Create the Spring Boot API skeleton for `POST /api/v1/auth/signup`.

Requirements:
- Explain why this API exists
- Explain which screen/feature calls it
- Show related files
- Provide request/response DTO examples
- Create code skeleton for Controller, Service, Repository, Entity, DTO
- Explain execution flow in order:
  Security/JWT filter -> Controller -> Service -> Repository -> DB -> Response DTO
- Explain the role of Controller / Service / Repository / DTO / Entity
- Explain exception cases
- Explain DB impact

Keep the explanation easy enough for a junior backend developer to understand.
Use medium reasoning effort.
```

## 1-2. 로그인 API

```txt
Use the `create-spring-api-doc` skill.

First read:
- AGENTS.md
- README.md
- docs/README.md
- docs/AUTH_API_LEARNING.md

Task:
Create the Spring Boot API skeleton for `POST /api/v1/auth/login`.

Requirements:
- Explain why this API exists
- Explain which screen/feature calls it
- Show related files
- Provide request/response DTO examples
- Create code skeleton for Controller, Service, Repository, JWT provider usage, DTO
- Explain execution flow in order:
  Controller -> Service -> User lookup -> Password check -> Token creation -> Response
- Explain each file role clearly
- Explain exception cases
- Explain whether refresh token persistence is needed

Use medium reasoning effort.
```

## 1-3. 일기 작성 API

```txt
Use the `create-spring-api-doc` skill.

First read:
- AGENTS.md
- README.md
- docs/README.md
- docs/DIARY_API_LEARNING.md

Task:
Design and generate the Spring Boot API skeleton for `POST /api/v1/diaries`.

Requirements:
- Explain why this API is core to the product
- Explain which app screen uses it
- Show related files in the diary package
- Provide request/response example
- Generate code skeleton for Controller, Service, Repository, Entity, Request DTO, Response DTO
- Explain execution flow step by step
- Explain Controller / Service / Repository separation clearly
- Explain DB insert impact
- Explain what should happen if AI diary analysis is temporarily unavailable

Use medium reasoning effort.
```

## 1-4. 월간 캘린더 조회 API

```txt
Use the `create-spring-api-doc` skill.

First read:
- AGENTS.md
- README.md
- docs/README.md
- docs/CALENDAR_API_LEARNING.md

Task:
Design the Spring Boot API skeleton for `GET /api/v1/calendar/monthly?year=YYYY&month=M`.

Requirements:
- Explain why this API exists
- Explain which screen uses it
- Show related files
- Provide response example focused on calendar-friendly DTO shape
- Generate code skeleton for Controller, Query Service, Query Repository, Response DTO
- Explain execution flow from request to response
- Explain date range logic and DB query impact
- Explain why screen-friendly DTOs are preferred over raw entity output

Use medium reasoning effort.
```

## 1-5. 채팅 메시지 전송 API

```txt
Use the `create-spring-api-doc` skill.

First read:
- AGENTS.md
- README.md
- docs/README.md
- docs/CHAT_API_LEARNING.md
- docs/ai-api/GENERATE_REPLY_API_LEARNING.md
- docs/ai-api/RISK_SCORE_API_LEARNING.md

Task:
Design the Spring Boot API flow for `POST /api/v1/chat/sessions/{sessionId}/messages`.

Requirements:
- Explain why this API exists
- Explain which app screen uses it
- Show related files
- Provide request/response example
- Generate code skeleton for Controller, Service, Repository, DTO, and internal ai-api client
- Explain execution flow step by step
- Include:
  user message persistence,
  risk-score call,
  generate-reply call,
  fallback behavior if ai-api fails,
  safety-first branching if risk is high
- Explain each file role clearly
- Explain DB impact
- Explain exception and fallback behavior clearly

Use high reasoning effort.
```

---

# 2. ai-api 내부 엔드포인트 작업용 프롬프트

## 2-1. analyze-diary

```txt
Use the `create-fastapi-internal-endpoint` skill.

First read:
- AGENTS.md
- README.md
- docs/ai-api/README.md
- docs/ai-api/AI_API_OVERVIEW_LEARNING.md
- docs/ai-api/ANALYZE_DIARY_API_LEARNING.md

Task:
Create the internal FastAPI endpoint `POST /internal/ai/analyze-diary`.

Requirements:
- Explain why this endpoint exists
- Explain which product feature indirectly depends on it
- Show related files in router/schema/service/client
- Provide request/response example
- Generate code skeleton for:
  router,
  request schema,
  response schema,
  service,
  model client
- Explain execution flow inside ai-api
- Explain the role of router / schema / service / client
- Return structured fields like:
  primaryEmotion,
  emotionIntensity,
  emotionTags,
  summary,
  confidence
- Explain failure/fallback behavior for short text, malformed model output, and timeout

Use medium reasoning effort.
```

## 2-2. risk-score

```txt
Use the `create-fastapi-internal-endpoint` skill.

First read:
- AGENTS.md
- README.md
- docs/ai-api/README.md
- docs/ai-api/RISK_SCORE_API_LEARNING.md

Task:
Create the internal FastAPI endpoint `POST /internal/ai/risk-score`.

Requirements:
- Explain why this endpoint exists
- Explain how Spring Boot chat flow depends on it
- Show router/schema/service/client structure
- Provide request/response example
- Return structured fields like:
  riskLevel,
  riskScore,
  signals,
  recommendedAction
- Explain execution flow inside ai-api
- Explain role separation clearly
- Separate risk classification from reply generation
- Explain conservative fallback behavior for ambiguous risky text

Use medium or high reasoning effort depending on complexity.
```

## 2-3. generate-reply

```txt
Use the `create-fastapi-internal-endpoint` skill.

First read:
- AGENTS.md
- README.md
- docs/ai-api/README.md
- docs/ai-api/GENERATE_REPLY_API_LEARNING.md
- docs/ai-api/RAG_CONTEXT_API_LEARNING.md
- docs/CHAT_API_LEARNING.md

Task:
Create the internal FastAPI endpoint `POST /internal/ai/generate-reply`.

Requirements:
- Explain why this endpoint exists
- Explain which chat feature depends on it
- Show related files for router, schemas, service, client, and rag helpers
- Provide request/response example
- Generate code skeleton
- Explain execution flow inside ai-api step by step
- Explain Router / Schema / Service / Client / RAG 역할을 구분해서 설명해줘
- Include:
  conversation history,
  memory summary,
  risk level,
  optional evidence retrieval,
  fallback behavior
- If risk is high, safety-first behavior must override normal reply generation
- Explain failure/fallback behavior clearly

Use high reasoning effort.
```

## 2-4. RAG helper 추가

```txt
Use the `create-fastapi-internal-endpoint` skill.

First read:
- AGENTS.md
- README.md
- docs/ai-api/README.md
- docs/ai-api/RAG_CONTEXT_API_LEARNING.md
- docs/ai-api/GENERATE_REPLY_API_LEARNING.md

Task:
Add a retrieval/context helper used by `generate-reply`.

Requirements:
- Explain why this helper exists
- Explain how it supports evidence-based reply generation
- Show related files for rag/, clients/, and services/
- Generate code skeleton for retriever, context builder, and service integration point
- Explain execution flow inside ai-api
- Explain each helper role clearly
- Explain what should happen if retrieval returns no relevant results

Use medium reasoning effort.
```

---

# 3. ERD / 스키마 / SQL 작업용 프롬프트

## 3-1. MVP 전체 스키마

```txt
Use the `generate-erd-and-sql` skill.

First read:
- AGENTS.md
- README.md
- docs/README.md
- docs/AUTH_API_LEARNING.md
- docs/DIARY_API_LEARNING.md
- docs/CHAT_API_LEARNING.md

Task:
Design the Mind Compass MVP database schema.

Requirements:
- Explain the design principles first
- Include tables for:
  users,
  user_settings,
  refresh_tokens,
  diaries,
  diary_emotions,
  diary_ai_analyses,
  chat_sessions,
  chat_messages,
  ai_response_logs,
  safety_events,
  monthly_reports
- Explain why each table exists
- Explain the relationship summary
- Provide PostgreSQL SQL draft
- Include PK, FK, UNIQUE, CHECK, and practical indexes
- Explain which APIs/features depend on each table
- Explain implementation priority for MVP

Use medium reasoning effort.
```

## 3-2. Chat + Safety 중심 스키마 확장

```txt
Use the `generate-erd-and-sql` skill.

First read:
- AGENTS.md
- README.md
- docs/CHAT_API_LEARNING.md
- docs/ai-api/RISK_SCORE_API_LEARNING.md

Task:
Extend the current schema for Chat + Safety flows.

Requirements:
- Focus on:
  chat_sessions,
  chat_messages,
  ai_response_logs,
  safety_events
- Explain why each table is needed
- Explain how risky messages should be traceable
- Provide relationship summary
- Provide PostgreSQL SQL draft
- Include indexes useful for chat history and safety event lookup
- Explain how this schema supports fallback and debugging

Use high reasoning effort.
```

## 3-3. Diary + AI 분석 스키마

```txt
Use the `generate-erd-and-sql` skill.

First read:
- AGENTS.md
- README.md
- docs/DIARY_API_LEARNING.md
- docs/ai-api/ANALYZE_DIARY_API_LEARNING.md

Task:
Design the schema for diary storage plus AI analysis results.

Requirements:
- Focus on:
  diaries,
  diary_emotions,
  diary_ai_analyses
- Explain why AI analysis should be separated from the main diary table
- Provide relationship summary
- Provide PostgreSQL SQL draft
- Explain which APIs depend on these tables
- Explain MVP-first implementation order

Use medium reasoning effort.
```

---

# 4. 시작용 공통 프롬프트

## 4-1. Spring Boot 작업 시작용

```txt
First read AGENTS.md, README.md, and the relevant docs files.
Use the `create-spring-api-doc` skill.

Today’s task is in backend-api.
Work on one API only.
Explain why the API exists, which files are involved, and the full execution flow.
Start with design and code skeleton before full implementation.
Use medium reasoning effort.
```

## 4-2. ai-api 작업 시작용

```txt
First read AGENTS.md, README.md, and docs/ai-api/README.md.
Use the `create-fastapi-internal-endpoint` skill.

Today’s task is in ai-api.
Work on one internal endpoint only.
Keep router/schema/service/client separation clear.
Explain execution flow and fallback behavior.
Use medium reasoning effort unless the flow includes safety-critical branching, then use high.
```

## 4-3. DB 작업 시작용

```txt
First read AGENTS.md, README.md, and the relevant domain docs.
Use the `generate-erd-and-sql` skill.

Today’s task is schema design.
Explain why each table exists, how tables relate, and provide PostgreSQL SQL draft.
Keep the design MVP-first and easy to implement in Spring Boot.
Use medium reasoning effort.
```

---

# 5. 추천 작업 순서 예시

아래 순서대로 진행하면 안정적입니다.

## 1단계
- Skill: `create-spring-api-doc`
- 대상: `POST /api/v1/auth/signup`
- effort: `medium`

## 2단계
- Skill: `create-spring-api-doc`
- 대상: `POST /api/v1/diaries`
- effort: `medium`

## 3단계
- Skill: `create-fastapi-internal-endpoint`
- 대상: `POST /internal/ai/analyze-diary`
- effort: `medium`

## 4단계
- Skill: `create-spring-api-doc`
- 대상: `POST /api/v1/chat/sessions/{sessionId}/messages`
- effort: `high`

## 5단계
- Skill: `generate-erd-and-sql`
- 대상: Chat + Safety 확장
- effort: `high`

---

# 6. 사용 팁

- 프롬프트 한 번에 한 API 또는 한 내부 엔드포인트만 요청하는 것이 좋습니다.
- 복잡한 흐름(Chat, Safety, RAG, Security)은 `high`를 쓰는 것이 낫습니다.
- DTO/파일 하나 수정 같은 작은 일은 `low`로 줄여도 됩니다.
- 구조 설계 → 코드 골격 → 세부 구현 순서로 가는 것이 안정적입니다.
- 설명이 너무 약하면, 관련 docs 파일을 더 명시해서 다시 요청하면 됩니다.
- 항상 “왜 필요한지 + 실행 순서 + 파일 역할”까지 같이 요구하는 것이 학습에 가장 좋습니다.

---

# 7. 한 줄 요약

이 문서의 목적은  
**업로드된 학습형 README의 원칙까지 반영해서, AGENTS.md + Skills + docs 문서를 전제로 Codex에 바로 붙여넣을 수 있는 실전 프롬프트를 제공하는 것**입니다.
