# RAG Context API 학습 문서

이 문서는 ai-api의 `RAG 문맥 조립` 관련 흐름을 학습하기 위한 문서다.

주의:
MVP에서는 RAG를 거대한 시스템으로 먼저 만들 필요는 없다.
하지만 generate-reply의 품질과 신뢰성을 높이기 위해
RAG 흐름을 별도 문서로 이해해두는 것이 좋다.

---

# 1. RAG가 왜 필요한가

LLM은 그럴듯한 답변을 만들 수 있지만,
멘탈헬스 도메인에서는 “그럴듯함”만으로는 부족하다.

서비스는 다음을 원한다.
- 검증된 가이드/근거를 바탕으로 답변하고 싶다
- 사용자가 원하면 근거 요약을 같이 보여주고 싶다
- 안전한 기법(CBT, grounding 등)을 더 일관되게 안내하고 싶다

그래서 RAG가 필요하다.

---

# 2. RAG가 하는 일

RAG는 보통 아래 3단계로 이해하면 쉽다.

1. 검색
   - 관련 문서/가이드/기억을 찾는다

2. 문맥 조립
   - 검색 결과를 현재 질문과 맞게 정리한다

3. 생성 보조
   - 조립된 문맥을 LLM 프롬프트에 넣어 답변 품질과 신뢰성을 높인다

---

# 3. ai-api 안의 관련 파일 예시

- `app/rag/retriever.py`
- `app/rag/context_builder.py`
- `app/rag/citation_formatter.py`
- `app/clients/vector_store_client.py`
- `app/clients/openai_client.py`
- `app/services/reply_generation_service.py`

---

# 4. 어떤 화면/기능에서 간접적으로 쓰이는가

- 근거 기반 AI 상담
- “이 답변의 근거 보기” UX
- 공감 최소화 / 근거 중심 모드
- CBT/마이크로 미션 제안
- 안전 기법 안내

즉, 사용자는 `RAG API`라는 이름을 모르지만,
실제로는 더 믿을 수 있는 답변을 받게 된다.

---

# 5. 예시 흐름

사용자 메시지:
“불안해서 숨이 막히는 느낌이 들 때 바로 할 수 있는 방법이 있을까요?”

RAG 흐름:
1. retriever가 불안, grounding, 호흡 안정화 관련 문서를 찾는다.
2. context_builder가 현재 질문과 관련 높은 문장만 추린다.
3. reply_generation_service가 이 근거를 프롬프트에 넣는다.
4. LLM이 그 문맥을 참고해 답변을 생성한다.
5. citation_formatter가 필요하면 근거 요약을 정리한다.

---

# 6. 실행 순서

1. `generate-reply` 요청이 들어온다.
2. `reply_generation_service`가 RAG 필요 여부를 판단한다.
3. 필요하면 `retriever.py`가 vector store나 문서 저장소를 조회한다.
4. `vector_store_client.py`가 실제 검색 시스템과 통신한다.
5. 검색 결과가 `context_builder.py`로 전달된다.
6. context_builder가 현재 메시지와 관련 높은 문맥만 조립한다.
7. 필요하면 `citation_formatter.py`가 응답용 근거 요약을 만든다.
8. 조립된 문맥이 LLM 프롬프트로 들어간다.
9. 생성 결과와 함께 근거 정보가 반환된다.

---

# 7. 파일별 역할

## retriever.py
무엇을 검색할지 결정하고 결과를 모은다.

## vector_store_client.py
벡터 DB나 검색 시스템과 실제 통신한다.

## context_builder.py
검색 결과 중 지금 질문에 필요한 부분만 골라 정리한다.

## citation_formatter.py
사용자에게 보여줄 근거 요약을 만든다.

## reply_generation_service.py
RAG를 쓸지 말지, 어느 정도 반영할지 결정한다.

---

# 8. 왜 RAG를 별도 문서로 이해해야 하나

초반에는 generate-reply 안에 다 같이 넣고 싶을 수 있다.
하지만 RAG를 분리해서 이해하면 장점이 있다.

- 검색 품질 개선 포인트가 보인다
- 답변 생성과 검색 책임이 분리된다
- 나중에 GraphRAG나 개인 기억 검색으로 확장하기 쉽다
- “근거 기반 상담”이라는 차별점을 설계하기 좋다

---

# 9. 예외 상황

- 검색 결과가 없음
- 검색 결과가 너무 많음
- 관련 없는 문서가 섞임
- 근거와 생성 답변이 어긋남

대응 전략:
- 검색 점수 threshold 적용
- top-k 제한
- context 압축
- 근거 없는 fallback 응답 허용

---

# 10. 학습 포인트

- RAG는 답변을 대신 만드는 것이 아니라 답변을 돕는 구조다.
- 검색/문맥 조립/생성을 나눠 생각하면 이해가 쉬워진다.
- 멘탈헬스 도메인에서는 RAG가 신뢰성과 일관성을 높이는 역할을 한다.
