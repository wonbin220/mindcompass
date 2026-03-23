---
name: create-fastapi-internal-endpoint
description: Create or update an internal ai-api endpoint for FastAPI using router-schema-service-client-rag separation. Use this for analyze-diary, risk-score, generate-reply, and retrieval/context helpers, and explain execution flow from Spring Boot call to FastAPI response.
---

# Create FastAPI Internal Endpoint Skill

This skill is for the `mind-compass` `ai-api` service.

Use this skill when the task involves:
- creating a FastAPI internal endpoint
- updating ai-api router/schema/service code
- documenting analyze-diary / risk-score / generate-reply behavior
- explaining how Spring Boot calls FastAPI
- adding retrieval or context assembly helpers for ai-api

Do not use this skill for:
- Spring Boot controllers/services/repositories
- mobile/frontend work
- public API design that bypasses Spring Boot

## Project assumptions

`ai-api` is an internal inference service.
It is not the public API entrypoint.

Core rule:
- mobile app -> Spring Boot
- Spring Boot -> ai-api

## Preferred ai-api structure

```text
app/
├─ routers/
├─ schemas/
├─ services/
├─ clients/
├─ rag/
└─ utils/
```

## Responsibility rules

### routers
- receive request
- parse schema
- call service
- return response schema
- stay thin

### schemas
- define explicit request/response contracts
- validate payloads
- keep outputs structured

### services
- orchestrate inference logic
- call prompt builders / clients / retrieval helpers
- handle parsing and shaping of outputs

### clients
- isolate model provider / vector store calls
- handle provider-specific details

### rag
- retrieve evidence
- assemble context
- optionally format evidence output

### utils
- low-level helper logic only

## Safety-first rule

This is a mental health related project.

For any ai-api endpoint:
- think about safety implications
- avoid overclaiming certainty
- support structured fallback behavior
- if risk is high, safety-first behavior overrides normal reply flow

## Endpoint-specific reminders

### analyze-diary
Prefer outputs like:
- `primaryEmotion`
- `emotionIntensity`
- `emotionTags`
- `summary`
- `confidence`

### risk-score
Prefer outputs like:
- `riskLevel`
- `riskScore`
- `signals`
- `recommendedAction`

Separate risk classification from reply generation when possible.

### generate-reply
Treat this as orchestration:
- conversation history
- memory summary
- risk level
- optional evidence
- response mode
- fallback behavior

## What this skill should produce

For each internal endpoint, produce:

1. Goal
2. Why the endpoint exists
3. Which product feature indirectly depends on it
4. Related files
5. Request/response example
6. Code skeleton
7. Execution flow inside ai-api
8. Failure/fallback behavior
9. Next step

## Required explanation style

Explain the flow in this order:

1. Spring Boot calls ai-api
2. Router receives the request
3. Request schema parses the payload
4. Service performs orchestration
5. Client / RAG helpers are invoked if needed
6. Response schema is built
7. ai-api returns structured output to Spring Boot

## Output template

```md
# <Endpoint Name>

## 1. Goal

## 2. Why this endpoint exists

## 3. Indirect product usage

## 4. Related files

## 5. Request example

## 6. Response example

## 7. Code skeleton

## 8. Execution flow inside ai-api

## 9. Failure / fallback behavior

## 10. Next step
```

## Good example tasks

- Create `POST /internal/ai/analyze-diary`
- Design `POST /internal/ai/risk-score`
- Implement `POST /internal/ai/generate-reply`
- Add retrieval helper used by `generate-reply`
- Explain how `risk-score` and `generate-reply` should stay separated
