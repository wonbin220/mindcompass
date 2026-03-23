---
name: create-spring-api-doc
description: Create or update a Spring Boot API with learning-oriented documentation. Use this when adding Auth, Diary, Calendar, Report, or Chat endpoints and explain why the API exists, which files are involved, and the request flow from Controller to Service to Repository.
---

# Create Spring API Doc Skill

This skill is for the `mind-compass` project.

Use this skill when the task involves:
- creating a new Spring Boot API endpoint
- updating an existing Spring Boot API endpoint
- documenting a backend API for learning purposes
- explaining request flow for Auth / Diary / Calendar / Chat APIs
- generating code skeleton plus explanation

Do not use this skill for:
- FastAPI-only inference endpoints
- frontend/mobile work
- large cross-project refactors with no single API focus

## Project assumptions

This repository uses a 2-server architecture:
- `backend-api` = Spring Boot public API server
- `ai-api` = FastAPI internal AI server

Core rule:
- The mobile app calls Spring Boot only.
- Spring Boot may call `ai-api` internally.

## What this skill should produce

For each Spring Boot API, produce these sections in order:

1. Goal
2. Why this API exists
3. Which screen/feature calls it
4. Related files
5. Request/response example
6. Code skeleton
7. Execution flow explanation
8. DB impact
9. Exception cases
10. Next step

## Required explanation style

Whenever you generate or update an API, explain the flow in plain language:

1. Request enters Security/JWT filter
2. Controller receives HTTP request
3. Request DTO is parsed
4. Service performs business logic
5. Repository loads/saves data
6. Optional internal AI client call happens
7. Response DTO is built
8. Controller returns response

## Package structure preference

Prefer structure like:

```text
<domain>/
├─ controller/
├─ service/
├─ repository/
├─ domain/
└─ dto/
   ├─ request/
   └─ response/
```

## Domain-specific reminders

### Auth
- Explain how the authenticated user is resolved
- Show where `userId` comes from
- Mention JWT / refresh token if relevant

### Diary
- Diary write must not depend on AI success unless explicitly designed that way
- Explain stable persistence first
- Mention diary analysis only as a follow-up concern

### Calendar / Report
- Prefer screen-friendly DTOs over raw DB shapes
- Explain date range logic and aggregation simply

### Chat
- Save user messages before or independently from AI reply generation when possible
- Explain risk-score / generate-reply / fallback flow if relevant
- Mention Safety Net behavior for risky content

## Output template

Use this structure:

```md
# <API Name>

## 1. Goal

## 2. Why this API exists

## 3. Caller screen/feature

## 4. Related files

## 5. Request example

## 6. Response example

## 7. Code skeleton

## 8. Execution flow

## 9. DB impact

## 10. Exception cases

## 11. Next step
```

## Good example tasks

- Create `POST /api/v1/auth/signup` skeleton with explanation
- Design `GET /api/v1/calendar/monthly` and explain query flow
- Add `POST /api/v1/chat/sessions/{id}/messages` and document AI fallback path
- Explain how `PATCH /api/v1/users/me/settings` works end to end

## Important rule

Do not just output code.
Always teach:
- why the API exists
- which files collaborate
- how the request moves through the system
