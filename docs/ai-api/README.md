# Mind Compass ai-api 학습 문서 모음

이 폴더는 원래 `ai-api`(FastAPI) 기준으로 작성된 학습 문서를 모아둔 곳이다.

현재 레포 구조:
- `ai-api` = Spring AI 기반 내부 AI 서버
- `ai-api-fastapi` = 기존 FastAPI 내부 AI 서버

즉, 이 문서들은 지금은 주로 `ai-api-fastapi` 구현을 이해하는 참고 자료로 보면 된다.

이 문서들의 목적:
1. ai-api의 각 내부 AI 엔드포인트가 왜 필요한지 이해한다.
2. 어떤 앱 화면/기능에서 간접적으로 사용되는지 이해한다.
3. Spring Boot -> FastAPI -> AI Service 흐름을 이해한다.
4. FastAPI에서 어떤 파일이 어떤 순서로 실행되는지 이해한다.
5. router / schema / service / client / rag / utils 역할을 구분한다.
6. Codex에게 ai-api 작업을 시킬 때, 단순 코드 생성이 아니라 학습 가능한 형태로 요청한다.

문서 목록:
- `AI_API_OVERVIEW_LEARNING.md`
- `ANALYZE_DIARY_API_LEARNING.md`
- `RISK_SCORE_API_LEARNING.md`
- `GENERATE_REPLY_API_LEARNING.md`
- `RAG_CONTEXT_API_LEARNING.md`
- `SPRING_AI_INTEGRATION_STRATEGY.md`

권장 읽는 순서:
1. AI_API_OVERVIEW_LEARNING.md
2. ANALYZE_DIARY_API_LEARNING.md
3. RISK_SCORE_API_LEARNING.md
4. GENERATE_REPLY_API_LEARNING.md
5. RAG_CONTEXT_API_LEARNING.md

읽는 순서를 이렇게 추천하는 이유:
- 먼저 ai-api 전체 역할을 이해해야 한다.
- 그다음 일기 감정 분석을 이해하면 ai-api의 기본 패턴이 잡힌다.
- 위험 점수 산정은 멘탈헬스 도메인의 안전 설계를 이해하는 핵심이다.
- 답변 생성은 가장 복잡한 흐름이다.
- RAG 문맥 조립은 답변 품질과 신뢰성의 핵심 확장 포인트다.

이 문서들을 프로젝트에 넣는 추천 위치:
```text
mind-compass/
├─ docs/
│  ├─ README.md
│  ├─ AUTH_API_LEARNING.md
│  ├─ DIARY_API_LEARNING.md
│  ├─ CALENDAR_API_LEARNING.md
│  ├─ CHAT_API_LEARNING.md
│  └─ ai-api/
│     ├─ README.md
│     ├─ AI_API_OVERVIEW_LEARNING.md
│     ├─ ANALYZE_DIARY_API_LEARNING.md
│     ├─ RISK_SCORE_API_LEARNING.md
│     ├─ GENERATE_REPLY_API_LEARNING.md
│     └─ RAG_CONTEXT_API_LEARNING.md
├─ backend-api/
└─ ai-api/
```

Codex에게 ai-api 작업을 시킬 때는 이렇게 요청하면 좋다.
- 먼저 `docs/ai-api/README.md`를 읽고
- 관련 학습 문서를 읽고
- 왜 이 내부 API가 필요한지와
- 어떤 파일이 어떤 순서로 실행되는지까지 같이 설명해달라고 요청한다.

