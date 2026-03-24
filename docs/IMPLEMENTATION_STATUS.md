# Mind Compass 援ы쁽 ?곹깭

??臾몄꽌????梨꾪똿?먯꽌???꾩옱 援ы쁽 ?곹깭瑜?鍮좊Ⅴ寃??댁뼱媛湲??꾪븳 湲곗? 臾몄꽌??
臾몄꽌 ?몄퐫?⑹? UTF-8 湲곗??쇰줈 ?좎??쒕떎.

## ?꾩옱 湲곗?
- 湲곗? ?좎쭨: 2026-03-24
- ?꾩옱 ?④퀎 ?먮떒: MVP 二쇱슂 諛깆뿏??湲곕뒫 援ы쁽 ?꾨즺, ??紐⑤컮???곕룞怨??댁쁺 ?덉젙???④퀎 吏꾩엯
- ?꾩껜 援ъ“: `backend-api`??Spring Boot 怨듦컻 API, `ai-api`??Spring AI ?대? AI ?쒕쾭, `ai-api-fastapi`??FastAPI 鍮꾧탳 ?쒕쾭, ?대씪?댁뼵?몃뒗 `backend-api`留??몄텧

## ?꾩옱 ?꾨즺 踰붿쐞

### 1. backend-api
- Auth
  - `POST /api/v1/auth/signup`
  - `POST /api/v1/auth/login`
  - `POST /api/v1/auth/refresh`
- User
  - `GET /api/v1/users/me`
- Diary
  - `POST /api/v1/diaries`
  - `GET /api/v1/diaries/{diaryId}`
  - `PATCH /api/v1/diaries/{diaryId}`
  - `DELETE /api/v1/diaries/{diaryId}`
  - `GET /api/v1/diaries?date=YYYY-MM-DD`
- Calendar
  - `GET /api/v1/calendar/monthly-emotions?year=YYYY&month=MM`
  - `GET /api/v1/calendar/daily-summary?date=YYYY-MM-DD`
- Chat
  - `POST /api/v1/chat/sessions`
  - `GET /api/v1/chat/sessions`
  - `GET /api/v1/chat/sessions/{sessionId}`
  - `POST /api/v1/chat/sessions/{sessionId}/messages`
- Report
  - `GET /api/v1/reports/monthly-summary?year=YYYY&month=MM`
  - `GET /api/v1/reports/emotions/weekly`
  - `GET /api/v1/reports/risks/monthly?year=YYYY&month=MM`

### 2. ai-api
- Spring Boot 湲곕컲 鍮꾧탳 ?쒕쾭 怨④꺽 ?앹꽦
- `POST /internal/ai/analyze-diary`
- `POST /internal/ai/generate-reply`
- `POST /internal/ai/risk-score`
- `GET /health`

### 3. ai-api-fastapi
- `POST /internal/ai/analyze-diary`
- `POST /internal/ai/generate-reply`
- `POST /internal/ai/risk-score`
- `GET /health`

### 4. AI ?곌퀎 ?숈옉
- Diary ?????`analyze-diary`, `risk-score` ?꾩쿂由??곌껐 ?꾨즺
- Chat 硫붿떆吏 ?꾩넚 ??`risk-score` ?좏샇異???`NORMAL`, `SUPPORTIVE`, `SAFETY`, `FALLBACK` 遺꾧린 泥섎━ ?꾨즺
- AI ?ㅽ뙣 ?쒖뿉??Diary/Chat ?듭떖 ????먮쫫? ?좎??섎룄濡?fallback ?뺤콉 ?곸슜 ?꾨즺

### 5. 臾몄꽌??/ ?댁쁺 蹂닿컯
- API ?숈뒿 臾몄꽌 ?뺣━
  - `docs/AUTH_API_LEARNING.md`
  - `docs/DIARY_API_LEARNING.md`
  - `docs/CALENDAR_API_LEARNING.md`
  - `docs/CHAT_API_LEARNING.md`
  - `docs/REPORT_API_LEARNING.md`
- ?댁쁺 媛?대뱶 ?뺣━
  - `README.md`瑜?Spring AI 湲곗? 硫붿씤 臾몄꽌濡??꾪솚
  - `README_FASTAPI.md`濡?湲곗〈 FastAPI ?숈뒿 臾몄꽌 遺꾨━
  - `docs/OPERATIONS_GUIDE.md`
  - `docs/MOBILE_INTEGRATION_HANDOFF.md`
  - `docs/SCREEN_TO_API_MAPPING.md`
  - `docs/DB_TABLE_SPECIFICATION.md`
  - `docs/ai-api/SPRING_AI_INTEGRATION_STRATEGY.md`

### 6. web-app
- `Next.js + Tailwind CSS` 湲곕컲 ???대씪?댁뼵??援ъ“ 議댁옱
- `login`, `calendar`, `diary`, `chat`, `report` ?섏씠吏? 怨듯넻 ?덉씠?꾩썐 援ъ꽦 ?꾨즺
- `backend-api` ?몄텧??API ?대씪?댁뼵???뚯씪 議댁옱
- ?몄쬆 蹂듦뎄, 蹂댄샇 ?쇱슦?? ?좏겙 ????먮쫫 肄붾뱶 議댁옱

## ?꾩옱 寃利??곹깭

### backend-api
- ?쒕퉬???뚯뒪??
  - `AuthServiceTest`
  - `UserServiceTest`
  - `DiaryServiceTest`
  - `ChatServiceTest`
  - `ReportServiceTest`
  - `CalendarServiceTest`
- 而⑦듃濡ㅻ윭 ?뚯뒪??
  - `AuthControllerTest`
  - `UserControllerTest`
  - `DiaryControllerTest`
  - `ChatControllerTest`
  - `ReportControllerTest`
  - `CalendarControllerTest`
- 蹂댁븞 / ?뚯쑀沅??뚯뒪??
  - `SecurityIntegrationTest`
  - `OwnershipIntegrationTest`
- ?붾뱶?ъ뿏???뚯뒪??
  - `PublicApiAuthE2ETest`
  - `PublicApiUserE2ETest`
  - `PublicApiReportMonthlySummaryE2ETest`
  - `PublicApiReportWeeklyEmotionsE2ETest`
  - `PublicApiReportMonthlyRisksE2ETest`
  - `PublicApiChatE2ETest`
  - `PublicApiDiaryFallbackE2ETest`
  - `PublicApiChatFallbackE2ETest`
### ?꾩옱源뚯? ?뺤씤???듭떖 怨꾩빟
- Diary create
  - ????곗꽑
  - `analyze-diary` ?ㅽ뙣 fallback
  - `risk-score` ?ㅽ뙣 fallback
  - ??AI ?몄텧??紐⑤몢 ?ㅽ뙣?대룄 HTTP `201 Created` ?좎?
  - 而⑦듃濡ㅻ윭 ?덈꺼?먯꽌??AI ?꾨뱶媛 鍮꾩뼱 ?덈뒗 fallback ?묐떟 怨꾩빟 寃利?
- Chat send-message
  - ?ъ슜??硫붿떆吏 ????곗꽑
  - 怨좎쐞??`SAFETY`, 以묒쐞??`SUPPORTIVE`, ?쇰컲 `NORMAL`, ?μ븷 ??`FALLBACK`
  - ?대? AI ?ㅽ뙣 ?쒖뿉??HTTP `201 Created` ?좎?
- Calendar
  - ?붽컙 媛먯젙 罹섎┛???묐떟 怨꾩빟 寃利?
  - ?쇰퀎 媛먯젙 ?붿빟 ?묐떟 怨꾩빟 寃利?
  - ?섎せ???곗썡 ?낅젰 寃利?
- Report
  - ?붽컙 ?붿빟, 二쇨컙 媛먯젙 異붿씠, ?붽컙 ?꾪뿕??異붿씠??而⑦듃濡ㅻ윭 怨꾩빟 寃利?
  - E2E瑜??붽컙 ?붿빟 / 二쇨컙 媛먯젙 / ?붽컙 ?꾪뿕???뚯씪濡?遺꾨━???ㅽ뙣 吏?먯쓣 ??鍮⑤━ ?앸퀎 媛??

### Swagger / Postman
- Auth, User, Diary, Calendar, Chat, Report 二쇱슂 ?먮쫫 ?ㅽ샇異?寃利?湲곕줉 議댁옱
- Diary AI 諛섏쁺, Chat assistant ??? Safety/Supportive 遺꾧린, Report 議고쉶 ?묐떟源뚯? 臾몄꽌??寃利??꾨즺

### web-app
- `node_modules` ?ㅼ튂 ?꾨즺
- `.env.local` ?앹꽦 ?꾨즺
- `npm run build` ?ш?利??듦낵
- `npm run dev`???μ떆媛??ㅽ뻾 ?꾨줈?몄뒪 ?뱀꽦???뺤씤???ㅽ뻾 以???꾩븘??醫낅즺?먯?留? ?섏〈??鍮뚮뱶 湲곗??쇰줈??濡쒖뺄 遺??媛?ν븳 ?곹깭濡??먮떒

## ?꾩옱 由ъ뒪??/ 二쇱쓽 ?ы빆
- `backend-api`, `ai-api`, `ai-api-fastapi`???섍꼍蹂???섏〈?꾧? ?믩떎.
  - `JWT_SECRET`
  - `DB_PASSWORD`
  - `AI_API_BASE_URL`
  - `WEB_ALLOWED_ORIGINS`
  - `OPENAI_API_KEY`
- PowerShell ?ㅽ뻾 ?섍꼍怨?IntelliJ Run Configuration ?섍꼍蹂?섍? ?ㅻ? ???덉쑝誘濡??ㅽ뻾 寃쎈줈蹂??ㅼ젙 ?뺤씤???꾩슂?섎떎.
- `application.yaml` 湲곕낯 ?꾨왂? `ddl-auto: validate`?쇱꽌 migration ?꾨씫 ??濡쒖뺄 遺?낆씠 諛붾줈 留됲엺??
- ???꾨찓??異붽? ???뷀떚?곕쭔 留뚮뱾吏 留먭퀬 `db/migration`怨??ㅼ젣 DB ?뚯씠釉?諛섏쁺 ?щ?瑜?媛숈씠 ?뺤씤?댁빞 ?쒕떎.
- 臾몄꽌????鍮뚮뱶 ?깃났 湲곕줉???덉쑝?? ?꾩옱 濡쒖뺄 ?ш?利?寃곌낵???섏〈??誘몄꽕移??곹깭??洹몃?濡??좊ː?섎㈃ ???쒕떎.
- Diary / Chat AI ?꾩쿂由щ뒗 ?꾩옱 ?붿껌-?묐떟 ?덉뿉??`try/catch` fallback?쇰줈 蹂댄샇?섍퀬 ?덉쑝硫? ?꾩쭅 鍮꾨룞湲?遺꾨━???ъ떆???먭퉴吏???꾩엯?섏? ?딆븯??

## 理쒓렐 hardening 硫붾え

### 2026-03-24 diary create API hardening
- Completed API: `POST /api/v1/diaries`
- Verification status:
  - `DiaryServiceTest`媛 ?깃났, `analyze-diary` ?ㅽ뙣 fallback, `risk-score` ?ㅽ뙣 fallback??寃利?
  - `PublicApiFlowE2ETest`媛 ??AI ?몄텧??紐⑤몢 ?ㅽ뙣?대룄 `201 Created`媛 ?좎??섎뒗 寃껋쓣 寃利?
  - `DiaryControllerTest`媛 AI ?꾨뱶媛 ?녿뒗 fallback ?묐떟 吏곷젹?붾? 寃利?
- Confirmed behavior:
  - diary ??μ? AI 遺꾩꽍 ?ㅽ뙣 ?뚮Ц??濡ㅻ갚?섏? ?딅뒗??
  - ?묐떟? ??λ맂 diary ?곗씠??以묒떖?쇰줈 諛섑솚?섍퀬, ?깃났??AI ?꾨뱶留??좏깮?곸쑝濡??ы븿?쒕떎

### 2026-03-24 cross-domain hardening
- Completed API:
  - `POST /api/v1/chat/sessions/{sessionId}/messages`
  - `GET /api/v1/calendar/monthly-emotions`
  - `GET /api/v1/calendar/daily-summary`
- Verification status:
  - `ChatServiceTest`媛 AI orchestration ?ㅽ뙣 ??`FALLBACK` ?묐떟??寃利?
  - `PublicApiFlowE2ETest`媛 chat send-message?먯꽌 AI ?ㅽ뙣 ?쒖뿉??`201 Created`? `FALLBACK` ?묐떟??寃利?
  - `CalendarServiceTest`, `CalendarControllerTest`媛 ?붽컙/?쇰퀎 議고쉶 怨꾩빟??寃利?
- Confirmed behavior:
  - chat? ?ъ슜??硫붿떆吏瑜???ν븳 ??AI ?ㅽ뙣媛 ?섎룄 蹂댁“ ?묐떟?쇰줈 ?먮쫫???좎??쒕떎
  - calendar???붾㈃ 移쒗솕?곸씤 吏묎퀎 ?묐떟???덉젙?곸쑝濡?諛섑솚?쒕떎

### 2026-03-24 controller and report regression expansion
- Completed API:
  - `POST /api/v1/chat/sessions/{sessionId}/messages`
  - `GET /api/v1/reports/monthly-summary`
  - `GET /api/v1/reports/emotions/weekly`
  - `GET /api/v1/reports/risks/monthly`
- Verification status:
  - `ChatControllerTest`??`FALLBACK` ?묐떟 怨꾩빟 耳?댁뒪 異붽?
  - `PublicApiFlowE2ETest`?먯꽌 monthly summary肉??꾨땲??weekly emotions / monthly risks 議고쉶源뚯? ?ㅼ젣 ?ъ슜???먮쫫???ы븿
- Confirmed behavior:
  - chat fallback ?묐떟??而⑦듃濡ㅻ윭 ?덈꺼?먯꽌 ?덉젙?곸쑝濡?吏곷젹?붾맂??
  - report 議고쉶???듭떖 3媛??붾㈃ API媛 而⑦듃濡ㅻ윭 / E2E ?묒そ?먯꽌 怨꾩냽 ?뚭? 寃利앸맂??

### 2026-03-24 spring ai live runtime verification
- Completed change:
  - `ai-api`瑜?`java -jar`濡?8001 ?ы듃??湲곕룞
  - `backend-api`瑜?`AI_PROVIDER=spring-ai` 湲곕낯 ?꾨왂?쇰줈 ?ㅼ젣 ?ㅽ뻾
  - `signup -> login -> diary create` ?ㅼ젣 ?몄텧濡??곕룞 ?뺤씤
- Verification status:
  - `http://localhost:8001/health` ?묐떟 ?뺤씤
  - `http://localhost:8080/swagger-ui/index.html` 200 ?뺤씤
  - diary create ?ㅽ샇異???`riskLevel=LOW`, `riskScore=0.1`, `recommendedAction=NORMAL_RESPONSE` 諛섏쁺 ?뺤씤
- Confirmed behavior:
  - `backend-api`???ㅼ젣 ?고??꾩뿉??Spring AI ai-api(8001)濡??곌껐?????덈떎
  - ai-api 誘멸린???쒖뿉??fallback?쇰줈 ????깃났???좎??섍퀬, ai-api 湲곕룞 ?쒖뿉???꾪뿕???꾨뱶媛 ?ㅼ젣 諛섏쁺?쒕떎
### 2026-03-24 spring ai backend integration strategy
- Completed change:
  - `backend-api`??`app.ai.provider`, `spring-base-url`, `fastapi-base-url` ?ㅼ젙 遺꾨━
  - `AiEndpointProperties`濡?Spring AI / FastAPI ?꾪솚 洹쒖튃 ?뺣━
  - `SPRING_AI_INTEGRATION_STRATEGY.md` 臾몄꽌 異붽?
- Verification status:
  - `AiEndpointPropertiesTest`濡?provider蹂?base URL ?좏깮怨??덉쇅 耳?댁뒪 寃利?- Confirmed behavior:
  - 湲곕낯 provider??`spring-ai`?닿퀬, ?꾩슂???뚮쭔 ?ㅼ젙媛믪쑝濡?`fastapi`瑜??좏깮?????덈떎
### 2026-03-24 auth user E2E split
- Completed change:
  - Auth 怨듦컻 ?먮쫫??`PublicApiAuthE2ETest`濡?遺꾨━
  - User ???뺣낫 ?먮쫫??`PublicApiUserE2ETest`濡?遺꾨━
  - 怨듯넻 濡쒓렇??refresh token ?ъ궗?⑹쓣 ?꾪빐 `PublicApiE2ESupport` ?뺤옣
- Verification status:
  - Auth/User E2E 2媛쒖? 湲곗〈 split E2E 臾띠쓬 ?ъ떎???듦낵
- Confirmed behavior:
  - signup -> login -> refresh? login -> users/me ?먮쫫??蹂꾨룄 ?뚯씪 ?⑥쐞濡?諛붾줈 ?앸퀎 媛??
### 2026-03-24 report E2E split
- Completed change:
  - 湲곗〈 `PublicApiDiaryReportE2ETest`瑜?report 議고쉶 ?⑥쐞蹂??뚯씪濡?遺꾨━
  - `PublicApiReportMonthlySummaryE2ETest`
  - `PublicApiReportWeeklyEmotionsE2ETest`
  - `PublicApiReportMonthlyRisksE2ETest`
  - 怨듯넻 濡쒓렇???쇨린 ?앹꽦 ?ы띁 `PublicApiE2ESupport` 異붽?
- Verification status:
  - 遺꾨━??report E2E 3媛쒖? 湲곗〈 chat/diary fallback E2E ?ъ떎???듦낵
- Confirmed behavior:
  - report ?ㅽ뙣 ???붽컙 ?붿빟 / 二쇨컙 媛먯젙 / ?붽컙 ?꾪뿕??以??대뒓 ?먮쫫??源⑥죱?붿? 諛붾줈 ?앸퀎 媛??
### 2026-03-24 E2E scenario split
- Completed change:
  - 湲곗〈 `PublicApiFlowE2ETest`瑜?湲곕뒫蹂??쒕굹由ъ삤濡?遺꾨━
  - `PublicApiReportMonthlySummaryE2ETest`
  - `PublicApiReportWeeklyEmotionsE2ETest`
  - `PublicApiReportMonthlyRisksE2ETest`
  - `PublicApiChatE2ETest`
  - `PublicApiDiaryFallbackE2ETest`
  - `PublicApiChatFallbackE2ETest`
- Verification status:
  - 遺꾨━??E2E ?쒕굹由ъ삤蹂?Gradle ?뚯뒪???ъ떎???듦낵
- Confirmed behavior:
  - ?ㅽ뙣 ???대뒓 ?먮쫫??源⑥죱?붿? `Diary/Report`, `Chat`, `Diary fallback`, `Chat fallback` ?⑥쐞濡?諛붾줈 ?앸퀎 媛??

## 吏湲?諛붾줈 ?ㅼ쓬??????
1. `web-app` ?ㅽ뻾 ?섍꼍 ?ш?利?
2. `ai-api` ?댁쁺 ?먭? ?덉감 怨좎젙
   - `.env.local` ?먮뒗 ?ㅽ뻾 ?섍꼍 蹂?섏뿉 `NEXT_PUBLIC_BACKEND_API_BASE_URL` ?뺤씤
   - `npm run build`, `npm run dev` ?ш?利?
   - ?ㅼ젣 ?붾㈃蹂??섎룞 ?먭? ?먮쫫 吏꾪뻾
3. ?꾩슂 ??`backend-api`?먯꽌 `ai-api` / `ai-api-fastapi` ?꾪솚 ?꾨왂 寃??
   - `/health` ?뺤씤
   - `analyze-diary`, `generate-reply`, `risk-score` ?μ븷 吏뺥썑 泥댄겕由ъ뒪???ш?利?
4. ?꾩슂 ??web-app 濡쒕뵫/?먮윭 臾멸뎄 ?ㅻ벉湲?

## ?ㅼ쓬 異붿쿇 ?곗꽑?쒖쐞
1. ??鍮뚮뱶/?ㅽ뻾 ?곹깭 蹂듦뎄? ?섍꼍蹂???쒗뵆由??뺣━
2. ?댁쁺 泥댄겕由ъ뒪?몃? ?ㅼ젣 ?ㅽ뻾 ?덉감濡?寃利?
3. Safety 洹쒖튃 蹂댁젙怨??섑뵆 ?곗씠???뺤옣
4. ?꾩슂 ??`backend-api`?먯꽌 `ai-api` / `ai-api-fastapi` ?꾪솚 ?꾨왂 寃??
5. ?댄썑 ?꾩슂 ??Report 怨좊룄???먮뒗 紐⑤컮???ㅼ젣 ?곕룞 吏꾪뻾

## 理쒓렐 ?뺤씤 硫붾え
- `ChatControllerTest`, `ReportControllerTest` ?듦낵
- `AuthControllerTest`, `UserControllerTest` ?듦낵
- `DiaryServiceTest` ?듦낵
- `PublicApiAuthE2ETest`, `PublicApiUserE2ETest`, `PublicApiReportMonthlySummaryE2ETest`, `PublicApiReportWeeklyEmotionsE2ETest`, `PublicApiReportMonthlyRisksE2ETest`, `PublicApiChatE2ETest`, `PublicApiDiaryFallbackE2ETest`, `PublicApiChatFallbackE2ETest` ?듦낵
- `ChatServiceTest`, `CalendarServiceTest`, `CalendarControllerTest` ?듦낵
- `backend-api` ?듭떖 fallback / 議고쉶 怨꾩빟? ?먯감 ?쒕퉬???뚯뒪?몄뿉??而⑦듃濡ㅻ윭 / E2E ?뚯뒪?멸퉴吏 ?뺤옣 以?
- `web-app`? 2026-03-24 湲곗? `npm install`, `.env.local` ?앹꽦, `npm run build` ?듦낵源뚯? 蹂듦뎄 ?꾨즺




### 2026-03-24 diary analyze response mapping refinement
- Completed change:
  - DiaryDetailResponse에 aiPrimaryEmotion, aiEmotionIntensity, aiSummary, aiConfidence 필드 추가
  - diary 상세/생성/수정 응답에서 사용자 입력 감정과 AI 분석 감정을 구분해서 노출하도록 매핑 정리
  - DiaryServiceTest, DiaryControllerTest에 AI 분석 응답 노출 계약 검증 추가
- Verification status:
  - ./gradlew.bat test --tests com.mindcompass.api.diary.DiaryServiceTest --tests com.mindcompass.api.diary.DiaryControllerTest 통과
- Confirmed behavior:
  - diary 응답만 봐도 Spring AI가 계산한 대표 감정, 강도, 요약, confidence를 바로 확인할 수 있다
  - AI 분석 실패 시에는 새 AI 필드도 함께 비워지고, 기존 저장 우선 fallback 계약은 그대로 유지된다
- Live check note:
  - 2026-03-24 실제 POST /api/v1/diaries 호출에서 iPrimaryEmotion, iEmotionIntensity, iSummary, iConfidence가 응답 JSON에 포함되는 것 확인
  - 현재 Spring AI 기본 stub summary의 한글은 런타임 응답에서 인코딩이 깨져 보여 추가 점검 필요


### 2026-03-24 ai-api diary summary utf8 hardening
- Completed change:
  - ai-api DiaryAnalysisService의 기본 summary 문구와 키워드 문자열을 UTF-8 기준으로 다시 저장
  - ai-api build.gradle에 JavaCompile UTF-8 인코딩 설정 추가
  - ai-api application.yaml에 server.servlet.encoding UTF-8 강제 설정 추가
  - DiaryAnalysisServiceTest 추가로 empty/anxious/default summary 문구 검증
- Verification status:
  - ./gradlew.bat test --tests com.mindcompass.aiapi.service.DiaryAnalysisServiceTest --tests com.mindcompass.aiapi.AiApiApplicationTests --stacktrace 통과
- Confirmed behavior:
  - ai-api 소스/컴파일/서블릿 응답 인코딩 기준을 UTF-8로 고정했다
  - 다음 ai-api 재기동 후 backend-api의 aiSummary 한글 깨짐 재확인이 필요하다
- Live check note:
  - ai-api 재기동 후 UTF-8 바이트 기준으로 backend-api diary create 응답의 aiSummary 한글이 정상 확인됨
  - PowerShell Invoke-WebRequest 기본 출력에서는 한글이 깨져 보일 수 있으므로 실검증은 UTF-8 바이트 디코딩 기준으로 확인함

### 2026-03-24 diary screen contract alignment
- Completed change:
  - web-app DiaryDetailResponse 타입에 aiPrimaryEmotion, aiEmotionIntensity, aiSummary, aiConfidence 반영
  - diary 상세 화면에서 사용자 입력 감정과 AI 분석 감정/요약/신뢰도를 함께 표시하도록 정리
  - DIARY_API_LEARNING.md와 web-app README에 diary 상세 화면 계약 메모 추가
- Verification status:
  - web-app에서 npm run build 통과
- Confirmed behavior:
  - backend-api diary 상세 응답 계약과 web-app diary 화면 타입/표시 항목이 일치한다

### 2026-03-24 calendar report ai exposure decision
- Completed change:
  - Calendar와 Report 화면에서 diary AI 결과를 어디까지 노출할지 MVP 기준을 문서화
  - Calendar는 timeline 중심 화면으로 보고 `aiSummary` 전문 노출은 보류
  - Report는 집계형 AI 결과만 유지하고 개별 diary `aiSummary` 나열은 보류
- Related docs:
  - `docs/CALENDAR_API_LEARNING.md`
  - `docs/REPORT_API_LEARNING.md`
- Confirmed behavior:
  - Diary 상세 화면은 AI 해석 문장을 보여주는 주 화면으로 유지
  - Calendar는 빠른 흐름 파악, Report는 기간 집계 인사이트에 집중하는 역할 분리가 정리됨

### 2026-03-24 calendar risk badge review
- Completed review:
  - Calendar에 `riskLevel` 보조 배지를 붙일지 DTO/서비스/UI 기준으로 검토
- Decision:
  - MVP에서는 Calendar에 `riskLevel` 배지를 추가하지 않음
  - 월간 캘린더 셀에는 정보 과밀과 과도한 Safety 신호 노출 우려가 있어 비적용
  - 추후 필요 시 일별 요약 카드 한정으로만 다시 검토
- Related doc:
  - `docs/CALENDAR_API_LEARNING.md`

### 2026-03-24 report aggregate contract hardening
- Completed change:
  - ReportControllerTest에 report 응답에서 `aiSummary`가 노출되지 않는 계약 검증 추가
  - web-app report 타입/화면 문구를 집계형 통계 중심으로 정리
  - web-app README에 report 화면 계약 메모 추가
- Verification status:
  - ./gradlew.bat test --tests com.mindcompass.api.report.ReportControllerTest 통과
  - web-app에서 npm run build 통과
- Confirmed behavior:
  - report는 개별 diary 해석 문장을 끌어오지 않고 월간/주간 집계와 위험도 추이만 보여준다

### 2026-03-24 web route smoke check
- Completed check:
  - `web-app`를 `NEXT_PUBLIC_BACKEND_API_BASE_URL=http://localhost:8080` 기준으로 실행
  - `/diary`, `/calendar`, `/report` 라우트가 200으로 열리는지 확인
  - 초기 렌더링 HTML 기준으로 diary/calendar/report 헤더 문구와 diary AI 요약 섹션 노출 여부를 점검
- Result:
  - diary, calendar, report 라우트 모두 응답 확인
  - diary 라우트에는 AI 분석 관련 문구가 보이고, report 라우트에는 `aiSummary` 노출 흔적이 없음
- Note:
  - 현재 환경에는 브라우저 자동화가 없어 로그인 후 클릭 기반의 완전한 수동 점검은 제한적이었고, 라우트/HTML 스모크 체크 중심으로 확인함
