# ai-api instructions

## Read first
Before editing files in `ai-api`, read:
- `../README.md`
- `../docs/README.md`
- `../docs/ai-api/README.md`
- `../docs/ai-api/AI_API_OVERVIEW_LEARNING.md`
- `../docs/ai-api/ANALYZE_DIARY_API_LEARNING.md` for diary emotion analysis
- `../docs/ai-api/RISK_SCORE_API_LEARNING.md` for safety and risk scoring
- `../docs/ai-api/GENERATE_REPLY_API_LEARNING.md` for reply generation
- `../docs/ai-api/RAG_CONTEXT_API_LEARNING.md` for retrieval and context building
- `../docs/CHAT_API_LEARNING.md` when reply generation must align with Spring Boot chat flow
- `../docs/DIARY_API_LEARNING.md` when diary analysis output affects diary UX

## Role of ai-api
`ai-api` is the internal AI server in a Spring Boot + FastAPI 2-server architecture.

Responsibilities of ai-api:
- diary emotion analysis
- risk scoring / safety classification
- AI reply generation
- embeddings / retrieval support
- RAG context assembly
- future experimental inference features

Non-responsibilities of ai-api:
- public mobile API entrypoint
- auth / JWT / user ownership as the source of truth
- primary persistence of diary/chat/business records

The mobile app must not depend on `ai-api` directly.
Spring Boot remains the single public API entrypoint.

## Architecture rules
- Treat ai-api as an internal inference service called by Spring Boot.
- Prefer stateless request/response behavior where possible.
- Keep important user/business persistence decisions in Spring Boot.
- ai-api may return structured analysis/generation results, but Spring Boot decides how they are stored and exposed.
- ai-api failures should not imply that the whole product architecture changes.
- Keep ai-api contracts explicit and predictable.

## Package role rules

### routers
Routers should:
- receive HTTP requests
- parse request schemas
- call services
- return response schemas

Routers should stay thin.
Do not place large prompt logic, retrieval orchestration, or parsing logic directly in routers.

### schemas
Schemas should:
- define request/response contracts
- validate inputs
- keep payloads explicit and stable

### services
Services should:
- contain core inference logic inside ai-api
- orchestrate prompt building, risk classification, retrieval, and output shaping
- remain readable and testable

### clients
Clients should:
- isolate external model/vector store/provider calls
- hide provider-specific request details from services
- support retry/timeout/failure handling where appropriate

### rag
RAG modules should:
- retrieve relevant documents or memory
- assemble useful context
- optionally format evidence/citation payloads

### utils
Utilities should:
- contain low-level helpers only
- avoid becoming a dump for business logic

## MVP priority inside ai-api
Unless explicitly asked otherwise, prioritize work in this order:
1. app bootstrap and config
2. analyze-diary
3. risk-score
4. generate-reply
5. basic RAG helpers used by generate-reply
6. improved parsing / evaluation / tests
7. advanced retrieval / memory / GraphRAG / multimodal experiments

## Endpoint rules
Current key internal endpoints:
- `POST /internal/ai/analyze-diary`
- `POST /internal/ai/risk-score`
- `POST /internal/ai/generate-reply`

When implementing an endpoint, always explain:
1. why the endpoint exists
2. which product feature indirectly depends on it
3. request/response example
4. related files
5. execution order inside ai-api
6. failure cases and fallback behavior
7. how Spring Boot is expected to use the result

## Safety-first rules
This is a mental health related service.

For any chat or diary analysis flow:
- always consider safety implications
- risk scoring may be required before normal reply generation
- do not treat all risky content as ordinary chat
- prefer safety-first branching when risk is high
- avoid overclaiming certainty in sensitive situations
- support limited-confidence behavior when appropriate

When risk is high:
- allow response types such as `SAFETY_RESPONSE`
- prefer grounded, short, safe wording
- avoid unsafe escalation or overconfident therapeutic claims

## Analyze-diary rules
For `analyze-diary`:
- return structured outputs, not vague prose
- prefer fields like `primaryEmotion`, `emotionIntensity`, `emotionTags`, `summary`, `confidence`
- keep label spaces controlled and documented
- handle empty/short/unparseable text gracefully

## Risk-score rules
For `risk-score`:
- separate risk classification from reply generation where possible
- allow hybrid approaches: rule-based + model-based
- return interpretable fields like `riskLevel`, `riskScore`, `signals`, `recommendedAction`
- prefer conservative fallback behavior in ambiguous high-risk cases

## Generate-reply rules
For `generate-reply`:
- treat this endpoint as orchestration, not just raw text generation
- consider conversation history, memory summary, risk level, and optional evidence
- use RAG only when it helps
- allow graceful fallback when retrieval returns nothing
- keep output schema explicit
- if risk is high, safety rules override normal reply style

## RAG rules
For RAG-related code:
- separate retrieval from response generation
- keep `retriever`, `context_builder`, and `citation/evidence formatting` responsibilities distinct
- do not force RAG into every request
- prefer small, relevant context over large noisy context
- document what sources are expected and what happens when none are found

## Prompting rules
- Keep prompts modular and reusable.
- Prefer prompt builder functions/services over inline giant strings in routers.
- Make output format requirements explicit.
- Prefer deterministic structured parsing when possible.
- Document prompt intent in comments when useful.

## Error handling rules
- Do not silently swallow provider failures.
- Return structured fallback responses when possible.
- Make timeout, malformed output, and empty retrieval cases explicit.
- Keep error messages useful for internal debugging without leaking unnecessary provider details.

## Testing preference
Prefer tests for:
- schema validation
- service-level orchestration
- output parsing
- fallback behavior
- high-risk safety branches

## Explanation style
When generating or changing ai-api code, explain the request flow in plain language:
1. Spring Boot calls ai-api
2. Router receives the request
3. Request schema parses the payload
4. Service performs orchestration
5. Client / RAG helpers are invoked if needed
6. Response schema is built
7. ai-api returns structured output to Spring Boot

## Scope control
- Do not try to build the entire ai-api in one step.
- Work endpoint by endpoint, service by service.
- Start with code skeletons and stable contracts before deeper optimization.
- Keep naming practical and easy to understand.

## Output preference
Prefer responses in this order:
1. Goal
2. Design decision
3. Related files
4. Code skeleton
5. Execution flow explanation
6. Failure / fallback behavior
7. Next step

## Reasoning effort rule
- Use `low` for small, well-scoped edits (1-2 files, DTOs, simple endpoint or docs changes).
- Use `medium` as the default for normal API work (one feature/API across controller-service-repository or router-schema-service).
- Use `high` for multi-file design, complex debugging, security issues, AI fallback/safety flows, or Spring Boot <-> ai-api orchestration.
- If unsure, start with `medium`; move to `low` for tiny mechanical work and to `high` for architecture or failure-analysis tasks.