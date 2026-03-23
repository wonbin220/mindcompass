# Mind Compass repository instructions

## Read first
Before making changes, read the following files when relevant:
- `README.md`
- `docs/IMPLEMENTATION_STATUS.md`
- `docs/README.md`
- `docs/AUTH_API_LEARNING.md` for auth and user settings work
- `docs/DIARY_API_LEARNING.md` for diary work
- `docs/CALENDAR_API_LEARNING.md` for calendar and report-query work
- `docs/CHAT_API_LEARNING.md` for chat/session flows
- `docs/ai-api/README.md` for internal AI service structure
- `docs/ai-api/AI_API_OVERVIEW_LEARNING.md` for ai-api overview
- `docs/ai-api/ANALYZE_DIARY_API_LEARNING.md` for diary analysis flows
- `docs/ai-api/RISK_SCORE_API_LEARNING.md` for safety and risk scoring
- `docs/ai-api/GENERATE_REPLY_API_LEARNING.md` for reply generation
- `docs/ai-api/RAG_CONTEXT_API_LEARNING.md` for retrieval and context building

## Repository architecture
This project uses a 2-server architecture:
- `backend-api` = Spring Boot public API server
- `ai-api` = FastAPI internal AI inference server

Core rule:
- The mobile app must call Spring Boot only.
- Spring Boot is the single public API entrypoint.
- Spring Boot may call ai-api internally.
- Do not make the mobile app depend on ai-api directly.

## High-level responsibilities

### backend-api
Responsible for:
- auth / JWT / user
- diary CRUD
- calendar queries
- chat session and message persistence
- reports/statistics exposure
- calling ai-api internally
- business flow and primary persistence

### ai-api
Responsible for:
- diary emotion analysis
- risk scoring / safety classification
- AI reply generation
- embeddings / retrieval support
- RAG context assembly
- future experimental inference features

## MVP priorities
Unless explicitly told otherwise, prioritize work in this order:
1. Spring Boot foundation
2. Auth / user
3. Diary CRUD
4. Calendar / emotion queries
5. Minimal ai-api endpoints
6. Chat sessions / messages
7. Safety Net
8. Reports / statistics
9. Advanced AI features

## Project design rules
- Prefer simple, extensible MVP design over premature complexity.
- Keep a practical, industry-standard folder structure unless there is a clear reason to change it.
- Avoid over-engineering or over-spec technology choices for the current stage of the product.
- Prefer project structure and technology choices that improve readability and maintainability for developers.
- Keep the public API boundary in Spring Boot.
- Keep ai-api as an internal inference service.
- Storage, retrieval, and safety come before advanced AI features.
- AI failures must not break the whole product flow.
- Mental health flows require safety-first thinking.

## UI reference rule
- If the user uploads app screen images, use them as product references for API priority and response shape.
- Do not restart design from scratch if the current structure can be extended from those images.
- Map screen-driven work in this order when relevant: Calendar -> Diary analysis -> Chat -> Report.

## File comment rule
- For files you create or modify, add a short Korean file-level comment when the format supports comments.
- Apply this to `.java`, `.py`, `.sql`, `.yaml`, and similar work files where practical.
- For Java, do not limit this to major classes; DTO, enum, repository, exception, config, and utility files should also get short comments.
- Keep comments short and practical; detailed request flow stays in README or docs.

## Progress tracking rule
- Keep implementation progress in `docs/IMPLEMENTATION_STATUS.md` so a new chat can continue with minimal re-explanation.
- When an API or work unit is judged complete, update `docs/IMPLEMENTATION_STATUS.md` in the same task.
- Record at least: completed APIs, verification status (Swagger/Postman), current blockers or cautions, and the next recommended work.
- If README or domain docs changed meaningfully, keep `docs/IMPLEMENTATION_STATUS.md` aligned with that state.

## Documentation rule for every API
Whenever you create or modify an API, explain:
1. why the API exists
2. which screen or feature uses it
3. which files are involved
4. the execution order when a request comes in
5. the role of Controller/Service/Repository/DTO/Entity or Router/Schema/Service/Client
6. main exception cases
7. DB read/write impact if any
8. fallback behavior if AI is involved

## Explanation style
Prefer explanations in a way a junior backend developer can follow.

For backend-api flows, explain in this style:
- Client request
- Security / JWT filter
- Controller
- Service
- Repository / DB
- Internal AI client if used
- Response DTO

For ai-api flows, explain in this style:
- Spring Boot calls ai-api
- Router receives request
- Request schema parses payload
- Service orchestrates logic
- Client / RAG helpers are invoked if needed
- Response schema is built
- Structured response returns to Spring Boot

## Scope control
- Do not try to implement the entire project in one step.
- Work domain by domain, API by API.
- If the task is large, start with design and code skeleton before full implementation.
- Prefer small, reviewable units of work.

## Output preference
Prefer responses in this order:
1. Goal
2. Design decision
3. Related files
4. Code skeleton
5. Execution flow explanation
6. Exception / fallback behavior
7. Next step

## Reasoning effort rule
- Default to `medium`; use `low` for tiny file-local changes, and `high` for cross-layer design, debugging, safety-critical chat flows, or ai-api orchestration.

## Skills
Reusable workflows live in `.agents/skills`.
Prefer these skills when relevant:
- `create-spring-api-doc`
- `create-fastapi-internal-endpoint`
- `generate-erd-and-sql`
