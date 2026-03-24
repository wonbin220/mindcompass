# ai-api instructions

## Read first
Before editing files in `ai-api`, read:
- `../README.md`
- `../README_FASTAPI.md`
- `../docs/README.md`
- `../docs/IMPLEMENTATION_STATUS.md`
- `../docs/ai-api/README.md`

## Role of ai-api
`ai-api` is the Spring AI based internal AI server.

Responsibilities of `ai-api`:
- expose internal AI inference endpoints for Spring Boot
- provide a Spring AI comparison target against `ai-api-fastapi`
- keep request and response contracts stable while implementation strategy changes

Non-responsibilities of `ai-api`:
- public mobile API entrypoint
- auth / JWT / user ownership source of truth
- primary diary/chat persistence

Core boundary:
- mobile app -> `backend-api`
- `backend-api` -> `ai-api` or `ai-api-fastapi`

## Current comparison endpoints
- `POST /internal/ai/analyze-diary`
- `POST /internal/ai/risk-score`
- `POST /internal/ai/generate-reply`
- `GET /health`

