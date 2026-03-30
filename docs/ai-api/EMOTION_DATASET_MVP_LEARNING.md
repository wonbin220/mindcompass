# 감성대화 데이터셋 MVP 학습 문서

이 문서는 AI Hub 감성대화 데이터셋을 Mind Compass의 감정분류 MVP에 맞게 가공하는 방법을 정리한 문서다.

---

# 1. Goal

- 감성대화 원천 데이터의 `xlsx/json` 구조를 빠르게 이해한다.
- 어떤 파일을 학습 기준본으로 쓸지 결정한다.
- 모델 학습용 `csv`를 어떤 컬럼 구조로 만들지 정한다.
- 서비스에서 바로 쓰기 좋은 감정 라벨 체계를 확정한다.

---

# 2. 왜 이 문서가 필요한가

감정분류 모델은 단순히 데이터를 많이 넣는다고 바로 서비스에 맞게 잘 동작하지 않는다.

특히 Mind Compass에서는 아래 조건이 중요하다.

- 모바일 앱은 Spring Boot만 호출해야 한다.
- diary/chat 흐름에서 AI 실패가 전체 저장 실패로 이어지면 안 된다.
- 모델의 원본 라벨과 서비스 라벨이 다를 수 있다.
- 나중에 챗봇 프롬프트, 메모리, RAG와 연결할 수 있어야 한다.

그래서 먼저 데이터 구조와 라벨 체계를 문서로 고정해두는 것이 중요하다.

---

# 3. 현재 확인한 실제 데이터 구조

데이터 폴더:

```text
C:\Users\wonbin\OneDrive\바탕 화면\mindcompass project\018.감성대화
├─ Training_221115_add
│  ├─ 라벨링데이터
│  │  └─ 감성대화말뭉치(최종데이터)_Training.json
│  └─ 원천데이터
│     └─ 감성대화말뭉치(최종데이터)_Training.xlsx
└─ Validation_221115_add
   ├─ 라벨링데이터
   │  └─ 감성대화말뭉치(최종데이터)_Validation.json
   └─ 원천데이터
      └─ 감성대화말뭉치(최종데이터)_Validation.xlsx
```

확인 결과:

- Training JSON 건수: `51,628`
- Validation JSON 건수: `6,640`
- Training XLSX는 `Sheet1` 한 장으로 구성되어 있다.

즉 현재 데이터는:

1. 사람/시스템 대화가 보기 좋게 펼쳐진 `xlsx`
2. 감정 코드와 프로필 메타데이터가 더 구조적으로 들어 있는 `json`

이 두 축으로 이해하면 된다.

---

# 4. XLSX 구조

실제 헤더 예시는 아래와 같다.

```text
연령 | 성별 | 상황키워드 | 신체질환 | 감정_대분류 | 감정_소분류 | 사람문장1 | 시스템문장1 | 사람문장2 | 시스템문장2 | 사람문장3 | 시스템문장3
```

예시 행은 아래처럼 들어 있다.

```text
청년 | 여성 | 진로,취업,직장 | 해당없음 | 분노 | 노여워하는 | 일은 왜 해도 해도 끝이 없을까? 화가 난다. | 많이 힘드시겠어요. 주위에 의논할 상대가 있나요? | 그냥 내가 해결하는 게 나아. 남들한테 부담 주고 싶지도 않고. | 혼자 해결하기로 했군요. 혼자서 해결하기 힘들면 주위에 의논할 사람을 찾아보세요.
```

XLSX의 장점:

- 사람이 읽기 쉽다.
- `감정_대분류`, `감정_소분류`가 한글이라 라벨 설계에 좋다.
- 대화 3턴이 바로 보여서 품질 검수에 좋다.

XLSX의 단점:

- 고유 ID가 명확하게 드러나지 않는다.
- 모델 서빙이나 추적용 메타데이터는 JSON보다 약하다.

---

# 5. JSON 구조

실제 샘플 구조는 아래와 비슷하다.

```json
{
  "profile": {
    "persona-id": "Pro_05349",
    "persona": {
      "persona-id": "A02_G02_C01",
      "human": ["A02", "G02"],
      "computer": ["C01"]
    },
    "emotion": {
      "emotion-id": "S06_D02_E18",
      "type": "E18",
      "situation": ["S06", "D02"]
    }
  },
  "talk": {
    "id": {
      "profile-id": "Pro_05349",
      "talk-id": "Pro_05349_00053"
    },
    "content": {
      "HS01": "일은 왜 해도 해도 끝이 없을까? 화가 난다.",
      "SS01": "많이 힘드시겠어요. 주위에 의논할 상대가 있나요?",
      "HS02": "그냥 내가 해결하는 게 나아. 남들한테 부담 주고 싶지도 않고.",
      "SS02": "혼자 해결하기로 했군요. 혼자서 해결하기 힘들면 주위에 의논할 사람을 찾아보세요.",
      "HS03": "",
      "SS03": ""
    }
  }
}
```

JSON의 장점:

- `persona-id`, `talk-id`, `emotion-id`, `emotion.type(E코드)`가 있다.
- 나중에 데이터 추적, 오류 분석, hard example 관리에 좋다.
- 상황 코드(`Sxx`, `Dxx`)를 별도 feature로 쓸 수 있다.

JSON의 단점:

- 사람이 바로 읽기에는 XLSX보다 덜 직관적이다.
- `E18`, `E31` 같은 코드만 보면 의미를 바로 알기 어렵다.

---

# 6. 학습용으로 어떤 파일을 기준본으로 쓸까

MVP 기준 권장안:

## 6-1. 1차 기준본
`xlsx`를 기준본으로 사용한다.

이유:

- 한글 라벨(`감정_대분류`, `감정_소분류`)이 바로 보인다.
- 텍스트 품질을 사람 눈으로 검수하기 쉽다.
- 초기에 모델 학습용 `csv`를 만들기 가장 쉽다.

## 6-2. 2차 메타데이터 보강본
`json`은 별도 메타데이터 소스로 사용한다.

사용 목적:

- `talk-id`, `persona-id`, `emotion.type(E코드)` 저장
- 오답 분석
- 상황별 성능 분석
- 나중에 RAG/리포트 실험용 feature 확장

즉 처음부터 `xlsx + json`을 억지로 강하게 조인하기보다,

1. `xlsx`로 MVP 학습용 csv를 먼저 만들고
2. `json`으로 추적용 메타데이터를 보강하는 방식이 가장 안전하다.

---

# 7. 학습용 CSV는 어떻게 만들까

권장 출력 파일:

```text
ai-api-fastapi/
└─ training/
   └─ emotion_classifier/
      ├─ raw/
      ├─ interim/
      ├─ processed/
      └─ artifacts/
```

권장 최종 파일:

- `processed/train_emotion_mvp.csv`
- `processed/valid_emotion_mvp.csv`
- 필요 시 `processed/label_map.json`

권장 컬럼:

```text
sample_id
split
source_file
age_group
gender
situation_keyword
physical_disease
emotion_major
emotion_minor
service_label
text
turn_index
dialogue_text
persona_id
talk_id
emotion_code
situation_code
domain_code
```

설명:

- `text`: 실제 모델 입력 문장
- `service_label`: Mind Compass 서비스 기준 최종 라벨
- `turn_index`: `HS01`, `HS02`, `HS03` 중 어느 턴인지
- `dialogue_text`: 같은 대화의 사람 발화 전체를 합친 문자열
- `emotion_code`: JSON의 `E18` 같은 원본 코드

---

# 8. 한 대화를 몇 개의 학습 샘플로 만들까

권장 방식은 2단계다.

## 8-1. 기본 학습 샘플
사람 발화 전체를 합쳐서 1건으로 만든다.

예:

```text
HS01 + " " + HS02 + " " + HS03
```

장점:

- diary처럼 비교적 긴 자유서술 문장에 가깝다.
- 문맥이 끊기지 않는다.
- 실제 서비스 입력과 더 비슷하다.

## 8-2. 보조 증강 샘플
각 `HS01`, `HS02`, `HS03`를 개별 샘플로도 만든다.

장점:

- 데이터 수가 늘어난다.
- 짧은 채팅 문장 분류에도 도움이 된다.

주의:

- 같은 대화에서 나온 샘플은 train/valid에 섞이면 안 된다.
- 반드시 같은 `sample group`으로 묶어서 같은 split에 넣어야 한다.

MVP 추천:

1. 본 모델은 `dialogue_text` 중심으로 먼저 학습
2. 이후 성능 보완용으로 `single turn` 샘플을 추가

---

# 9. 텍스트 전처리는 어디까지 할까

과한 정규화는 추천하지 않는다.

권장 전처리:

- 앞뒤 공백 제거
- 연속 공백 1칸으로 축소
- 빈 문자열 제거
- `HS03`, `SS03`처럼 비어 있는 턴은 제외

가급적 하지 말 것:

- 감탄사 제거
- 맞춤법 강제 교정
- 이모티브한 표현 삭제
- 조사/어미 정규화

이유:

감정분류에서는 “화가 난다”, “너무 불안하다”, “지친다”, “괜찮아진 것 같아” 같은 원문 표현이 오히려 중요한 signal이다.

---

# 10. Mind Compass 서비스용 라벨 체계 확정

## 10-1. 결론

MVP 1차 `primaryEmotion`은 6개로 간다.

- `HAPPY`
- `CALM`
- `ANXIOUS`
- `SAD`
- `ANGRY`
- `TIRED`

이유:

- 너무 적은 4분류는 실제 diary/chat 뉘앙스를 놓치기 쉽다.
- 반대로 처음부터 10분류 이상으로 가면 데이터 불균형과 오분류가 커진다.
- 현재 `backend-api`의 `PrimaryEmotion` enum과도 자연스럽게 연결된다.

## 10-2. 보조 태그는 따로 둔다

보조 `emotionTags` 후보:

- `LONELY`
- `RELIEVED`
- `OVERWHELMED`
- `NUMB`

즉:

- `primaryEmotion` = 서비스 저장/통계용 핵심 라벨
- `emotionTags` = 챗봇 프롬프트, 리포트 설명, 후속 고도화용 보조 태그

---

# 11. 왜 6개가 좋은가

## 11-1. 서비스 관점

Mind Compass는 단순 감정 실험이 아니라:

- 일기 저장
- 감정 요약
- 상담 챗봇
- 위험 신호 분기

까지 연결되어야 한다.

그래서 사용자가 봤을 때 해석 가능한 라벨이어야 한다.

예:

- `ANXIOUS`: 불안, 걱정, 초조, 긴장
- `SAD`: 슬픔, 우울, 상실감
- `ANGRY`: 분노, 짜증, 억울함
- `TIRED`: 피로, 무기력, 탈진
- `HAPPY`: 기쁨, 만족, 설렘
- `CALM`: 안정, 평온, 수용

## 11-2. 모델 관점

이 6개는:

- 한국어 일상 문장 분류에 비교적 구분이 명확하고
- 데이터 수를 어느 정도 확보하기 쉽고
- 챗봇의 tone control에도 바로 쓸 수 있다.

---

# 12. 원본 라벨을 서비스 라벨로 어떻게 매핑할까

원칙은 아래와 같다.

## 12-1. 1차 기준
XLSX의 `감정_대분류`, `감정_소분류`를 우선 사용한다.

## 12-2. 2차 기준
JSON의 `emotion.type(E코드)`는 추적용으로 유지한다.

## 12-3. 예시 매핑

- `기쁨`, `만족`, `설렘` -> `HAPPY`
- `안도`, `편안`, `평온` -> `CALM`
- `불안`, `걱정`, `긴장`, `초조`, `두려움` -> `ANXIOUS`
- `슬픔`, `우울`, `상실감`, `외로움` -> `SAD`
- `분노`, `짜증`, `억울함`, `노여움` -> `ANGRY`
- `피로`, `지침`, `무기력`, `기운없음`, `스트레스 누적` -> `TIRED`

보조 태그 예:

- `외로움`은 primary를 `SAD`로 두고 tag는 `LONELY`
- `안도`는 primary를 `CALM`으로 두고 tag는 `RELIEVED`
- `압도됨`은 primary를 `ANXIOUS` 또는 `TIRED`로 두고 tag는 `OVERWHELMED`
- `멍함`, `감정없음`은 primary를 `TIRED` 또는 `SAD`로 두고 tag는 `NUMB`

---

# 13. 불균형 데이터는 어떻게 처리할까

현재 슬라이드 기준으로 불안 계열이 상대적으로 많을 가능성이 높다.

MVP 권장 순서:

1. 먼저 실제 분포를 집계한다.
2. 극단적으로 많은 라벨만 `undersampling`한다.
3. 나머지는 `class weight`로 보정한다.
4. 평가 지표는 `accuracy`보다 `macro f1`를 우선 본다.

이유:

- 감정분류는 다수 클래스만 잘 맞혀도 accuracy가 높게 나올 수 있다.
- 하지만 서비스에서는 `SAD`, `ANXIOUS`, `TIRED`를 놓치면 더 위험하다.

---

# 14. 모델 학습 입력 형태 추천

권장 입력:

- 최대 길이 `128`
- tokenizer: 모델 전용 tokenizer 사용
- 긴 문장은 `truncation=True`
- 짧은 문장은 `padding=max_length`

권장 평가지표:

- `macro_f1`
- `weighted_f1`
- confusion matrix

권장 split 원칙:

- Training은 기존 Training 파일 기준
- Validation은 기존 Validation 파일 기준
- 같은 대화에서 나온 파생 샘플은 split을 넘지 않게 유지

---

# 15. Mind Compass 코드와 어떻게 연결되는가

연결 파일:

- `backend-api/src/main/java/com/mindcompass/api/diary/domain/PrimaryEmotion.java`
- `backend-api/src/main/java/com/mindcompass/api/diary/client/AiDiaryAnalysisClient.java`
- `ai-api/src/main/java/com/mindcompass/aiapi/dto/AnalyzeDiaryRequest.java`
- `ai-api/src/main/java/com/mindcompass/aiapi/dto/AnalyzeDiaryResponse.java`
- `ai-api-fastapi/app/schemas/analyze_diary.py`
- `ai-api-fastapi/app/services/emotion_analysis_service.py`
- `ai-api-fastapi/app/routers/diary_router.py`

핵심 연결 포인트:

- 모델의 최종 라벨은 `AnalyzeDiaryResponse.primaryEmotion`에 들어가야 한다.
- 서비스가 저장하는 대표 감정은 `PrimaryEmotion` enum과 충돌하지 않아야 한다.
- 나중에 richer label을 쓰더라도 public API 계약은 너무 자주 바뀌지 않는 것이 좋다.

---

# 16. 추천 실행 계획

## 1단계
XLSX 기반으로 `train_emotion_mvp.csv`, `valid_emotion_mvp.csv` 생성

## 2단계
6개 서비스 라벨로 매핑 테이블 확정

## 3단계
`beomi/KcELECTRA-base`로 1차 파인튜닝

## 4단계
`ai-api-fastapi`에 `/internal/ai/analyze-diary` 또는 `/internal/model/emotion-classify` 형태로 서빙

## 5단계
`ai-api` 또는 `backend-api`에서 내부 호출 연결

## 6단계
챗봇 프롬프트에 `primaryEmotion`, `emotionTags`, `riskLevel`, `memorySummary`, `retrievedContext`를 함께 넣기

---

# 17. 다음 단계

다음 문서에서 같이 이어서 정리할 항목:

- `beomi/KcELECTRA-base` 학습 방향
- FastAPI 서빙 구조
- `backend-api -> ai-api -> model server` 호출 구조
- Spring AI 챗봇 + 메모리 + RAG 전체 흐름
