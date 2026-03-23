# backend-api instructions

## Read first
Before editing files in `backend-api`, read:
- `../README.md`
- `../docs/README.md`
- `../docs/AUTH_API_LEARNING.md` for auth
- `../docs/DIARY_API_LEARNING.md` for diary
- `../docs/CALENDAR_API_LEARNING.md` for calendar and report queries
- `../docs/CHAT_API_LEARNING.md` for chat flows
- `../docs/ai-api/README.md` to understand the internal AI service boundary
- `../docs/ai-api/ANALYZE_DIARY_API_LEARNING.md` when backend-api calls diary analysis
- `../docs/ai-api/RISK_SCORE_API_LEARNING.md` when backend-api calls safety scoring
- `../docs/ai-api/GENERATE_REPLY_API_LEARNING.md` when backend-api calls AI reply generation

## Role of backend-api
`backend-api` is the main public API server.

It is responsible for:
- auth / user / JWT
- diary CRUD
- calendar queries
- chat session and message persistence
- reports
- public REST API contracts
- business flow and source of truth
- calling `ai-api` internally when needed

It is not responsible for:
- being the primary place for prompt-heavy AI orchestration
- exposing ai-api directly to the mobile app
- pushing core persistence responsibility into ai-api

## Architecture rules
- Treat Spring Boot as the source of truth for business flow.
- Persist important user actions in Spring Boot even if ai-api fails.
- Never make the mobile app depend on ai-api directly.
- Keep controllers thin.
- Put business decisions in services.
- Put DB access in repositories.
- Use DTOs for request/response boundaries.
- Do not expose entities directly as API responses.

## Package structure preference
Prefer package structure like:
- `auth`
- `user`
- `diary`
- `calendar`
- `report`
- `chat`
- `common`
- `infra`

Inside each domain, prefer:
- `controller`
- `service`
- `repository`
- `domain`
- `dto/request`
- `dto/response`

## API implementation rule
For each API you implement, include:
1. purpose of the API
2. caller screen/feature
3. request/response example
4. related files
5. execution order from request to response
6. DB impact
7. exception cases
8. whether ai-api is involved and what happens if it fails

## Request-flow explanation rule
When you create an endpoint, explain the execution flow in plain language:
1. Request enters Security/JWT filter
2. Controller receives HTTP request
3. Request DTO is parsed
4. Service performs business logic
5. Repository loads/saves data
6. Optional internal AI client call happens
7. Response DTO is built
8. Controller returns response

## Auth-specific rule
For protected APIs:
- explain how the authenticated user is resolved
- show where `userId` comes from
- include user ownership checks where needed

## Diary-specific rule
For diary APIs:
- prioritize stable persistence first
- do not make diary creation depend on AI success
- diary write should still succeed when ai-api is unavailable unless explicitly designed otherwise
- if diary analysis is triggered, explain the async/sync decision clearly

## Calendar/report rule
For calendar and report APIs:
- optimize for read models and screen-friendly response shapes
- do not return raw DB shapes if a view-specific DTO is better
- explain date range logic and aggregation logic clearly

## Chat-specific rule
For chat APIs:
- save the user message before or independently from AI reply generation when possible
- consider timeout/failure handling for ai-api
- consider Safety Net branching for risky messages
- persist enough data to reconstruct the conversation later

## Internal AI client rule
When backend-api calls ai-api:
- keep the client layer separate (`client` or `infra/client`)
- make request/response DTOs explicit
- document timeout / retry / fallback decisions
- explain what is persisted before and after the ai-api call

## Safety rule
This is a mental health related service.
When implementing chat or AI-related endpoints:
- always consider risk scoring
- allow safety-first responses
- avoid treating all risky content as a normal chat reply path

## Delivery style
Prefer:
1. goal
2. design decision
3. related files
4. code skeleton
5. request flow explanation
6. DB impact / exception / fallback
7. next step

## Reasoning effort rule
- Use `low` for small, well-scoped edits (1-2 files, DTOs, simple endpoint or docs changes).
- Use `medium` as the default for normal API work (one feature/API across controller-service-repository or router-schema-service).
- Use `high` for multi-file design, complex debugging, security issues, AI fallback/safety flows, or Spring Boot <-> ai-api orchestration.
- If unsure, start with `medium`; move to `low` for tiny mechanical work and to `high` for architecture or failure-analysis tasks.