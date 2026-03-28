// 개발팀장 대상의 Mind Compass 시스템 아키텍처 슬라이드를 생성하는 스크립트입니다.
const path = require("path");
const pptxgen = require("pptxgenjs");
const {
  warnIfSlideHasOverlaps,
  warnIfSlideElementsOutOfBounds,
} = require("./pptxgenjs_helpers");

const pptx = new pptxgen();
pptx.layout = "LAYOUT_WIDE";
pptx.author = "OpenAI Codex";
pptx.company = "Mind Compass";
pptx.subject = "Mind Compass architecture deck";
pptx.title = "Mind Compass System Architecture";
pptx.lang = "ko-KR";
pptx.theme = {
  headFontFace: "Malgun Gothic",
  bodyFontFace: "Malgun Gothic",
  lang: "ko-KR",
};

const ICON_DIR = path.join(__dirname, "assets", "icons");
const OUT_FILE = path.join(__dirname, "mindcompass-system-architecture-techlead.pptx");

const C = {
  bg: "F0F4F4",
  bar: "10928D",
  ink: "1F2937",
  sub: "5B6875",
  line: "CBD5D6",
  white: "FFFFFF",
  leftCanvas: "F8FAFA",
  tealSoft: "E6F4F3",
  blueSoft: "EBF3FF",
  greenSoft: "EBF8EF",
  amberSoft: "FFF4E8",
  purpleSoft: "F4EEFF",
  tagFill: "FFF9EE",
  tagLine: "DAB978",
  arrow: "7E8E99",
  divider: "C7D0D1",
};

function addChrome(slide, title, subtitle) {
  slide.background = { color: C.bg };

  slide.addShape(pptx.ShapeType.rect, {
    x: 0,
    y: 0,
    w: 13.333,
    h: 0.24,
    fill: { color: C.bar },
    line: { color: C.bar },
  });

  slide.addText(title, {
    x: 0.52,
    y: 0.48,
    w: 4.8,
    h: 0.36,
    fontFace: "Malgun Gothic",
    fontSize: 22,
    bold: true,
    color: C.ink,
    margin: 0,
  });

  slide.addText(subtitle, {
    x: 0.52,
    y: 0.98,
    w: 6.8,
    h: 0.2,
    fontFace: "Malgun Gothic",
    fontSize: 10.5,
    color: C.sub,
    margin: 0,
  });

  [
    { x: 10.94, color: "FF5A57" },
    { x: 11.28, color: "FFC247" },
    { x: 11.62, color: "31C36C" },
  ].forEach((dot) => {
    slide.addShape(pptx.ShapeType.ellipse, {
      x: dot.x,
      y: 0.48,
      w: 0.18,
      h: 0.18,
      fill: { color: dot.color },
      line: { color: dot.color },
    });
  });

  slide.addShape(pptx.ShapeType.line, {
    x: 9.43,
    y: 1.34,
    w: 0,
    h: 5.75,
    line: { color: C.divider, width: 0.8 },
  });

  slide.addShape(pptx.ShapeType.rect, {
    x: 0.38,
    y: 1.56,
    w: 8.56,
    h: 4.96,
    fill: { color: C.leftCanvas },
    line: { color: "DCE5E5", width: 0.8 },
  });
}

function addTag(slide, x, y, text, w = 1.2) {
  slide.addShape(pptx.ShapeType.roundRect, {
    x,
    y,
    w,
    h: 0.24,
    rectRadius: 0.03,
    fill: { color: C.tagFill },
    line: { color: C.tagLine, width: 0.8 },
  });
  slide.addText(text, {
    x: x + 0.06,
    y: y + 0.045,
    w: w - 0.12,
    h: 0.12,
    fontFace: "Malgun Gothic",
    fontSize: 8.2,
    bold: true,
    color: "6C7278",
    margin: 0,
  });
}

function addBox(slide, x, y, w, h, opts) {
  slide.addShape(pptx.ShapeType.roundRect, {
    x,
    y,
    w,
    h,
    rectRadius: 0.08,
    fill: { color: opts.fill || C.white },
    line: { color: opts.line || C.line, width: 0.8 },
  });

  if (opts.tag) {
    addTag(slide, x + 0.08, y - 0.15, opts.tag, opts.tagWidth || 1.25);
  }
}

function addIcon(slide, x, y, w, h, fileName, scale = 0.82) {
  slide.addShape(pptx.ShapeType.roundRect, {
    x,
    y,
    w,
    h,
    rectRadius: 0.03,
    fill: { color: "FCFEFE" },
    line: { color: "D8E0E2", width: 0.8 },
  });

  const iw = w * scale;
  const ih = h * scale;
  slide.addImage({
    path: path.join(ICON_DIR, fileName),
    x: x + (w - iw) / 2,
    y: y + (h - ih) / 2,
    w: iw,
    h: ih,
  });
}

function addNode(slide, x, y, w, h, opts) {
  slide.addShape(pptx.ShapeType.roundRect, {
    x,
    y,
    w,
    h,
    rectRadius: 0.08,
    fill: { color: opts.fill || C.white },
    line: { color: opts.line || C.line, width: 0.8 },
  });

  if (opts.icon) {
    addIcon(slide, x + w / 2 - 0.27, y + 0.14, 0.54, 0.3, opts.icon, opts.iconScale || 0.8);
  }

  slide.addText(opts.title, {
    x: x + 0.08,
    y: y + 0.5,
    w: w - 0.16,
    h: 0.16,
    fontFace: "Malgun Gothic",
    fontSize: opts.fontSize || 9.2,
    bold: true,
    align: "center",
    color: C.ink,
    margin: 0,
  });

  if (opts.subtitle) {
    slide.addText(opts.subtitle, {
      x: x + 0.08,
      y: y + 0.69,
      w: w - 0.16,
      h: 0.14,
      fontFace: "Malgun Gothic",
      fontSize: 7.2,
      align: "center",
      color: C.sub,
      margin: 0,
    });
  }
}

function addStorage(slide, x, y, title, subtitle, icon = "postgresql.svg") {
  slide.addShape(pptx.ShapeType.roundRect, {
    x,
    y,
    w: 1.34,
    h: 0.6,
    rectRadius: 0.04,
    fill: { color: C.white },
    line: { color: "D8E0E6", width: 0.8 },
  });
  addIcon(slide, x + 0.08, y + 0.1, 0.34, 0.22, icon, 0.84);
  slide.addText(title, {
    x: x + 0.48,
    y: y + 0.12,
    w: 0.76,
    h: 0.14,
    fontFace: "Malgun Gothic",
    fontSize: 8.6,
    bold: true,
    color: C.ink,
    margin: 0,
  });
  slide.addText(subtitle, {
    x: x + 0.48,
    y: y + 0.31,
    w: 0.78,
    h: 0.12,
    fontFace: "Malgun Gothic",
    fontSize: 6.8,
    color: C.sub,
    margin: 0,
  });
}

function addHArrow(slide, x1, y, x2, label, labelY) {
  slide.addShape(pptx.ShapeType.line, {
    x: x1,
    y,
    w: x2 - x1,
    h: 0,
    line: {
      color: C.arrow,
      width: 0.9,
      beginArrowType: "none",
      endArrowType: "triangle",
    },
  });
  if (label) {
    slide.addText(label, {
      x: x1 + (x2 - x1) / 2 - 0.38,
      y: labelY,
      w: 0.76,
      h: 0.12,
      fontFace: "Malgun Gothic",
      fontSize: 7.4,
      align: "center",
      color: "6B7280",
      margin: 0,
      fill: { color: C.leftCanvas, transparency: 4 },
    });
  }
}

function addVArrow(slide, x, y1, y2, label, labelX) {
  slide.addShape(pptx.ShapeType.line, {
    x,
    y: y1,
    w: 0,
    h: y2 - y1,
    line: {
      color: C.arrow,
      width: 0.9,
      beginArrowType: "none",
      endArrowType: "triangle",
    },
  });
  if (label) {
    slide.addText(label, {
      x: labelX,
      y: y1 + (y2 - y1) / 2 - 0.06,
      w: 0.84,
      h: 0.12,
      fontFace: "Malgun Gothic",
      fontSize: 7.4,
      align: "center",
      color: "6B7280",
      margin: 0,
      fill: { color: C.leftCanvas, transparency: 4 },
    });
  }
}

function addRightSection(slide, x, y, title, items) {
  slide.addText(title, {
    x,
    y,
    w: 3.25,
    h: 0.22,
    fontFace: "Malgun Gothic",
    fontSize: 12.4,
    bold: true,
    color: C.ink,
    margin: 0,
  });

  let cy = y + 0.36;
  items.forEach((item, index) => {
    slide.addText(`${index + 1}. ${item}`, {
      x: x + 0.08,
      y: cy,
      w: 3.08,
      h: 0.38,
      fontFace: "Malgun Gothic",
      fontSize: 10,
      breakLine: true,
      color: C.ink,
      margin: 0,
    });
    cy += 0.42;
  });
}

function addFooter(slide, text) {
  slide.addText(text, {
    x: 0.52,
    y: 6.88,
    w: 6,
    h: 0.12,
    fontFace: "Malgun Gothic",
    fontSize: 7.2,
    color: "6B7280",
    margin: 0,
  });
}

function buildRestSlide() {
  const slide = pptx.addSlide();
  addChrome(
    slide,
    "RESTful API 아키텍처",
    "클라이언트 요청을 backend-api 단일 공개 경계로 수렴시키고 도메인 처리와 저장소 접근을 분리한 구조"
  );

  addBox(slide, 0.92, 2.12, 1.54, 1.08, {
    fill: "FCFCFB",
    line: "DAB97B",
    tag: "FRONTEND",
    tagWidth: 1.06,
  });
  addNode(slide, 1.16, 2.34, 1.04, 0.68, {
    fill: C.white,
    line: "C8D8E6",
    icon: "browser-chrome.svg",
    title: "Web App",
    subtitle: "Next.js / Tailwind",
  });

  slide.addText("User", {
    x: 0.04,
    y: 2.56,
    w: 0.34,
    h: 0.16,
    fontFace: "Malgun Gothic",
    fontSize: 8.6,
    color: C.ink,
    margin: 0,
  });
  slide.addShape(pptx.ShapeType.ellipse, {
    x: 0.16,
    y: 2.24,
    w: 0.14,
    h: 0.14,
    fill: { color: C.bg, transparency: 100 },
    line: { color: "666666", width: 0.8 },
  });
  slide.addShape(pptx.ShapeType.line, {
    x: 0.23,
    y: 2.38,
    w: 0,
    h: 0.2,
    line: { color: "666666", width: 0.8 },
  });

  addBox(slide, 3.32, 1.92, 3.92, 2.58, {
    fill: "FFFCF8",
    line: "DAB97B",
    tag: "PUBLIC API",
    tagWidth: 1.08,
  });
  addNode(slide, 3.62, 2.42, 0.84, 0.68, {
    fill: C.purpleSoft,
    line: "CFC2EE",
    title: "LB",
    subtitle: "entry",
  });
  addNode(slide, 4.7, 2.42, 0.84, 0.68, {
    fill: "F3F5F6",
    line: "CBD3D8",
    title: "Nginx",
    subtitle: "routing",
  });
  addNode(slide, 5.78, 2.18, 1.12, 0.78, {
    fill: C.greenSoft,
    line: "B8DABE",
    icon: "spring.svg",
    title: "backend-api",
    subtitle: "Controller / Service",
  });
  addNode(slide, 5.78, 3.18, 1.12, 0.78, {
    fill: C.tealSoft,
    line: "B8DAD6",
    icon: "spring-ai.svg",
    title: "ai-api",
    subtitle: "internal inference",
  });

  addNode(slide, 3.62, 3.58, 0.72, 0.52, {
    fill: C.blueSoft,
    line: "C7D7F0",
    title: "Auth",
    fontSize: 8.3,
  });
  addNode(slide, 4.44, 3.58, 0.72, 0.52, {
    fill: C.blueSoft,
    line: "C7D7F0",
    title: "Diary",
    fontSize: 8.3,
  });
  addNode(slide, 5.26, 3.58, 0.72, 0.52, {
    fill: C.blueSoft,
    line: "C7D7F0",
    title: "Chat",
    fontSize: 8.3,
  });
  addNode(slide, 6.08, 3.58, 0.72, 0.52, {
    fill: C.blueSoft,
    line: "C7D7F0",
    title: "Report",
    fontSize: 8.3,
  });

  addBox(slide, 7.92, 1.96, 1.08, 2.2, {
    fill: "FFFCF8",
    line: "DAB97B",
    tag: "DATABASE",
    tagWidth: 1.04,
  });
  addStorage(slide, 8.06, 2.32, "PostgreSQL", "users / diary / chat");
  addStorage(slide, 8.06, 3.1, "pgvector", "retrieval / context");

  addHArrow(slide, 0.38, 2.64, 0.92, "HTTPS", 2.52);
  addHArrow(slide, 2.46, 2.64, 3.62, "REST API", 2.52);
  addHArrow(slide, 4.46, 2.76, 4.7, null, 0);
  addHArrow(slide, 5.54, 2.76, 5.78, null, 0);
  addVArrow(slide, 6.34, 2.96, 3.18, "내부 호출", 6.42);
  addHArrow(slide, 6.9, 2.56, 8.06, "SQL", 2.44);
  addHArrow(slide, 6.9, 3.56, 8.06, "벡터 검색", 3.44);

  addRightSection(slide, 9.56, 1.46, "[Phase 1: 사용자 요청]", [
    "웹 화면은 필요한 데이터만 REST 요청으로 backend-api에 보냅니다.",
    "클라이언트는 내부 AI 서버나 저장소 위치를 직접 알 필요가 없습니다.",
  ]);
  addRightSection(slide, 9.56, 2.86, "[Phase 2: 공개 API 처리]", [
    "Load Balancer와 Nginx가 트래픽을 받아 공개 API 요청을 backend-api로 전달합니다.",
    "backend-api가 인증, 권한, 도메인 규칙, 응답 DTO 조립을 담당합니다.",
  ]);
  addRightSection(slide, 9.56, 4.24, "[Phase 3: 저장소와 내부 AI]", [
    "정형 데이터는 PostgreSQL에 저장하고 검색/임베딩 컨텍스트는 pgvector를 활용합니다.",
    "AI가 필요한 요청만 내부 ai-api로 전달하므로 public boundary가 흔들리지 않습니다.",
  ]);
  addRightSection(slide, 9.56, 5.64, "[Phase 4: 응답 반환]", [
    "최종 응답은 항상 backend-api 계약으로 반환되어 프론트 변경 비용을 줄입니다.",
    "내부 AI 구조가 바뀌어도 외부 API 계약은 안정적으로 유지됩니다.",
  ]);

  addFooter(slide, "핵심 메시지: backend-api 단일 공개 경계, 도메인별 서비스 분리, AI 계층의 내부화");
  warnIfSlideHasOverlaps(slide, pptx);
  warnIfSlideElementsOutOfBounds(slide, pptx);
}

function buildChatSlide() {
  const slide = pptx.addSlide();
  addChrome(
    slide,
    "AI 챗봇 아키텍처",
    "세션 저장, 안전 분기, RAG 컨텍스트, 답변 생성, 비교용 FastAPI 계층까지 연결한 내부 처리 흐름"
  );

  addBox(slide, 0.86, 2.14, 1.44, 1.02, {
    fill: "FCFCFB",
    line: "DAB97B",
    tag: "CHAT UI",
    tagWidth: 0.92,
  });
  addNode(slide, 1.08, 2.34, 1.02, 0.66, {
    fill: C.white,
    line: "C8D8E6",
    icon: "browser-chrome.svg",
    title: "Chat Screen",
    subtitle: "session / message",
    fontSize: 8.8,
  });

  addBox(slide, 2.72, 1.92, 2.04, 2.96, {
    fill: C.white,
    line: "CCD8D9",
    tag: "SPRING BOOT",
    tagWidth: 1.14,
  });
  addNode(slide, 3.04, 2.24, 1.42, 0.72, {
    fill: C.greenSoft,
    line: "B8DABE",
    icon: "spring.svg",
    title: "backend-api",
    subtitle: "ChatController / ChatService",
  });
  addNode(slide, 3.04, 3.18, 1.42, 0.62, {
    fill: C.blueSoft,
    line: "C7D7F0",
    title: "세션 / 메시지 저장",
    subtitle: "PostgreSQL",
    fontSize: 8.6,
  });
  addNode(slide, 3.04, 4, 1.42, 0.62, {
    fill: C.amberSoft,
    line: "E2C186",
    title: "Fallback 응답",
    subtitle: "AI 실패 시 안전 문구",
    fontSize: 8.6,
  });

  addBox(slide, 5.12, 1.92, 2.38, 2.96, {
    fill: "FCFEFD",
    line: "C8DAD6",
    tag: "AI PLATFORM",
    tagWidth: 1.28,
  });
  addNode(slide, 5.44, 2.18, 1.74, 0.66, {
    fill: C.tealSoft,
    line: "B8DAD6",
    icon: "spring-ai.svg",
    title: "ai-api",
    subtitle: "Router / Service orchestration",
  });
  addNode(slide, 5.44, 3.12, 0.76, 0.54, {
    fill: C.amberSoft,
    line: "E2C186",
    title: "risk-score",
    fontSize: 8.1,
  });
  addNode(slide, 6.02, 3.12, 0.76, 0.54, {
    fill: C.blueSoft,
    line: "C7D7F0",
    title: "RAG",
    fontSize: 8.3,
  });
  addNode(slide, 6.6, 3.12, 0.76, 0.54, {
    fill: C.purpleSoft,
    line: "D3C8F0",
    title: "reply",
    fontSize: 8.3,
  });
  addNode(slide, 5.78, 4.04, 1.06, 0.56, {
    fill: "FFF9EE",
    line: "DAB97B",
    icon: "fastapi.svg",
    title: "ai-api-fastapi",
    fontSize: 7.8,
  });

  addBox(slide, 7.88, 1.92, 1.12, 2.96, {
    fill: "FFFCF8",
    line: "DAB97B",
    tag: "KNOWLEDGE",
    tagWidth: 1,
  });
  addStorage(slide, 8.04, 2.18, "pgvector", "문서 / 임베딩");
  addStorage(slide, 8.04, 2.96, "History DB", "chat / diary");
  addStorage(slide, 8.04, 3.74, "LLM", "OpenAI provider", "openai.svg");

  addHArrow(slide, 2.3, 2.62, 3.04, "메시지 전송", 2.5);
  addHArrow(slide, 4.46, 2.62, 5.44, "내부 AI 호출", 2.5);
  addVArrow(slide, 3.75, 2.96, 3.18, "저장", 3.82);
  addVArrow(slide, 6.3, 2.84, 3.12, "안전 분기", 6.34);
  addVArrow(slide, 6.3, 3.66, 4.04, "비교 모델", 6.36);
  addHArrow(slide, 6.2, 3.38, 8.04, "retrieval", 3.26);
  addHArrow(slide, 6.78, 3.38, 8.04, "history", 3.56);
  addHArrow(slide, 7.36, 3.38, 8.04, "prompt", 3.86);

  addRightSection(slide, 9.56, 1.46, "[Phase 1: 입력과 안전 분기]", [
    "채팅 입력은 backend-api에서 세션 권한과 요청 형식을 먼저 확인합니다.",
    "ai-api는 risk-score를 우선 실행해 SUPPORTIVE, SAFETY, FALLBACK 분기를 결정합니다.",
  ]);
  addRightSection(slide, 9.56, 2.92, "[Phase 2: 컨텍스트 수집]", [
    "RAG 단계에서 pgvector와 대화 이력을 함께 조회해 답변 컨텍스트를 조립합니다.",
    "필요하면 ai-api-fastapi를 비교용 모델 서버로 호출해 실험 경로를 유지합니다.",
  ]);
  addRightSection(slide, 9.56, 4.36, "[Phase 3: 답변 생성]", [
    "reply 단계가 안전 분기 결과와 검색 컨텍스트를 묶어 최종 응답을 생성합니다.",
    "채팅 로그는 backend-api를 통해 저장되어 이후 개인화와 리포트에 다시 활용됩니다.",
  ]);
  addRightSection(slide, 9.56, 5.8, "[Phase 4: fallback 전략]", [
    "LLM이나 내부 AI가 실패해도 전체 대화 흐름은 fallback 응답으로 유지됩니다.",
    "클라이언트는 언제나 동일한 backend-api 응답 계약만 바라보면 됩니다.",
  ]);

  addFooter(slide, "핵심 메시지: safety-first 분기, RAG 기반 맥락화, AI 실패에도 끊기지 않는 챗봇 흐름");
  warnIfSlideHasOverlaps(slide, pptx);
  warnIfSlideElementsOutOfBounds(slide, pptx);
}

async function main() {
  buildRestSlide();
  buildChatSlide();
  await pptx.writeFile({ fileName: OUT_FILE });
}

main().catch((error) => {
  console.error(error);
  process.exitCode = 1;
});
