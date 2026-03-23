# Mind Compass 백엔드 학습형 README (Codex용 포함)

이 문서는 단순한 프로젝트 소개서가 아니라,  
**내가 직접 백엔드 구조를 이해하고 학습하기 위한 README**이자  
**Codex에게 작업을 시킬 때 흔들리지 않도록 기준을 고정하는 문서**다.

즉, 이 README의 목적은 두 가지다.

1. 내가 `Spring Boot + FastAPI 2서버 구조`를 이해할 수 있게 한다.  
2. Codex가 API를 만들 때, 왜 이 API가 필요한지와 어떤 파일이 어떤 순서로 실행되는지까지 함께 설명하도록 유도한다.

---

# 1. 왜 이런 문서를 따로 만드는가

Codex나 다른 AI에게 개발을 맡기면, 결과물이 빨리 나오긴 하지만 다음 문제가 자주 생긴다.

- 코드는 생겼는데 내가 이해를 못 한다.
- 왜 이 API가 필요한지 설명이 없다.
- 컨트롤러, 서비스, 레포지토리 역할이 머릿속에서 섞인다.
- 요청이 들어왔을 때 어떤 파일이 먼저 실행되는지 감이 안 잡힌다.
- 나중에 비슷한 API를 만들 때 기준이 흔들린다.
- AI가 한 번에 너무 많은 걸 만들어서 오히려 구조가 복잡해진다.

그래서 이 프로젝트는 처음부터 아래 원칙으로 간다.

## 이 프로젝트의 학습 원칙

- **코드만 만들지 말고, 왜 그렇게 만들었는지 같이 남긴다.**
- **API를 만들 때마다 실행 흐름을 문서화한다.**
- **API나 작업 단위가 끝났다고 판단되면 `docs/IMPLEMENTATION_STATUS.md`에 완료 상태와 다음 작업을 기록한다.**
- **Controller / Service / Repository / Entity / DTO 역할을 구분해서 이해한다.**
- **Spring Boot가 외부 요청의 진입점이라는 사실을 계속 유지한다.**
- **AI 기능보다 먼저 기록, 저장, 조회, 인증, 안전성부터 이해한다.**
- **앱 화면 이미지가 있으면 그 이미지를 API 설계 우선순위와 응답 형태의 참고 기준으로 사용한다.**
- **생성하거나 수정하는 코드/설정/SQL 파일에는 가능한 범위에서 짧은 한글 주석을 달고, 자세한 흐름 설명은 README와 docs에 남긴다.**
- **폴더 구조는 현업에서 이해하기 쉬운 실용적인 형태를 유지하고, 특별한 이유가 없으면 과하게 복잡한 계층을 늘리지 않는다.**
- **기술 선택은 현재 단계에 맞는 수준으로 제한하고, 오버스펙이나 과한 추상화보다 가독성과 유지보수성을 우선한다.**

## 진행 상태 문서를 따로 두는 이유

새 채팅에서는 원칙 문서만으로는 “어디까지 끝났는지”가 부족할 수 있다.  
그래서 `docs/IMPLEMENTATION_STATUS.md`에 아래를 같이 남긴다.

- 완료된 API
- Swagger / Postman 테스트 통과 여부
- 현재 주의 사항
- 다음 우선순위 작업

즉:

- `README.md` = 왜 이렇게 만드는지와 학습 기준
- `docs/IMPLEMENTATION_STATUS.md` = 지금 어디까지 했는지와 다음에 뭘 할지

---

# 2. 왜 Codex용 문서를 시스템 프롬프트 / 체크리스트로 나눴는가

이전에 Codex용 문서를 두 개로 나눴다.

- `mind-compass_codex_system_prompt.md`
- `mind-compass_codex_task_checklist.md`

이렇게 나눈 이유는 역할이 다르기 때문이다.

## 2-1. 시스템 프롬프트 문서가 필요한 이유

시스템 프롬프트 문서는 **Codex의 머리속 기준**을 고정하는 문서다.

여기에는 이런 내용이 들어간다.

- 이 프로젝트가 무엇인지
- 왜 Spring Boot + FastAPI 2서버 구조인지
- Spring Boot와 FastAPI가 각각 뭘 맡는지
- MVP 우선순위가 무엇인지
- 멘탈헬스 서비스라서 왜 Safety Net이 중요한지
- Codex가 어떤 톤과 어떤 구조로 답해야 하는지

쉽게 말하면:

- 시스템 프롬프트 = **Codex의 사고방식 세팅 문서**

이 문서가 없으면 Codex는 매번 새 채팅마다
- 구조를 다르게 제안하거나
- 앱이 FastAPI를 직접 부르게 하거나
- 인증보다 AI 기능을 먼저 만들자고 하거나
- 너무 거대한 구조를 제안할 수 있다.

즉, 프로젝트의 중심축이 흔들릴 수 있다.

---

## 2-2. 체크리스트 문서가 필요한 이유

체크리스트 문서는 **실제로 Codex에게 일을 시킬 때 쓰는 작업 문서**다.

여기에는 이런 내용이 들어간다.

- 지금 어떤 순서로 개발해야 하는지
- 한 번에 뭘 요청해야 하는지
- 어떤 API부터 만들어야 하는지
- 각 단계에서 확인할 것이 뭔지
- 너무 큰 요청을 피하려면 어떻게 잘게 나눌지

쉽게 말하면:

- 체크리스트 = **Codex에게 일을 시키는 운영 문서**

시스템 프롬프트만 있으면 방향은 맞지만,
실제로 작업을 요청할 때 너무 크게 던지면 Codex가 멈추거나 흐려질 수 있다.

그래서 체크리스트 문서가 따로 필요하다.

---

## 2-3. 둘을 나눈 핵심 이유 한 줄

- 시스템 프롬프트: **왜 이렇게 만드는지 고정**
- 체크리스트: **무엇부터 어떻게 만들지 실행**

---

# 3. 이 프로젝트를 왜 Spring Boot + FastAPI 2서버로 구성하는가

이 프로젝트의 핵심 구조는 다음과 같다.

- 앱은 **Spring Boot만 호출**
- Spring Boot는 **정본 데이터와 업무 로직의 중심**
- FastAPI는 **AI 분석과 추론 담당**
- Spring Boot가 내부적으로 FastAPI를 호출

이렇게 나누는 이유는 명확하다.

## 3-1. Spring Boot가 맡아야 하는 것

Spring Boot는 아래처럼 **안정적이고 비즈니스 중심인 영역**을 맡는다.

- 회원가입
- 로그인
- JWT 인증/인가
- 일기 저장/조회/수정/삭제
- 감정 캘린더 조회
- 상담 세션 관리
- 채팅 메시지 저장
- 리포트 저장
- DB 트랜잭션 관리
- 외부 앱이 호출하는 공식 API

즉, Spring Boot는 **서비스의 중심 서버**다.

---

## 3-2. FastAPI가 맡아야 하는 것

FastAPI는 아래처럼 **자주 바뀌고 실험이 많은 AI 영역**을 맡는다.

- 감정 분석
- 위험 신호 점수 계산
- 상담 답변 초안 생성
- 임베딩 생성
- RAG 문맥 조립
- 추후 GraphRAG, 멀티모달 실험

즉, FastAPI는 **AI 두뇌 역할**이다.

---

## 3-3. 왜 앱이 FastAPI를 직접 부르면 안 되는가

앱이 FastAPI를 직접 호출하면 다음 문제가 생긴다.

- 인증 흐름이 분산된다.
- AI 서버가 외부에 직접 노출된다.
- 장애 포인트가 늘어난다.
- 앱에서 호출해야 할 API 진입점이 여러 개가 된다.
- 나중에 AI 구조가 바뀌면 앱도 같이 수정해야 한다.

그래서 앱은 **무조건 Spring Boot만 호출**하게 만든다.

이렇게 해야 앱 입장에서는 API 진입점이 하나라서 단순해지고,
AI 로직이 바뀌어도 앱은 거의 영향을 안 받는다.

---

# 3-4. 왜 MVP 첫 배치를 Auth / User / Diary로 자르는가

이 프로젝트를 처음부터 다시 시작할 때는 모든 도메인을 한 번에 만들지 않는다.
먼저 아래 3개만 안정적으로 세우는 것이 맞다.

- Auth
- User
- Diary

이렇게 자르는 이유는 다음과 같다.

## Auth를 먼저 만드는 이유

모든 데이터는 사용자 기준으로 저장된다.

- 일기는 누구 것인가?
- 채팅 세션은 누구 것인가?
- 월간 리포트는 누구 기준인가?

이 질문에 답하려면 먼저 사용자 식별과 인증이 있어야 한다.
그래서 회원가입, 로그인, 토큰 재발급이 가장 먼저 온다.

## User를 바로 붙이는 이유

로그인이 성공했다는 것과,
실제로 보호 API에서 현재 로그인 사용자를 읽을 수 있다는 것은 다르다.

그래서 `GET /api/v1/users/me` 같은 API를 바로 붙여서 아래를 확인해야 한다.

- JWT 필터가 동작하는가
- Security Context에 사용자 ID가 들어가는가
- DB에서 내 사용자 정보와 설정을 읽을 수 있는가

즉 User API는 “내 인증이 실제 서비스 흐름에서 살아 있는지” 확인하는 첫 보호 API다.

## Diary를 첫 배치 마지막으로 두는 이유

이 서비스의 핵심 가치는 AI 상담보다 먼저 감정 기록 저장이다.

상담 AI는 나중에 고도화해도 되지만,
일기 저장이 흔들리면 서비스의 중심 가치가 무너진다.

그래서 첫 배치의 Diary는 아래 원칙으로 간다.

- 일기 저장은 반드시 안정적으로 동작해야 한다
- AI 분석이 없어도 CRUD는 완성돼야 한다
- Diary 저장 성공이 ai-api 성공에 의존하면 안 된다

즉 첫 배치는 “사용자 식별”과 “핵심 기록 저장”을 먼저 완성하는 단계다.

## 왜 Calendar / Chat / AI를 뒤로 미루는가

Calendar는 Diary가 있어야 의미가 있고,
Chat은 Auth와 Diary 이후에 붙여야 사용자 기준 대화 흐름이 맞으며,
AI는 저장 구조와 공개 API 경계가 안정된 뒤 붙여야 장애 전파를 막을 수 있다.

그래서 MVP 구현 순서는 다음처럼 간다.

1. Spring Boot 공통 기반
2. Auth
3. User
4. Diary
5. Calendar
6. Minimal ai-api
7. Chat
8. Safety / Report / Advanced AI

이 순서를 지키면
“AI가 잠시 실패해도 기록과 인증은 살아 있는 구조”를 먼저 확보할 수 있다.

---

# 4. 백엔드에서 자주 헷갈리는 개념을 먼저 아주 쉽게 설명

백엔드에서 API 하나가 만들어질 때 보통 이런 층이 있다.

- Controller
- Service
- Repository
- Entity
- DTO

이걸 아주 쉽게 설명하면 아래와 같다.

---

## 4-1. Controller는 “문을 여는 곳”

Controller는 외부 요청이 **제일 먼저 들어오는 입구**다.

예를 들어 앱에서 이런 요청이 왔다고 하자.

`POST /api/v1/diaries`

그 요청을 제일 먼저 받는 파일이 보통 Controller다.

예:
`DiaryController.java`

Controller의 역할은 보통 이 정도다.

- URL 받기
- HTTP method 받기 (GET, POST, PATCH, DELETE)
- 요청 body 받기
- path variable, query parameter 받기
- 요청을 Service로 넘기기
- Service 결과를 응답으로 감싸서 내려주기

Controller는 **복잡한 비즈니스 판단을 많이 하지 않는 것이 좋다.**

쉽게 말하면:

- Controller = **접수창구**

---

## 4-2. Service는 “실제 판단과 처리의 중심”

Service는 API의 핵심 로직이 들어가는 곳이다.

예:
- 이 사용자가 이 일기를 쓸 수 있는가?
- 감정 태그를 어떻게 저장할 것인가?
- 먼저 일기를 저장한 뒤 AI 분석을 부를 것인가?
- AI 실패 시에도 저장은 성공으로 볼 것인가?

이런 판단이 Service에 들어간다.

즉, Service는 단순 전달자가 아니라,
**업무 규칙과 흐름을 결정하는 핵심 계층**이다.

쉽게 말하면:

- Service = **실무 담당자 / 팀장**

---

## 4-3. Repository는 “DB와 대화하는 곳”

Repository는 데이터베이스에 접근하는 계층이다.

예:
- user를 저장
- diary를 조회
- 특정 날짜의 diary 목록을 조회
- 월별 감정 통계를 조회

Repository는 보통 JPA Repository 또는 QueryDSL/SQL과 연결된다.

쉽게 말하면:

- Repository = **DB 창구 담당자**

Repository는 “비즈니스 판단”보다
**데이터를 저장하고 읽는 역할**에 집중하는 것이 좋다.

---

## 4-4. Entity는 “DB에 저장되는 객체”

Entity는 테이블과 연결되는 자바 객체다.

예:
- `User`
- `Diary`
- `ChatSession`
- `ChatMessage`

보통 Entity는 DB 스키마와 꽤 밀접하다.

쉽게 말하면:

- Entity = **DB에 실제로 저장될 데이터 구조**

---

## 4-5. DTO는 “API 입출력 전용 객체”

DTO는 요청과 응답을 주고받기 위한 객체다.

예:
- `CreateDiaryRequest`
- `DiaryDetailResponse`
- `LoginRequest`
- `LoginResponse`

왜 DTO가 필요할까?

Entity를 바로 외부에 노출하면
- 불필요한 필드가 같이 나가고
- 보안상 위험하고
- 나중에 응답 구조를 바꾸기 어렵다.

그래서 DTO를 쓴다.

쉽게 말하면:

- DTO = **API용 포장 박스**

---

## 4-6. 한 줄 요약

- Controller = 요청 받는 곳
- Service = 실제 로직 처리
- Repository = DB 접근
- Entity = DB 저장 구조
- DTO = 요청/응답 포맷

---

# 5. 외부에서 API 요청이 들어오면 실제로 어떤 순서로 실행되는가

이제 아주 중요한 핵심 흐름을 본다.

외부에서 API 요청이 들어오면 보통 아래 순서로 흘러간다.

## 기본 흐름

1. 앱이 HTTP 요청을 보낸다.
2. Spring Security / JWT 필터가 먼저 확인한다.
3. Controller가 요청을 받는다.
4. Controller가 Request DTO로 바인딩한다.
5. Service가 호출된다.
6. Service가 비즈니스 판단을 한다.
7. 필요하면 Repository를 호출한다.
8. Repository가 DB를 조회/저장한다.
9. Service가 결과를 조합한다.
10. 필요하면 외부 내부 API(FastAPI)를 호출한다.
11. Service가 Response DTO를 만든다.
12. Controller가 응답을 반환한다.
13. GlobalExceptionHandler가 에러를 공통 형식으로 처리할 수 있다.

---

## 5-1. 그림처럼 이해하면

### AI가 없는 일반 API 요청 흐름
앱  
→ Security Filter  
→ Controller  
→ Service  
→ Repository  
→ DB  
→ Service  
→ Controller  
→ 앱 응답

### AI가 포함된 API 요청 흐름
앱  
→ Security Filter  
→ Controller  
→ Service  
→ Repository(DB 저장/조회)  
→ Internal AI Client  
→ FastAPI  
→ Spring Boot Service  
→ Response DTO 조합  
→ Controller  
→ 앱 응답

---

# 6. 파일 기준으로 보면 어떤 순서로 실행되는가

예를 들어 일기 작성 API가 있다고 하자.

`POST /api/v1/diaries`

보통 실행 흐름은 이런 식이다.

1. `SecurityConfig.java`
2. `JwtAuthenticationFilter.java`
3. `DiaryController.java`
4. `CreateDiaryRequest.java`
5. `DiaryService.java`
6. `DiaryRepository.java`
7. `Diary.java`
8. `DiaryEmotion.java` 또는 관련 값 객체
9. `DiaryResponse.java`
10. `GlobalExceptionHandler.java` (에러가 날 경우)

AI 분석까지 붙는다면 여기에 추가로:

11. `AiAnalysisClient.java`
12. `AiAnalysisRequest.java`
13. `AiAnalysisResponse.java`
14. FastAPI의 `analyze_diary_router.py`
15. FastAPI의 `emotion_analysis_service.py`

즉, API 하나가 실행될 때
**항상 컨트롤러만 실행되는 것이 아니라, 여러 파일이 순차적으로 협력한다.**

---

# 7. API를 만들 때 왜 “이 API가 필요한가”를 먼저 적어야 하는가

백엔드 초보가 가장 많이 겪는 문제는 이거다.

- 엔드포인트를 먼저 만들고
- 나중에 왜 만들었는지를 생각한다.

그러면 API가 점점 늘어나면서
- 겹치는 기능이 생기고
- 책임이 섞이고
- URL 설계가 이상해지고
- 프론트가 쓰기 어려운 구조가 된다.

그래서 API를 만들 때는 항상 아래 순서가 좋다.

1. **왜 이 API가 필요한가**
2. **누가 호출하는가**
3. **어떤 화면에서 쓰는가**
4. **입력은 무엇인가**
5. **출력은 무엇인가**
6. **어떤 파일들이 실행되는가**
7. **DB에서 어떤 데이터가 읽히고 저장되는가**

이 순서를 문서화해 두면,
내가 나중에 다시 봐도 이해가 쉽고,
Codex에게도 정확하게 요청할 수 있다.

---

# 8. API 하나를 만들 때마다 남길 학습 템플릿

앞으로 API를 하나 만들 때마다 아래 형식으로 정리한다.

---

## API 학습 템플릿

### 1) API 이름
예: 일기 작성 API

### 2) 엔드포인트
예: `POST /api/v1/diaries`

### 3) 이 API가 왜 필요한가
예:
사용자가 하루의 감정을 기록하고 저장하기 위해 필요하다.  
이 서비스의 핵심은 상담 이전에 “기록”이 가능해야 하므로 MVP에서 매우 중요하다.

### 4) 누가 호출하는가
예:
모바일 앱의 일기 작성 화면

### 5) 입력값
예:
- diary content
- 대표 감정
- 감정 강도
- 작성일시

### 6) 출력값
예:
- diaryId
- 저장 결과
- 작성 시간
- 대표 감정

### 7) 관련 파일
예:
- `DiaryController.java`
- `DiaryService.java`
- `DiaryRepository.java`
- `Diary.java`
- `CreateDiaryRequest.java`
- `DiaryDetailResponse.java`

### 8) 실행 순서
예:
1. 앱이 `/api/v1/diaries`로 POST 요청
2. JWT 필터가 사용자 인증 확인
3. `DiaryController`가 요청 수신
4. `CreateDiaryRequest`로 body 파싱
5. `DiaryService#createDiary()` 호출
6. 서비스가 사용자 존재 여부/입력값 검증
7. `Diary` 엔티티 생성
8. `DiaryRepository.save()` 호출
9. 저장 결과를 `DiaryDetailResponse`로 변환
10. Controller가 응답 반환

### 9) DB 영향
예:
- `diary` 테이블 insert
- 필요 시 `diary_emotion` insert

### 10) 예외 상황
예:
- 로그인 안 된 사용자
- 존재하지 않는 사용자
- 필수 입력값 누락
- 잘못된 감정 강도 값

---

# 9. 실제 예시로 배우기

이제 진짜 자주 쓰게 될 API 몇 개를 예시로,
왜 만들었고 어떤 파일이 순서대로 실행되는지 설명한다.

---

# 10. 예시 1: 회원가입 API

## API
`POST /api/v1/auth/signup`

## 왜 필요한가
사용자가 서비스를 사용하려면 먼저 계정이 있어야 한다.  
이 앱은 감정일기, 상담 세션, 리포트가 모두 사용자 기준으로 저장되므로  
회원가입은 가장 먼저 필요한 기능이다.

## 누가 호출하는가
- 앱의 회원가입 화면

## 관련 파일 예시
- `auth/controller/AuthController.java`
- `auth/service/AuthService.java`
- `auth/repository/UserRepository.java`
- `auth/domain/User.java`
- `auth/dto/request/SignUpRequest.java`
- `auth/dto/response/SignUpResponse.java`
- `common/exception/GlobalExceptionHandler.java`

## 실행 순서
1. 앱이 `POST /api/v1/auth/signup` 요청을 보낸다.
2. `AuthController`가 요청을 받는다.
3. 요청 body가 `SignUpRequest` DTO로 변환된다.
4. `AuthController`가 `AuthService.signUp()`을 호출한다.
5. `AuthService`가 이메일 중복 여부를 확인한다.
6. `UserRepository.findByEmail()`이 DB를 조회한다.
7. 중복이 없으면 `User` 엔티티를 만든다.
8. 비밀번호를 암호화한다.
9. `UserRepository.save(user)`로 저장한다.
10. 저장된 결과를 `SignUpResponse` DTO로 변환한다.
11. Controller가 응답을 반환한다.
12. 예외가 생기면 `GlobalExceptionHandler`가 공통 에러 응답을 내려준다.

## 학습 포인트
여기서 중요한 것은 **Controller는 요청을 받고 Service에 넘기고, 실제 가입 판단은 Service가 한다**는 점이다.  
또한 **DB 조회/저장은 Repository가 담당한다**.

---

# 11. 예시 2: 로그인 API

## API
`POST /api/v1/auth/login`

## 왜 필요한가
로그인해야 이후의 일기 조회, 감정 캘린더, 채팅 세션이 모두 “내 데이터” 기준으로 동작할 수 있다.

## 관련 파일 예시
- `AuthController.java`
- `AuthService.java`
- `UserRepository.java`
- `PasswordEncoder`
- `JwtTokenProvider.java`
- `LoginRequest.java`
- `LoginResponse.java`

## 실행 순서
1. 앱이 이메일/비밀번호를 담아 로그인 요청을 보낸다.
2. `AuthController`가 `LoginRequest`로 요청을 받는다.
3. `AuthService.login()`이 호출된다.
4. `UserRepository.findByEmail()`로 사용자를 찾는다.
5. `PasswordEncoder`로 비밀번호를 검증한다.
6. 맞다면 `JwtTokenProvider`가 access token과 refresh token을 만든다.
7. 필요하면 refresh token을 저장한다.
8. `LoginResponse`를 만든다.
9. Controller가 토큰 정보를 응답한다.

## 학습 포인트
로그인은 “사용자를 저장하는 API”가 아니라  
**사용자를 검증하고 토큰을 발급하는 API**라는 점이 핵심이다.

---

# 12. 예시 3: 일기 작성 API

## API
`POST /api/v1/diaries`

## 왜 필요한가
이 서비스의 가장 핵심 기능은 감정 기록이다.  
상담 AI가 잠시 실패해도 일기 기록은 가능해야 한다.  
그래서 일기 작성 API는 MVP에서 매우 중요하다.

## 누가 호출하는가
- 앱의 일기 작성 화면
- 음성 입력을 텍스트로 바꾼 뒤 저장하는 흐름(추후)

## 관련 파일 예시
- `diary/controller/DiaryController.java`
- `diary/service/DiaryService.java`
- `diary/repository/DiaryRepository.java`
- `diary/domain/Diary.java`
- `diary/domain/DiaryEmotion.java`
- `diary/dto/request/CreateDiaryRequest.java`
- `diary/dto/response/DiaryDetailResponse.java`

## 실행 순서
1. 앱이 일기 내용과 대표 감정, 감정 강도를 담아 요청한다.
2. JWT 필터가 로그인 여부를 먼저 확인한다.
3. `DiaryController`가 요청을 받는다.
4. body가 `CreateDiaryRequest` DTO로 변환된다.
5. `DiaryService.createDiary()`가 호출된다.
6. 서비스가 사용자 ID를 확인한다.
7. 입력값을 검증한다.
8. `Diary` 엔티티를 생성한다.
9. `DiaryRepository.save()`로 DB에 저장한다.
10. 저장 결과를 `DiaryDetailResponse`로 변환한다.
11. Controller가 앱에 응답을 내려준다.

## 학습 포인트
여기서 중요한 것은  
**“요청을 받는 것”과 “실제로 저장하는 것”이 분리되어 있다는 점**이다.

- 요청 받기: Controller
- 저장 판단: Service
- DB 저장: Repository

---

# 13. 예시 4: 월간 감정 캘린더 조회 API

## API
`GET /api/v1/calendar/monthly?year=2026&month=3`

## 왜 필요한가
앱의 메인 화면이 캘린더 기반이라면,
사용자는 한 달 동안 내가 어떤 감정으로 지냈는지를 한눈에 보고 싶어 한다.  
그래서 CRUD 다음으로 중요한 것이 “조회 최적화 API”다.

## 관련 파일 예시
- `calendar/controller/CalendarController.java`
- `calendar/service/CalendarQueryService.java`
- `diary/repository/DiaryQueryRepository.java`
- `calendar/dto/response/MonthlyCalendarResponse.java`

## 실행 순서
1. 앱이 연/월을 query parameter로 담아 요청한다.
2. JWT 필터가 사용자 인증을 확인한다.
3. `CalendarController`가 요청을 받는다.
4. `CalendarQueryService.getMonthlyCalendar()`가 호출된다.
5. 서비스가 시작일/종료일을 계산한다.
6. `DiaryQueryRepository`가 해당 월의 diary 데이터를 조회한다.
7. 서비스가 날짜별 대표 감정, 감정 강도, 작성 여부를 가공한다.
8. `MonthlyCalendarResponse` DTO를 만든다.
9. Controller가 응답을 반환한다.

## 학습 포인트
조회 API에서는 단순 CRUD보다  
**응답 화면에 맞는 데이터 가공**이 중요하다.

즉, “DB 테이블 그대로 응답”하는 게 아니라  
**캘린더 화면이 바로 쓸 수 있는 형태**로 Service가 변환해 줘야 한다.

---

# 14. 예시 5: 채팅 메시지 전송 API (AI 포함)

## API
`POST /api/v1/chat/sessions/{sessionId}/messages`

## 왜 필요한가
이 API는 사용자가 AI 상담과 실제로 대화하는 핵심 진입점이다.  
단순 저장이 아니라, 메시지 저장 + AI 호출 + 응답 저장이 연결될 수 있다.

## 관련 파일 예시 (Spring Boot)
- `chat/controller/ChatController.java`
- `chat/service/ChatService.java`
- `chat/repository/ChatSessionRepository.java`
- `chat/repository/ChatMessageRepository.java`
- `chat/domain/ChatSession.java`
- `chat/domain/ChatMessage.java`
- `chat/client/AiChatClient.java`
- `chat/dto/request/SendChatMessageRequest.java`
- `chat/dto/response/SendChatMessageResponse.java`

## 관련 파일 예시 (FastAPI)
- `app/routers/chat_router.py`
- `app/services/reply_generation_service.py`
- `app/schemas/generate_reply_request.py`
- `app/schemas/generate_reply_response.py`

## 실행 순서
1. 앱이 `sessionId`와 사용자 메시지를 담아 요청한다.
2. JWT 필터가 인증을 확인한다.
3. `ChatController`가 요청을 받는다.
4. `SendChatMessageRequest`로 body를 파싱한다.
5. `ChatService.sendMessage()`가 호출된다.
6. 서비스가 해당 세션이 존재하는지 확인한다.
7. 사용자의 메시지를 `ChatMessage`로 저장한다.
8. 최근 대화 기록을 조회한다.
9. `AiChatClient`가 FastAPI 내부 API를 호출한다.
10. FastAPI의 router가 요청을 받는다.
11. FastAPI service가 감정/문맥/안전 조건을 고려해 답변 초안을 만든다.
12. FastAPI가 응답을 반환한다.
13. Spring Boot의 `ChatService`가 AI 응답을 다시 `ChatMessage` 또는 `AIResponseLog`로 저장한다.
14. 최종 응답 DTO를 조합한다.
15. Controller가 앱에 응답을 반환한다.

## 학습 포인트
이 API는 일반 CRUD와 다르게  
**DB 저장 + 내부 AI 호출 + 응답 저장**이 연속으로 일어난다.

즉, 이 API를 이해하면
- Spring Boot와 FastAPI의 역할 분리
- 왜 앱이 FastAPI를 직접 호출하지 않는지
- 왜 Service 계층이 중요한지

를 한 번에 이해할 수 있다.

---

# 15. AI가 포함된 API에서는 왜 Service가 더 중요해지는가

AI가 포함되면 Service의 역할이 훨씬 중요해진다.

왜냐하면 Service가 다음을 판단해야 하기 때문이다.

- 먼저 DB에 저장할지
- 먼저 AI를 호출할지
- AI가 실패하면 어떻게 처리할지
- 위험 신호가 있으면 일반 답변 대신 Safety 응답으로 바꿀지
- 응답을 DB에 어떤 형식으로 남길지

즉, AI API에서는 Service가 단순한 중계자가 아니라  
**전체 흐름을 설계하는 지휘자**가 된다.

---

# 16. API를 만들 때 어떤 파일들이 생기는지 기본 패턴

Spring Boot에서 API 하나를 만든다고 하면 보통 아래 파일들이 생긴다.

예: diary 작성 API

- `DiaryController.java`
- `DiaryService.java`
- `DiaryRepository.java`
- `Diary.java`
- `CreateDiaryRequest.java`
- `DiaryDetailResponse.java`

조금 더 복잡하면 추가로 생긴다.

- `DiaryMapper.java`
- `DiaryQueryRepository.java`
- `DiaryValidator.java`
- `DiaryException.java`

즉, API 하나는 보통 “컨트롤러 파일 하나”로 끝나는 게 아니라  
**입력 DTO, 출력 DTO, 서비스, 저장소, 엔티티**가 같이 생기는 구조다.

---

# 17. 내가 이 구조를 이해할 때 꼭 기억할 핵심 문장

## 핵심 문장 1
**Controller는 입구이고, Service는 판단의 중심이며, Repository는 DB 창구다.**

## 핵심 문장 2
**API 하나를 만든다는 것은 URL 하나 만드는 것이 아니라, 요청부터 저장/조회/응답까지의 흐름을 설계하는 것이다.**

## 핵심 문장 3
**멘탈헬스 앱에서는 AI 답변보다 저장 안정성과 안전 분기가 더 중요하다.**

## 핵심 문장 4
**Spring Boot는 서비스의 중심이고, FastAPI는 내부 AI 엔진이다.**

---

# 18. Codex에게 API를 만들라고 시킬 때 반드시 요구할 것

앞으로 Codex에게 API를 시킬 때는 단순히
“이 API 만들어줘”
라고 하지 않는다.

반드시 아래까지 같이 요구한다.

## Codex 요청 필수 조건

1. 이 API가 왜 필요한지 설명
2. 어느 화면/기능에서 쓰는지 설명
3. 관련 파일 목록 제시
4. 요청이 들어왔을 때 실행 순서를 단계별로 설명
5. DTO / Entity / Service / Repository 역할 구분 설명
6. 예외 상황 설명
7. DB에 어떤 변화가 생기는지 설명

---

## Codex 요청 예시

```txt
POST /api/v1/diaries API를 설계해줘.

아래를 반드시 포함해줘.
1. 이 API가 왜 필요한지
2. 어떤 화면에서 호출하는지
3. 관련 파일 목록
4. 외부 요청이 들어왔을 때 어떤 파일이 어떤 순서로 실행되는지
5. Controller / Service / Repository / Entity / DTO 역할 설명
6. 예외 상황
7. DB insert/update 영향
8. Spring Boot 기준 코드 골격
```

---

# 19. 앞으로 README를 이렇게 계속 확장하면 좋은 이유

이 문서를 계속 업데이트하면 좋은 점이 많다.

- 나중에 내가 다시 봐도 이해가 쉽다.
- 면접에서 “왜 이렇게 설계했는지” 설명하기 쉬워진다.
- Codex 결과물이 흔들릴 때 기준점이 된다.
- 새로운 API를 추가할 때 일관성을 유지할 수 있다.
- 단순 복붙 개발이 아니라, 구조를 이해하면서 개발할 수 있다.

즉, 이 README는 그냥 설명서가 아니라  
**내가 백엔드를 학습하고 프로젝트를 통제하기 위한 기준 문서**다.

---

# 20. 이 프로젝트에서 가장 먼저 이해해야 하는 우선순위

이 프로젝트의 우선순위는 아래와 같다.

1. Spring Boot 공통 기반
2. 회원/인증
3. 감정일기 CRUD
4. 캘린더/감정 조회
5. FastAPI 최소 AI 분석
6. 상담 세션/메시지
7. Safety Net
8. 통계/리포트
9. 고도화 AI

이 순서를 왜 지키냐면,

- 상담 AI가 없어도 감정일기는 써져야 하고
- AI가 불안정해도 서비스는 살아 있어야 하며
- 고도화보다 저장/조회/안전성이 먼저이기 때문이다.

---

# 21. 마지막 정리

이 README의 핵심 목적은 하나다.

**“API를 만들 때마다, 왜 만들었는지와 요청 흐름을 내가 이해할 수 있게 남기는 것”**

앞으로 이 프로젝트에서 새 API를 만들 때는  
반드시 아래 3가지를 같이 남긴다.

1. 왜 필요한 API인가  
2. 어떤 파일이 관련되는가  
3. 외부 요청이 들어왔을 때 어떤 순서로 실행되는가  

이 습관이 생기면,
단순히 AI가 짜준 코드를 쓰는 것이 아니라  
**내가 구조를 이해하면서 백엔드를 만들 수 있게 된다.**

---

# 22. 현재 먼저 구현한 것: 회원가입 / 로그인 API

이번 단계에서는 `backend-api`에서 가장 먼저 `회원가입`과 `로그인`을 구현한다.  
이유는 아주 단순하다.

- 일기는 누구 것이냐?
- 채팅 세션은 누구 것이냐?
- 감정 캘린더는 누구 기준이냐?

이 질문에 답하려면 먼저 **사용자를 식별하는 기준**이 있어야 한다.  
그래서 Diary, Calendar, Chat보다 먼저 Auth를 구현한다.

또한 이번 구현은 업로드한 가입/로그인 화면과 ERD를 기준으로 잡는다.

- 가입 화면: `닉네임`, `이메일`, `비밀번호`, `필수 약관 동의`
- 로그인 화면: `이메일`, `비밀번호`
- ERD 기준 핵심 테이블: `users`, `user_settings`, `refresh_tokens`

## 22-1. 왜 이렇게 만들었는가

### 회원가입 API를 먼저 만든 이유
회원가입은 서비스에 들어오는 사용자를 처음 생성하는 API다.  
이 단계에서 해야 하는 핵심은 다음과 같다.

- 이메일 중복 검사
- 비밀번호 암호화
- `users` 테이블 저장
- 기본 `user_settings` 생성

즉, 단순히 폼 값을 저장하는 것이 아니라  
**이후 모든 도메인이 의존할 사용자 기준점을 만드는 작업**이다.

### 로그인 API를 바로 같이 만든 이유
회원가입만 있으면 사용자를 만들 수는 있지만,  
앱이 이후 보호 API를 호출할 수는 없다.

로그인은 다음을 담당한다.

- 이메일/비밀번호 검증
- JWT access token 발급
- refresh token 발급 및 저장

즉, 로그인은 “사용자를 만드는 API”가 아니라  
**이미 존재하는 사용자를 검증하고, 다음 요청에 사용할 인증 수단을 발급하는 API**다.

## 22-2. 관련 파일

### Controller
- `backend-api/src/main/java/com/mindcompass/api/auth/controller/AuthController.java`

### Service
- `backend-api/src/main/java/com/mindcompass/api/auth/service/AuthService.java`

### Repository
- `backend-api/src/main/java/com/mindcompass/api/auth/repository/UserRepository.java`
- `backend-api/src/main/java/com/mindcompass/api/auth/repository/UserSettingsRepository.java`
- `backend-api/src/main/java/com/mindcompass/api/auth/repository/RefreshTokenRepository.java`

### Entity
- `backend-api/src/main/java/com/mindcompass/api/auth/domain/User.java`
- `backend-api/src/main/java/com/mindcompass/api/auth/domain/UserSettings.java`
- `backend-api/src/main/java/com/mindcompass/api/auth/domain/RefreshToken.java`

### DTO
- `backend-api/src/main/java/com/mindcompass/api/auth/dto/request/SignUpRequest.java`
- `backend-api/src/main/java/com/mindcompass/api/auth/dto/request/LoginRequest.java`
- `backend-api/src/main/java/com/mindcompass/api/auth/dto/response/SignUpResponse.java`
- `backend-api/src/main/java/com/mindcompass/api/auth/dto/response/LoginResponse.java`

### Security
- `backend-api/src/main/java/com/mindcompass/api/common/config/SecurityConfig.java`
- `backend-api/src/main/java/com/mindcompass/api/auth/security/JwtAuthenticationFilter.java`
- `backend-api/src/main/java/com/mindcompass/api/auth/security/JwtTokenProvider.java`

## 22-3. 회원가입 API가 실행되는 실제 순서

엔드포인트: `POST /api/v1/auth/signup`

1. 앱의 회원가입 화면이 닉네임, 이메일, 비밀번호, 약관 동의 값을 보낸다.
2. 이 URL은 공개 API이므로 JWT 필터를 꼭 통과해야 하는 보호 API는 아니다.
3. `AuthController.signup()`이 요청을 받는다.
4. 요청 body가 `SignUpRequest` DTO로 변환된다.
5. DTO 단계에서 형식 검증이 먼저 일어난다.
   - 이메일 형식이 맞는지
   - 비밀번호 길이가 맞는지
   - 필수 약관 동의가 true인지
6. Controller는 직접 DB를 건드리지 않고 `AuthService.signup()`에 일을 넘긴다.
7. `AuthService`가 이메일을 정규화하고 중복 여부를 판단한다.
8. `UserRepository.findByEmail()`이 DB에서 이미 가입된 이메일이 있는지 조회한다.
9. 중복이 없으면 `PasswordEncoder`가 비밀번호를 암호화한다.
10. `User` 엔티티를 만든다.
11. 기본 설정용 `UserSettings` 엔티티를 함께 만든다.
12. `UserRepository.save()`가 `users` 테이블에 저장한다.
13. `UserSettingsRepository.save()`가 `user_settings` 테이블에 저장한다.
14. `AuthService`가 저장 결과를 `SignUpResponse` DTO로 바꾼다.
15. `AuthController`가 최종 응답을 앱에 돌려준다.

핵심은 이거다.

- Controller는 요청을 받는다.
- Service는 판단한다.
- Repository는 DB와 대화한다.

## 22-4. 로그인 API가 실행되는 실제 순서

엔드포인트: `POST /api/v1/auth/login`

1. 앱의 로그인 화면이 이메일과 비밀번호를 보낸다.
2. `AuthController.login()`이 요청을 받는다.
3. body가 `LoginRequest` DTO로 변환된다.
4. Controller는 `AuthService.login()`을 호출한다.
5. `AuthService`가 이메일 기준으로 사용자를 찾는다.
6. `UserRepository.findByEmail()`이 `users` 테이블을 조회한다.
7. 사용자가 없거나 탈퇴/비활성 상태면 로그인 실패로 처리한다.
8. 사용자가 있으면 `PasswordEncoder.matches()`로 입력 비밀번호와 저장된 암호화 비밀번호를 비교한다.
9. 검증에 성공하면 `JwtTokenProvider`가 access token과 refresh token을 만든다.
10. refresh token 원문은 그대로 저장하지 않고 해시값으로 바꾼다.
11. 기존 활성 refresh token을 정리한 뒤 `RefreshTokenRepository.save()`로 저장한다.
12. `AuthService`가 토큰과 사용자 요약 정보를 `LoginResponse` DTO로 만든다.
13. `AuthController`가 최종 응답을 반환한다.

여기서 중요한 차이는 다음과 같다.

- 회원가입은 사용자를 만든다.
- 로그인은 사용자를 검증한다.
- 로그인 성공 결과물은 사용자 정보 자체보다 `토큰 발급`이 더 중요하다.

## 22-5. 각 계층을 아주 쉽게 다시 보면

### Controller
입구다.  
HTTP 요청을 받고, DTO로 받고, Service를 호출하고, 응답을 돌려준다.

### Service
판단의 중심이다.  
중복 가입인지, 비밀번호가 맞는지, 어떤 엔티티를 저장할지, 어떤 토큰을 발급할지 결정한다.

### Repository
DB 전담 창구다.  
이메일로 사용자 조회, 사용자 저장, 설정 저장, refresh token 저장을 담당한다.

### Entity
DB에 실제 저장되는 구조다.  
`User`, `UserSettings`, `RefreshToken`이 여기에 해당한다.

### DTO
외부와 주고받는 포장 형식이다.  
`SignUpRequest`, `LoginRequest`, `SignUpResponse`, `LoginResponse`가 여기에 해당한다.

## 22-6. DB 영향

### 회원가입
- `users` insert
- `user_settings` insert

### 로그인
- `users` 조회
- `refresh_tokens` insert
- 필요 시 기존 활성 토큰 revoke update

## 22-7. 예외 상황

### 회원가입
- 이미 존재하는 이메일
- 잘못된 이메일 형식
- 비밀번호 길이 부족
- 필수 약관 동의 누락

### 로그인
- 존재하지 않는 이메일
- 비밀번호 불일치
- 비활성 사용자
- 탈퇴 사용자

## 22-8. 내가 이 구현에서 꼭 이해해야 하는 핵심 문장

- 회원가입은 사용자 기준점을 만드는 API다.
- 로그인은 사용자 검증과 토큰 발급 API다.
- Controller는 입구, Service는 판단, Repository는 DB 창구다.
- Auth가 먼저 안정되어야 Diary, Calendar, Chat이 사용자 기준으로 붙는다.

---

# 23. 다음으로 구현한 것: 토큰 재발급 / 내 정보 조회

회원가입과 로그인 다음에는 바로 아래 두 API가 붙는 것이 자연스럽다.

- `POST /api/v1/auth/refresh`
- `GET /api/v1/users/me`

이유는 간단하다.

- 로그인만 있으면 토큰을 한 번 받을 수는 있다.
- 하지만 access token이 만료되면 앱은 다시 쓸 수 없게 된다.
- 그리고 로그인 성공 후 “지금 내가 누구로 로그인돼 있는지” 확인할 API도 필요하다.

즉:

- `refresh`는 로그인 상태를 이어주는 API다.
- `me`는 인증이 실제로 동작하는지 확인하는 API다.

## 23-1. 토큰 재발급 API를 왜 만들었는가

access token은 보통 짧게 쓰는 토큰이다.  
그래서 앱을 오래 켜두면 access token이 만료될 수 있다.

이때 사용자가 매번 로그인 화면으로 돌아가게 만들면 UX가 나빠진다.  
그래서 refresh token으로 새 access token을 다시 받는 API가 필요하다.

이번 구현에서는 refresh token을 그냥 믿지 않고:

- JWT 서명/만료를 먼저 검증하고
- DB에 저장된 refresh token 해시와 일치하는지 다시 확인하고
- 기존 refresh token은 revoke 처리하고
- 새 refresh token으로 교체한다

즉 단순 재발급이 아니라 **rotation 정책**으로 구현했다.

## 23-2. 내 정보 조회 API를 왜 만들었는가

로그인 응답만으로는 “현재 인증이 계속 살아 있는지”를 매번 확인하기 어렵다.  
앱 진입 후 아래 화면들은 보통 내 정보 조회를 먼저 쓴다.

- 마이페이지
- 설정 화면
- 앱 시작 직후 자동 로그인 확인

그래서 `GET /api/v1/users/me`를 만들었다.

이 API가 성공하면 아래가 증명된다.

- access token이 유효하다
- JWT 필터가 정상 동작한다
- Security Context에서 사용자 ID를 꺼낼 수 있다
- DB에서 내 사용자 정보와 설정을 읽을 수 있다

## 23-3. 토큰 재발급 API 실행 순서

엔드포인트: `POST /api/v1/auth/refresh`

1. 앱이 저장해 둔 refresh token을 body에 담아 보낸다.
2. 이 API는 공개 진입점이지만 body 안의 refresh token을 기준으로 인증을 다시 확인한다.
3. `AuthController.refresh()`가 요청을 받는다.
4. JSON body가 `RefreshTokenRequest` DTO로 변환된다.
5. `AuthService.refresh()`가 호출된다.
6. Service가 `JwtTokenProvider.validateToken()`으로 refresh token 서명과 만료를 먼저 확인한다.
7. Service가 token 안에서 `userId`를 꺼낸다.
8. raw refresh token을 그대로 비교하지 않고 SHA-256 해시로 바꾼다.
9. `RefreshTokenRepository.findByUserIdAndTokenHashAndRevokedAtIsNull()`로 DB에 저장된 활성 토큰과 일치하는지 확인한다.
10. 해당 토큰이 있으면 기존 토큰은 revoke 처리한다.
11. 새 access token과 새 refresh token을 다시 발급한다.
12. 새 refresh token 해시를 `refresh_tokens` 테이블에 저장한다.
13. `TokenRefreshResponse` DTO를 만들어 반환한다.

핵심은 이거다.

- JWT 검증만 하지 않는다.
- DB에 저장된 토큰과도 맞는지 본다.
- 이전 토큰은 폐기하고 새 토큰으로 교체한다.

## 23-4. 내 정보 조회 API 실행 순서

엔드포인트: `GET /api/v1/users/me`

1. 앱이 `Authorization: Bearer {accessToken}` 헤더를 담아 요청한다.
2. `JwtAuthenticationFilter`가 Controller보다 먼저 실행된다.
3. 필터가 access token 서명과 만료를 검증한다.
4. 정상이면 token 안의 `userId`를 꺼내 Security Context에 넣는다.
5. 그 다음에 `UserController.getMe()`가 호출된다.
6. Controller는 `@AuthenticationPrincipal`로 현재 인증된 `userId`를 받는다.
7. `UserService.getMe(userId)`를 호출한다.
8. `UserRepository.findById()`가 `users` 테이블을 조회한다.
9. `UserSettingsRepository.findByUserId()`가 `user_settings` 테이블을 조회한다.
10. Service가 사용자 정보와 설정을 `UserMeResponse`로 묶는다.
11. Controller가 최종 응답을 반환한다.

여기서 꼭 이해해야 하는 포인트는:

- `me` API는 body에 userId를 받지 않는다.
- userId는 JWT에서 꺼낸다.
- 그래서 다른 사람 ID를 억지로 넣어 조회하는 방식이 아니다.

## 23-5. 관련 파일

### 토큰 재발급
- `backend-api/src/main/java/com/mindcompass/api/auth/controller/AuthController.java`
- `backend-api/src/main/java/com/mindcompass/api/auth/service/AuthService.java`
- `backend-api/src/main/java/com/mindcompass/api/auth/dto/request/RefreshTokenRequest.java`
- `backend-api/src/main/java/com/mindcompass/api/auth/dto/response/TokenRefreshResponse.java`
- `backend-api/src/main/java/com/mindcompass/api/auth/repository/RefreshTokenRepository.java`
- `backend-api/src/main/java/com/mindcompass/api/auth/security/JwtTokenProvider.java`

### 내 정보 조회
- `backend-api/src/main/java/com/mindcompass/api/user/controller/UserController.java`
- `backend-api/src/main/java/com/mindcompass/api/user/service/UserService.java`
- `backend-api/src/main/java/com/mindcompass/api/user/dto/response/UserMeResponse.java`
- `backend-api/src/main/java/com/mindcompass/api/auth/repository/UserRepository.java`
- `backend-api/src/main/java/com/mindcompass/api/auth/repository/UserSettingsRepository.java`
- `backend-api/src/main/java/com/mindcompass/api/auth/security/JwtAuthenticationFilter.java`

## 23-6. DB 영향

### 토큰 재발급
- `refresh_tokens` 기존 활성 토큰 revoke update
- `refresh_tokens` 새 토큰 insert

### 내 정보 조회
- `users` select
- `user_settings` select

## 23-7. 예외 상황

### 토큰 재발급
- refresh token 서명 불일치
- refresh token 만료
- DB에 저장된 활성 refresh token과 불일치
- 탈퇴/비활성 사용자

### 내 정보 조회
- access token 만료
- access token 서명 오류
- JWT는 통과했지만 DB에 사용자가 없음
- 사용자 설정 데이터 없음
