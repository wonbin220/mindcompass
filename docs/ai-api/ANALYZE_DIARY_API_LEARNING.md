# Analyze Diary API 학습 문서

이 문서는 ai-api의 `일기 감정 분석` 내부 API를 학습하기 위한 문서다.

---

# 1. 이 API가 왜 필요한가

사용자가 일기를 작성하면, 그 글에 담긴 감정 상태를 기계적으로 저장하는 것만으로는 부족하다.

서비스는 이런 질문에 답하고 싶다.
- 이 일기에서 대표 감정은 무엇인가?
- 감정 강도는 어느 정도인가?
- 불안/슬픔/분노/안정 같은 감정이 어떤 비율로 드러나는가?
- 후속 상담으로 이어질 만한 포인트가 있는가?

그래서 `analyze-diary` API가 필요하다.

이 API는 주로:
- 일기 저장 직후 AI 분석
- 일기 상세 화면에서 감정 요약 표시
- 월간 리포트용 보조 데이터 생성
에 사용된다.

---

# 2. 엔드포인트

`POST /internal/ai/analyze-diary`

이 엔드포인트는 외부 앱이 아니라 Spring Boot가 내부적으로 호출한다.

---

# 3. 어떤 화면/기능에서 간접적으로 쓰이는가

직접 호출 주체는 Spring Boot지만, 실제 사용자 기능으로는 아래와 연결된다.

- 일기 작성 후 감정 분석 결과 표시
- 일기 상세 화면의 대표 감정/감정 태그
- 통계 메뉴의 감정 분류 데이터
- AI 상담 시작 전 초기 감정 파악

즉 사용자는 이 API를 직접 보지 않지만,
실제 앱 경험에는 깊게 연결된다.

---

# 4. 요청 예시

```json
{
  "userId": 1,
  "diaryId": 101,
  "content": "오늘은 일이 많아서 힘들었지만, 저녁 산책 후 마음이 조금 차분해졌다.",
  "writtenAt": "2026-03-18T21:30:00"
}
```

---

# 5. 응답 예시

```json
{
  "primaryEmotion": "CALM",
  "emotionIntensity": 2,
  "emotionTags": ["CALM"],
  "summary": "TIRED는 아직 fallback 전용으로 취급되어 보수적인 기본 감정 결과로 처리했습니다.",
  "confidence": 0.10
}
```

---

# 6. 관련 파일 예시

## FastAPI
- `app/routers/diary_router.py`
- `app/schemas/analyze_diary.py`
- `app/services/emotion_analysis_service.py`
- `app/services/prompt_builder_service.py`
- `app/clients/openai_client.py`
- `app/utils/text_cleaner.py`

## Spring Boot 측 호출부
- `backend-api/.../client/AiDiaryClient.java`
- `backend-api/.../service/DiaryService.java`

---

# 7. FastAPI 안에서 실행 순서

1. Spring Boot가 ai-api로 `/internal/ai/analyze-diary` 요청을 보낸다.
2. `diary_router.py`가 요청을 받는다.
3. 요청 body가 `AnalyzeDiaryRequest` 스키마로 변환된다.
4. router가 `emotion_analysis_service.analyze_diary()`를 호출한다.
5. service가 텍스트 전처리를 수행한다.
6. 필요하면 `prompt_builder_service`가 분석용 프롬프트를 만든다.
7. `openai_client`가 LLM 호출을 수행한다.
8. 응답을 파싱해서 대표 감정, 강도, 태그, 요약을 구조화한다.
9. `AnalyzeDiaryResponse` 스키마로 응답을 만든다.
10. FastAPI가 Spring Boot로 결과를 반환한다.
11. Spring Boot가 결과를 저장하거나 diary 응답에 반영한다.

---

# 8. 파일별 역할을 쉽게 설명하면

## diary_router.py
HTTP 요청을 받는 입구다.
- URL 연결
- 요청 스키마 파싱
- service 호출
- 응답 반환

## analyze_diary.py
입출력 스키마다.
- 요청 필드 검증
- 응답 구조 정의

## emotion_analysis_service.py
핵심 로직 담당이다.
- 텍스트 분석
- 감정 분류
- 요약 생성
- confidence 계산

## prompt_builder_service.py
LLM에게 보낼 프롬프트를 정리한다.
- 분석 기준 문구
- 감정 라벨 정의
- 출력 형식 강제

## openai_client.py
실제 외부 모델 호출 담당이다.

## text_cleaner.py
불필요한 공백, 특수문자, 전처리 로직을 담당한다.

---

# 9. 왜 Router와 Service를 분리해야 하나

초보가 자주 하는 실수는 router에 로직을 다 넣는 것이다.

하지만 그렇게 하면:
- 코드가 길어진다
- 테스트가 어렵다
- 다른 API에서 재사용이 안 된다

그래서 원칙은:
- router = 얇게
- service = 핵심 로직

---

# 10. 예외 상황

- 빈 문자열 일기
- 지나치게 짧아서 감정 추출이 어려운 일기
- LLM 응답 포맷 파손
- 외부 모델 타임아웃
- 감정 라벨이 사전 정의 범위를 벗어남

이때는 보통:
- fallback 감정값
- 낮은 confidence
- 단순 요약 결과
등으로 대응할 수 있다.

현재 FastAPI 비교 서버 기준 추가 메모:
- `TIRED`는 아직 학습 품질이 부족해서 learned primary emotion으로 그대로 내보내지 않는다.
- 모델이 `TIRED`를 예측하면 `CALM` fallback으로 내려가며, emotion-classify 응답에는 `fallbackReason="TIRED_FALLBACK_ONLY"`가 포함될 수 있다.
- analyze-diary는 이 정책을 반영한 보수적 summary를 반환한다.

---

# 11. 학습 포인트

- 이 API는 diary를 저장하는 API가 아니라 분석하는 API다.
- 실제 저장 책임은 Spring Boot 쪽에 있고, ai-api는 분석 결과를 돌려준다.
- ai-api 안에서는 Router -> Schema -> Service -> Client 순서로 이해하면 쉽다.

---

# 12. 실패 시 운영 가이드

## 실패 징후
- diary 생성/수정은 성공했는데 AI 분석 태그가 응답에 보이지 않는다.
- `emotionTags`에 `sourceType=AI_ANALYSIS`가 없다.
- 서버 로그에 `Diary AI analysis failed`가 남는다.

## backend-api 영향
- diary 저장은 유지된다.
- 사용자 입력 감정 태그만 응답에 남을 수 있다.
- AI 분석 결과 저장(`diary_ai_analyses`, AI emotion tag 저장)은 생략될 수 있다.

## 1차 확인 순서
1. ai-api가 실행 중인지 `GET http://localhost:8001/health`로 확인
2. `POST /internal/ai/analyze-diary` 직접 호출로 응답 확인
3. `AI_API_BASE_URL`이 `http://localhost:8001`인지 확인
4. backend-api 로그에서 `Diary AI analysis failed` 경고 확인

## 운영 메모
- analyze-diary 실패는 diary 저장 실패보다 우선순위가 낮다.
- 운영 중 장애가 나도 기록 기능을 막지 않는 것이 현재 MVP 원칙이다.

---

# 13. Spring AI 비교 서버 기준 현재 구조

현재 `ai-api`(Spring AI)는 analyze-diary를 아래 순서로 처리한다.

1. `DiaryAiController`가 `/internal/ai/analyze-diary` 요청을 받는다.
2. `DiaryAnalysisService`가 먼저 Spring AI 프롬프트 호출을 시도한다.
3. `SpringDiaryAnalysisPromptClient`가 `ChatClient`로 JSON 전용 응답을 요청한다.
4. 서비스가 `primaryEmotion`, `emotionIntensity`, `emotionTags`, `summary`, `confidence`를 구조화 파싱한다.
5. 모델 응답 파싱이 실패하거나 모델 호출이 불가능하면 규칙 기반 fallback으로 내려간다.

즉 현재 Spring AI 비교 서버의 핵심은:
- 가능하면 프롬프트 기반 분석 시도
- 실패해도 backend-api 계약은 깨지지 않게 fallback 유지
- FastAPI와 비교할 수 있도록 결과 형식을 동일 계약으로 맞춤
