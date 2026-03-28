// Mind Compass ai-api 시스템 아키텍처 슬라이드를 생성하는 스크립트입니다.
const pptxgen = require("pptxgenjs");
const {
  warnIfSlideHasOverlaps,
  warnIfSlideElementsOutOfBounds,
  safeOuterShadow,
} = require("./pptxgenjs_helpers");

const pptx = new pptxgen();
pptx.layout = "LAYOUT_WIDE";
pptx.author = "OpenAI Codex";
pptx.company = "Mind Compass";
pptx.subject = "ai-api system architecture";
pptx.title = "Mind Compass ai-api System Architecture";
pptx.lang = "ko-KR";
pptx.theme = {
  headFontFace: "Malgun Gothic",
  bodyFontFace: "Malgun Gothic",
  lang: "ko-KR",
};

const COLORS = {
  ink: "132238",
  sub: "4B5B73",
  accent: "0F766E",
  accentSoft: "D7F3EE",
  blue: "1D4ED8",
  blueSoft: "DBEAFE",
  amber: "B45309",
  amberSoft: "FEF3C7",
  rose: "BE185D",
  roseSoft: "FCE7F3",
  slate: "E8EEF5",
  white: "FFFFFF",
  line: "C8D4E3",
  darkPanel: "0F172A",
  mint: "10B981",
};

function addBg(slide) {
  slide.background = { color: "F7FAFC" };
}

function addHeader(slide, eyebrow, title, subtitle) {
  slide.addText(eyebrow, {
    x: 0.6, y: 0.35, w: 3.0, h: 0.25,
    fontFace: "Malgun Gothic",
    fontSize: 11,
    bold: true,
    color: COLORS.accent,
    charSpace: 0.4,
  });
  slide.addText(title, {
    x: 0.6, y: 0.62, w: 11.1, h: 0.52,
    fontFace: "Malgun Gothic",
    fontSize: 25,
    bold: true,
    color: COLORS.ink,
  });
  slide.addText(subtitle, {
    x: 0.6, y: 1.24, w: 11.4, h: 0.32,
    fontFace: "Malgun Gothic",
    fontSize: 10.8,
    color: COLORS.sub,
    breakLine: false,
  });
}

function addFooter(slide, page) {
  slide.addText(`Mind Compass | ai-api architecture draft | ${page}`, {
    x: 0.6, y: 7.24, w: 4.5, h: 0.12,
    fontFace: "Malgun Gothic",
    fontSize: 8,
    color: "6B7280",
  });
}

function addPanel(slide, x, y, w, h, fill, title, bodyLines, accent) {
  slide.addShape(pptx.ShapeType.roundRect, {
    x, y, w, h,
    rectRadius: 0.08,
    fill: { color: fill },
    line: { color: accent || COLORS.line, width: 1.2 },
    shadow: safeOuterShadow("000000", 0.12, 45, 1.5, 1),
  });
  slide.addText(title, {
    x: x + 0.18, y: y + 0.14, w: w - 0.36, h: 0.26,
    fontFace: "Malgun Gothic",
    fontSize: 13,
    bold: true,
    color: COLORS.ink,
  });
  slide.addText(bodyLines.join("\n"), {
    x: x + 0.18, y: y + 0.46, w: w - 0.36, h: h - 0.58,
    fontFace: "Malgun Gothic",
    fontSize: 10.2,
    color: COLORS.sub,
    breakLine: false,
    margin: 0,
    valign: "top",
  });
}

function addArrow(slide, x, y, w, h, color) {
  slide.addShape(pptx.ShapeType.chevron, {
    x, y, w, h,
    fill: { color },
    line: { color },
  });
}

function addBulletList(slide, x, y, w, items, color = COLORS.sub, h = 2.2) {
  const runs = [];
  items.forEach((item) => {
    runs.push({
      text: item,
      options: { bullet: { indent: 12 }, breakLine: true },
    });
  });
  slide.addText(runs, {
    x, y, w, h,
    fontFace: "Malgun Gothic",
    fontSize: 11,
    color,
    breakLine: false,
    margin: 0,
    valign: "top",
  });
}

function finalizeSlide(slide, page) {
  addFooter(slide, page);
  warnIfSlideHasOverlaps(slide, pptx);
  warnIfSlideElementsOutOfBounds(slide, pptx);
}

// Slide 1
{
  const slide = pptx.addSlide();
  addBg(slide);
  slide.addShape(pptx.ShapeType.rect, {
    x: 0, y: 0, w: 13.33, h: 7.5,
    fill: { color: "F7FAFC" },
    line: { color: "F7FAFC" },
  });
  slide.addShape(pptx.ShapeType.roundRect, {
    x: 0.6, y: 0.7, w: 12.1, h: 5.9,
    rectRadius: 0.12,
    fill: { color: COLORS.white },
    line: { color: COLORS.slate, width: 1.2 },
    shadow: safeOuterShadow("000000", 0.12, 45, 2, 1),
  });
  slide.addText("INTERNAL AI SERVER", {
    x: 0.95, y: 1.0, w: 2.8, h: 0.26,
    fontFace: "Malgun Gothic",
    fontSize: 12,
    bold: true,
    color: COLORS.accent,
    charSpace: 0.5,
  });
  slide.addText("Mind Compass ai-api\nSystem Architecture", {
    x: 0.95, y: 1.35, w: 6.8, h: 1.25,
    fontFace: "Malgun Gothic",
    fontSize: 24,
    bold: true,
    color: COLORS.ink,
    breakLine: true,
    margin: 0,
  });
  slide.addText(
    "Spring Boot public API와 분리된 내부 AI orchestration 서버를 기준으로,\n현재 구현 계약, logical data model, 확장 방향을 한 번에 정리한 초안입니다.",
    {
      x: 0.95, y: 2.7, w: 6.6, h: 0.8,
      fontFace: "Malgun Gothic",
      fontSize: 12,
      color: COLORS.sub,
      breakLine: true,
      margin: 0,
    }
  );
  addPanel(slide, 7.9, 1.25, 4.0, 1.25, COLORS.accentSoft, "Current ai-api role", [
    "Public API 아님",
    "backend-api 전용 내부 호출 서버",
    "분석·위험도·답변 생성 담당",
  ], COLORS.accent);
  addPanel(slide, 7.9, 2.8, 4.0, 1.25, COLORS.blueSoft, "Current endpoints", [
    "GET /health",
    "POST /internal/ai/analyze-diary",
    "POST /internal/ai/risk-score",
    "POST /internal/ai/generate-reply",
  ], COLORS.blue);
  addPanel(slide, 7.9, 4.35, 4.0, 1.45, COLORS.amberSoft, "Key architecture rule", [
    "mobile app -> backend-api",
    "backend-api -> ai-api",
    "ai-api failure must not break product flow",
  ], COLORS.amber);
  finalizeSlide(slide, 1);
}

// Slide 2
{
  const slide = pptx.addSlide();
  addBg(slide);
  addHeader(
    slide,
    "SYSTEM CONTEXT",
    "Public boundary와 internal AI boundary",
    "Mind Compass는 공개 비즈니스 서버와 내부 AI 서버를 분리하고, mobile app이 ai-api를 직접 호출하지 않도록 설계되어 있습니다."
  );
  addPanel(slide, 0.8, 2.0, 2.4, 2.4, COLORS.blueSoft, "Mobile App", [
    "로그인",
    "Diary 작성",
    "Calendar 조회",
    "Chat 상담",
    "Report 보기",
  ], COLORS.blue);
  addArrow(slide, 3.35, 2.95, 0.45, 0.34, COLORS.line);
  addPanel(slide, 3.95, 1.75, 3.15, 3.0, COLORS.white, "backend-api (Spring Boot)", [
    "공개 API entrypoint",
    "인증 / JWT / user",
    "diary / calendar / chat / report",
    "primary persistence",
    "internal ai client 호출",
  ], COLORS.ink);
  addArrow(slide, 7.25, 2.95, 0.45, 0.34, COLORS.line);
  addPanel(slide, 7.85, 1.75, 2.9, 3.0, COLORS.accentSoft, "ai-api (Spring AI)", [
    "analyze-diary",
    "risk-score",
    "generate-reply",
    "prompt orchestration",
    "fallback-safe response",
  ], COLORS.accent);
  addArrow(slide, 10.9, 2.2, 0.45, 0.34, COLORS.line);
  addArrow(slide, 10.9, 3.45, 0.45, 0.34, COLORS.line);
  addPanel(slide, 11.45, 1.75, 1.1, 1.15, COLORS.roseSoft, "OpenAI", [
    "LLM",
  ], COLORS.rose);
  addPanel(slide, 11.45, 3.25, 1.1, 1.5, COLORS.amberSoft, "ai-api-\nfastapi", [
    "legacy / comparison",
    "model serving",
  ], COLORS.amber);
  slide.addText("source of truth", {
    x: 4.65, y: 4.95, w: 1.6, h: 0.2,
    fontFace: "Malgun Gothic", fontSize: 9.5, color: COLORS.blue, bold: true,
  });
  slide.addShape(pptx.ShapeType.line, {
    x: 3.95, y: 5.15, w: 3.15, h: 0,
    line: { color: COLORS.blue, width: 1.2, dash: "dash" },
  });
  slide.addText("핵심 규칙: 앱은 항상 backend-api만 호출하고, ai-api는 내부 분석/생성 전용으로 유지", {
    x: 0.8, y: 5.75, w: 11.6, h: 0.4,
    fontFace: "Malgun Gothic", fontSize: 12, bold: true, color: COLORS.ink,
  });
  finalizeSlide(slide, 2);
}

// Slide 3
{
  const slide = pptx.addSlide();
  addBg(slide);
  addHeader(
    slide,
    "INTERNAL CONTRACTS",
    "ai-api current internal endpoint draft",
    "현재 구현 기준으로 컨트롤러와 DTO에 존재하는 계약만 정리했습니다. backend-api 연동과 발표 자료의 공통 기준선으로 사용할 수 있습니다."
  );
  addPanel(slide, 0.7, 1.85, 2.7, 1.55, COLORS.accentSoft, "GET /health", [
    "상태 점검",
    "response: status, service, runtime",
    "DB/LLM 호출 없음",
  ], COLORS.accent);
  addPanel(slide, 3.55, 1.85, 2.7, 1.55, COLORS.blueSoft, "POST /internal/ai/analyze-diary", [
    "Request: userId, diaryId, content, writtenAt",
    "Response: emotion, intensity, tags, summary, confidence",
  ], COLORS.blue);
  addPanel(slide, 6.4, 1.85, 2.7, 1.55, COLORS.amberSoft, "POST /internal/ai/risk-score", [
    "Request: userId, sessionId?, text, sourceType",
    "Response: riskLevel, riskScore, signals, action",
  ], COLORS.amber);
  addPanel(slide, 9.25, 1.85, 3.35, 1.55, COLORS.roseSoft, "POST /internal/ai/generate-reply", [
    "Request: latest message + history + memorySummary + mode",
    "Response: reply, confidence, responseType",
  ], COLORS.rose);
  slide.addText("Current fallback rules", {
    x: 0.8, y: 4.05, w: 2.2, h: 0.25,
    fontFace: "Malgun Gothic", fontSize: 13, bold: true, color: COLORS.ink,
  });
  addBulletList(slide, 0.9, 4.4, 5.8, [
    "analyze-diary: 빈 content면 CALM, 모델 실패 시 규칙 기반 감정 fallback",
    "risk-score: 빈 text면 LOW, 위험 키워드면 HIGH/MEDIUM fallback",
    "generate-reply: 빈 message 또는 모델 실패 시 FALLBACK 문구 반환",
    "핵심 원칙: AI 실패가 diary/chat 전체 실패로 번지지 않음",
  ]);
  slide.addText("Current implementation notes", {
    x: 6.95, y: 4.05, w: 2.7, h: 0.25,
    fontFace: "Malgun Gothic", fontSize: 13, bold: true, color: COLORS.ink,
  });
  addBulletList(slide, 7.05, 4.4, 5.4, [
    "DTO는 string 중심 계약이라 enum/typed contract 확장 여지 있음",
    "RAG/evidence citation은 문서엔 있지만 현재 Spring AI DTO에는 아직 없음",
    "최근 대화 이력은 4개까지만 prompt에 포함",
    "health, analyze, risk, reply 4개가 현재 명세 대상",
  ]);
  finalizeSlide(slide, 3);
}

// Slide 4
{
  const slide = pptx.addSlide();
  addBg(slide);
  addHeader(
    slide,
    "REQUEST FLOWS",
    "Diary와 Chat에서 ai-api가 끼어드는 위치",
    "동일한 ai-api라도 diary flow와 chat flow는 역할이 다릅니다. diary는 분석 중심, chat은 safety 선판단 후 reply 생성 중심입니다."
  );
  slide.addText("Diary flow", {
    x: 0.8, y: 1.95, w: 1.6, h: 0.25,
    fontFace: "Malgun Gothic", fontSize: 13, bold: true, color: COLORS.accent,
  });
  addPanel(slide, 0.8, 2.25, 1.9, 0.95, COLORS.white, "1. Mobile", ["일기 작성 요청"], COLORS.line);
  addArrow(slide, 2.82, 2.55, 0.35, 0.22, COLORS.line);
  addPanel(slide, 3.25, 2.25, 2.2, 0.95, COLORS.white, "2. backend-api", ["저장 / 권한 확인"], COLORS.line);
  addArrow(slide, 5.57, 2.55, 0.35, 0.22, COLORS.line);
  addPanel(slide, 6.0, 2.25, 2.1, 0.95, COLORS.accentSoft, "3. ai-api", ["analyze-diary", "필요 시 risk-score"], COLORS.accent);
  addArrow(slide, 8.22, 2.55, 0.35, 0.22, COLORS.line);
  addPanel(slide, 8.65, 2.25, 3.2, 0.95, COLORS.white, "4. backend-api response", ["분석 결과 저장 또는 응답 반영"], COLORS.line);

  slide.addText("Chat flow", {
    x: 0.8, y: 4.0, w: 1.6, h: 0.25,
    fontFace: "Malgun Gothic", fontSize: 13, bold: true, color: COLORS.rose,
  });
  addPanel(slide, 0.8, 4.3, 1.9, 1.05, COLORS.white, "1. Mobile", ["메시지 전송"], COLORS.line);
  addArrow(slide, 2.82, 4.65, 0.35, 0.22, COLORS.line);
  addPanel(slide, 3.25, 4.3, 2.2, 1.05, COLORS.white, "2. backend-api", ["사용자 메시지 저장"], COLORS.line);
  addArrow(slide, 5.57, 4.65, 0.35, 0.22, COLORS.line);
  addPanel(slide, 6.0, 4.3, 2.1, 1.05, COLORS.roseSoft, "3. ai-api", ["risk-score 먼저", "안전 아니면 generate-reply"], COLORS.rose);
  addArrow(slide, 8.22, 4.65, 0.35, 0.22, COLORS.line);
  addPanel(slide, 8.65, 4.3, 3.2, 1.05, COLORS.white, "4. backend-api response", ["assistant 메시지 저장", "앱에 최종 반환"], COLORS.line);

  addPanel(slide, 0.95, 5.95, 3.6, 0.9, COLORS.blueSoft, "Why split risk-score first?", [
    "답변 품질보다 safety 분기를 먼저 결정해야 하기 때문",
  ], COLORS.blue);
  addPanel(slide, 4.85, 5.95, 3.6, 0.9, COLORS.amberSoft, "Why keep backend-api in control?", [
    "저장 / 권한 / fallback 응답 책임은 backend-api가 잡아야 하기 때문",
  ], COLORS.amber);
  addPanel(slide, 8.75, 5.95, 3.6, 0.9, COLORS.accentSoft, "Why ai-api still matters?", [
    "모델 전략이 바뀌어도 내부 AI 계약을 한 곳에 유지할 수 있기 때문",
  ], COLORS.accent);
  finalizeSlide(slide, 4);
}

// Slide 5
{
  const slide = pptx.addSlide();
  addBg(slide);
  addHeader(
    slide,
    "LOGICAL DATA MODEL",
    "ai-api logical ERD at a glance",
    "현재 ai-api가 물리 DB를 크게 쓰지 않더라도, 운영·RAG·평가·추적을 위해 어떤 논리 엔티티가 필요한지 미리 정의해두면 이후 확장이 쉬워집니다."
  );
  addPanel(slide, 0.7, 1.9, 2.3, 1.55, COLORS.blueSoft, "Business references", [
    "user_reference",
    "diary_reference",
    "chat_session_reference",
    "chat_message_reference",
  ], COLORS.blue);
  addPanel(slide, 3.35, 1.9, 2.3, 1.55, COLORS.accentSoft, "Prompt + model ops", [
    "ai_model_config",
    "prompt_template",
    "ai_request_log",
  ], COLORS.accent);
  addPanel(slide, 6.0, 1.9, 2.45, 1.75, COLORS.roseSoft, "AI results", [
    "diary_analysis_result",
    "risk_assessment_result",
    "reply_generation_result",
  ], COLORS.rose);
  addPanel(slide, 8.8, 1.9, 2.1, 1.55, COLORS.amberSoft, "Conversation memory", [
    "conversation_context_snapshot",
  ], COLORS.amber);
  addPanel(slide, 11.2, 1.9, 1.35, 1.75, "E4F7EC", "RAG store", [
    "knowledge_document",
    "knowledge_chunk",
    "embedding_vector_reference",
  ], COLORS.mint);

  addArrow(slide, 3.02, 2.55, 0.24, 0.22, COLORS.line);
  addArrow(slide, 5.67, 2.55, 0.24, 0.22, COLORS.line);
  addArrow(slide, 8.48, 2.55, 0.24, 0.22, COLORS.line);
  addArrow(slide, 10.9, 2.55, 0.24, 0.22, COLORS.line);

  slide.addText("Relationship summary", {
    x: 0.8, y: 4.1, w: 2.3, h: 0.25,
    fontFace: "Malgun Gothic", fontSize: 13, bold: true, color: COLORS.ink,
  });
  addBulletList(slide, 0.9, 4.45, 5.7, [
    "user_reference owns diary_reference and chat_session_reference",
    "prompt_template + ai_model_config drive ai_request_log",
    "each ai_request_log produces diary/risk/reply result zero or one time",
    "chat_session_reference can produce many conversation_context_snapshot rows",
  ]);
  slide.addText("Storage boundary", {
    x: 6.95, y: 4.1, w: 2.1, h: 0.25,
    fontFace: "Malgun Gothic", fontSize: 13, bold: true, color: COLORS.ink,
  });
  addBulletList(slide, 7.05, 4.45, 5.4, [
    "authoritative user, diary, chat raw data stays in backend-api",
    "ai-api should mainly own prompt/model/log/result/retrieval metadata",
    "vector values can stay outside PostgreSQL while reference keys remain traceable",
    "logical ERD can exist before physical ai-api DB is finalized",
  ]);
  finalizeSlide(slide, 5);
}

// Slide 6
{
  const slide = pptx.addSlide();
  addBg(slide);
  addHeader(
    slide,
    "DELIVERY VIEW",
    "What can be produced before ai-api is fully finished?",
    "명세, logical ERD, 아키텍처 슬라이드는 ai-api 구현 100% 완료 전에도 먼저 만들고, 이후 구현 확정에 맞춰 v0.2로 다듬는 방식이 가장 효율적입니다."
  );
  addPanel(slide, 0.8, 1.95, 3.4, 2.35, COLORS.accentSoft, "Can do now", [
    "internal API spec draft",
    "logical ERD draft",
    "system architecture slides",
    "backend-api ↔ ai-api call map",
    "fallback responsibility map",
  ], COLORS.accent);
  addPanel(slide, 4.8, 1.95, 3.7, 2.35, COLORS.amberSoft, "Needs later refinement", [
    "OpenAPI strict schema",
    "RAG evidence fields",
    "persistent ai-api storage scope",
    "provider switch detail",
    "final monitoring and alerting",
  ], COLORS.amber);
  addPanel(slide, 9.1, 1.95, 3.4, 2.35, COLORS.roseSoft, "Recommended sequence", [
    "1. Contract draft",
    "2. Logical ERD",
    "3. Architecture deck",
    "4. ai-api implementation hardening",
    "5. spec / deck update",
  ], COLORS.rose);

  slide.addText("Expected ai-api completion view", {
    x: 0.8, y: 4.7, w: 2.8, h: 0.25,
    fontFace: "Malgun Gothic", fontSize: 13, bold: true, color: COLORS.ink,
  });
  addBulletList(slide, 0.9, 5.05, 6.1, [
    "MVP usable 수준: internal contract 정리 + fallback + backend-api integration + tests 기준 약 5~10 작업일",
    "발표/문서/운영 준비까지 포함: 약 1.5~3주 범위가 안전",
    "현재처럼 문서 초안을 먼저 고정하면 구현 변경이 있어도 수정 범위가 좁아짐",
  ], COLORS.sub, 1.75);
  slide.addShape(pptx.ShapeType.roundRect, {
    x: 7.45, y: 4.9, w: 4.95, h: 1.35,
    rectRadius: 0.08,
    fill: { color: COLORS.darkPanel },
    line: { color: COLORS.darkPanel },
  });
  slide.addText("Key message\nai-api가 100% 끝나지 않아도\n명세·ERD·아키텍처 자료는 지금 먼저 만드는 게 맞다.", {
    x: 7.8, y: 5.15, w: 4.25, h: 0.8,
    fontFace: "Malgun Gothic",
    fontSize: 15,
    bold: true,
    align: "center",
    color: COLORS.white,
    breakLine: true,
    margin: 0,
  });
  finalizeSlide(slide, 6);
}

async function main() {
  const outputPath = "C:/programing/mindcompass/docs/slides/ai-api-system-architecture/ai-api-system-architecture.pptx";
  await pptx.writeFile({ fileName: outputPath });
  console.log(`Wrote ${outputPath}`);
}

main().catch((error) => {
  console.error(error);
  process.exit(1);
});
