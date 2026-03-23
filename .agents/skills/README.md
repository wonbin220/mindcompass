# Mind Compass Skills

This directory contains reusable Codex Skills for the `mind-compass` project.

Skills are used for repeatable workflows that should not bloat `AGENTS.md`.
Project-wide rules stay in `AGENTS.md`.
Reusable task playbooks live here in `.agents/skills`.

## Included skills

### create-spring-api-doc
Use for Spring Boot API work that must include:
- why the API exists
- which screen/feature calls it
- related files
- request flow from Controller to Service to Repository
- DB impact and exception cases

Typical use cases:
- Auth API skeletons
- Diary CRUD endpoint docs
- Calendar query endpoint design
- Chat API flow explanation

### create-fastapi-internal-endpoint
Use for FastAPI internal AI endpoint work that must include:
- why the endpoint exists
- indirect product usage
- router / schema / service / client / rag separation
- execution flow inside ai-api
- failure and fallback behavior

Typical use cases:
- `analyze-diary`
- `risk-score`
- `generate-reply`
- retrieval/context helpers

### generate-erd-and-sql
Use for schema design work that must include:
- why each table exists
- relationship summary
- PostgreSQL SQL draft
- implementation priority

Typical use cases:
- MVP schema design
- ERD updates
- Flyway draft creation
- Safety / Chat schema extensions

## Recommended repository structure

```text
mind-compass/
├─ AGENTS.md
├─ .agents/
│  └─ skills/
│     ├─ README.md
│     ├─ create-spring-api-doc/
│     │  └─ SKILL.md
│     ├─ create-fastapi-internal-endpoint/
│     │  └─ SKILL.md
│     └─ generate-erd-and-sql/
│        └─ SKILL.md