# Mind Compass API 학습 문서 모음

이 폴더는 도메인별 API 학습 문서를 모아둔 곳이다.

문서 목록:
- `AUTH_API_LEARNING.md`
- `DIARY_API_LEARNING.md`
- `CALENDAR_API_LEARNING.md`
- `CHAT_API_LEARNING.md`
- `REPORT_API_LEARNING.md`
- `DB_TABLE_SPECIFICATION.md`
- `sql/erdcloud_current_schema.sql`
- `OPERATIONS_GUIDE.md`
- `MOBILE_INTEGRATION_HANDOFF.md`
- `SCREEN_TO_API_MAPPING.md`

이 문서들의 목적:
1. 각 API가 왜 필요한지 이해한다.
2. 어떤 화면에서 호출되는지 이해한다.
3. 어떤 파일들이 순차적으로 실행되는지 이해한다.
4. Controller / Service / Repository / DTO / Entity 역할을 구분한다.
5. Codex에게 API를 시킬 때 단순 코드 생성이 아니라 학습 가능한 형태로 요청한다.

권장 읽는 순서:
1. Auth
2. Diary
3. Calendar
4. Chat
5. Report
6. DB Table Specification

이 순서가 좋은 이유:
- Auth가 사용자 식별의 시작점
- Diary가 핵심 기록 기능
- Calendar가 기록을 보는 조회 기능
- Chat이 가장 복잡한 AI 연계 흐름
- Report가 기록과 안전 신호를 기간 단위 인사이트로 요약
- DB Table Specification이 실제 테이블 구조와 관계를 한 번에 정리

앞으로 새 도메인을 추가할 때도 같은 형식으로 문서를 늘리면 된다.
