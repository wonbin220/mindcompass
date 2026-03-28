// Mind Compass 시스템 아키텍처 슬라이드를 생성하는 스크립트입니다.
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
pptx.subject = "Mind Compass web ai architecture";
pptx.title = "Mind Compass Web AI Architecture";
pptx.lang = "ko-KR";
pptx.theme = {
  headFontFace: "Malgun Gothic",
  bodyFontFace: "Malgun Gothic",
  lang: "ko-KR",
};

const ICON_DIR = path.join(__dirname, "assets", "icons");

const C = {
  bg: "F7F9FC",
  strip: "22A6A8",
  ink: "111827",
  sub: "475569",
  line: "D9E2EC",
  white: "FFFFFF",
  tealSoft: "E6F8F8",
  blueSoft: "EAF2FF",
  amberSoft: "FFF3E4",
  greenSoft: "EAF8EF",
  accent: "2563EB",
  orange: "F59E0B",
  teal: "0F766E",
  grayText: "6B7280",
};

function addRoundRect(slide, x, y, w, h, fill, line = C.line, radius = 0.08) {
  slide.addShape(pptx.ShapeType.roundRect, {
    x,
    y,
    w,
    h,
    rectRadius: radius,
    fill: { color: fill },
    line: { color: line, width: 1 },
  });
}

function addTag(slide, x, y, text) {
  const width = Math.max(0.9, text.length * 0.12 + 0.16);
  slide.addShape(pptx.ShapeType.roundRect, {
    x,
    y,
    w: width,
    h: 0.24,
    rectRadius: 0.03,
    fill: { color: "FFFDFA" },
    line: { color: "D9C18D", width: 0.8 },
  });
  slide.addText(text, {
    x: x + 0.06,
    y: y + 0.04,
    w: width - 0.12,
    h: 0.12,
    fontFace: "Malgun Gothic",
    fontSize: 8.2,
    bold: true,
    color: C.grayText,
    margin: 0,
  });
}

function addIconImage(slide, x, y, w, h, fileName, scale = 0.8) {
  slide.addShape(pptx.ShapeType.roundRect, {
    x,
    y,
    w,
    h,
    rectRadius: 0.03,
    fill: { color: "FBFCFE" },
    line: { color: "D7E0EA", width: 1 },
  });
  const imgW = w * scale;
  const imgH = h * scale;
  slide.addImage({
    path: path.join(ICON_DIR, fileName),
    x: x + (w - imgW) / 2,
    y: y + (h - imgH) / 2,
    w: imgW,
    h: imgH,
  });
}

function addArrow(slide, x1, y1, x2, y2, label, color = "94A3B8", labelDx = 0, labelDy = 0) {
  slide.addShape(pptx.ShapeType.line, {
    x: x1,
    y: y1,
    w: x2 - x1,
    h: y2 - y1,
    line: {
      color,
      width: 0.95,
      beginArrowType: "none",
      endArrowType: "triangle",
    },
  });
  if (label) {
    const tx = Math.min(x1, x2) + Math.abs(x2 - x1) / 2 - 0.5 + labelDx;
    const ty = Math.min(y1, y2) + Math.abs(y2 - y1) / 2 - 0.14 + labelDy;
    slide.addText(label, {
      x: tx,
      y: ty,
      w: 1.0,
      h: 0.16,
      fontFace: "Malgun Gothic",
      fontSize: 8,
      color: C.grayText,
      align: "center",
      margin: 0,
      fill: { color: C.bg, transparency: 5 },
    });
  }
}

function addLineSegment(slide, x1, y1, x2, y2, color = "94A3B8", width = 0.95) {
  slide.addShape(pptx.ShapeType.line, {
    x: x1,
    y: y1,
    w: x2 - x1,
    h: y2 - y1,
    line: {
      color,
      width,
      beginArrowType: "none",
      endArrowType: "none",
    },
  });
}

function addPhase(slide, x, y, title, items) {
  slide.addText(title, {
    x,
    y,
    w: 4.1,
    h: 0.22,
    fontFace: "Malgun Gothic",
    fontSize: 13.0,
    bold: true,
    color: C.ink,
    margin: 0,
  });

  let currentY = y + 0.33;
  items.forEach((item) => {
    const isSub = item.startsWith("   ");
    slide.addText(item, {
      x: x + 0.02,
      y: currentY,
      w: 4.06,
      h: isSub ? 0.24 : 0.4,
      fontFace: "Malgun Gothic",
      fontSize: isSub ? 9.6 : 10.5,
      color: C.ink,
      margin: 0,
      breakLine: true,
    });
    currentY += isSub ? 0.22 : 0.36;
  });
}

function addStoreNode(slide, x, y, iconFile, title, subtitle) {
  addRoundRect(slide, x, y, 1.48, 0.68, C.white, "D5DDE8", 0.05);
  addIconImage(slide, x + 0.1, y + 0.11, 0.36, 0.24, iconFile, 0.82);
  slide.addText(title, {
    x: x + 0.54,
    y: y + 0.12,
    w: 0.84,
    h: 0.14,
    fontFace: "Malgun Gothic",
    fontSize: 8.8,
    bold: true,
    color: C.ink,
    margin: 0,
  });
  slide.addText(subtitle, {
    x: x + 0.54,
    y: y + 0.31,
    w: 0.84,
    h: 0.12,
    fontFace: "Malgun Gothic",
    fontSize: 7.1,
    color: C.sub,
    margin: 0,
  });
}

const slide = pptx.addSlide();
slide.background = { color: C.bg };

slide.addShape(pptx.ShapeType.rect, {
  x: 0,
  y: 0.03,
  w: 13.333,
  h: 0.2,
  fill: { color: C.strip },
  line: { color: C.strip },
});

slide.addText("시스템 아키텍처", {
  x: 0.75,
  y: 0.54,
  w: 3.0,
  h: 0.38,
  fontFace: "Malgun Gothic",
  fontSize: 23,
  bold: true,
  color: C.ink,
  margin: 0,
});

slide.addText("감정 기록부터 AI 상담까지 이어지는 웹 서비스와 AI 계층 구조", {
  x: 0.76,
  y: 1.18,
  w: 6.0,
  h: 0.22,
  fontFace: "Malgun Gothic",
  fontSize: 11.3,
  color: C.sub,
  margin: 0,
});

[
  { x: 10.85, color: "FF4D4F" },
  { x: 11.28, color: "FFC53D" },
  { x: 11.71, color: "22C55E" },
].forEach((dot) => {
  slide.addShape(pptx.ShapeType.ellipse, {
    x: dot.x,
    y: 0.58,
    w: 0.22,
    h: 0.22,
    fill: { color: dot.color },
    line: { color: dot.color },
  });
});

addRoundRect(slide, 0.68, 1.72, 7.9, 4.65, C.white, C.line);
slide.addShape(pptx.ShapeType.rect, {
  x: 0.68,
  y: 1.72,
  w: 7.9,
  h: 0.06,
  fill: { color: "D9F3F4" },
  line: { color: "D9F3F4" },
});

addTag(slide, 0.86, 1.84, "FRONTEND");
addTag(slide, 2.8, 1.84, "AI PLATFORM");
addTag(slide, 7.1, 3.8, "DATABASE");

slide.addText("반응형 웹", {
  x: 1.0,
  y: 2.08,
  w: 1.1,
  h: 0.2,
  fontFace: "Malgun Gothic",
  fontSize: 14,
  bold: true,
  color: C.teal,
  align: "center",
  margin: 0,
});
addRoundRect(slide, 0.94, 2.34, 1.28, 1.18, C.tealSoft, "BCE8E8");
addIconImage(slide, 1.25, 2.5, 0.64, 0.38, "browser-chrome.svg", 0.72);
slide.addText("Mind Compass\nResponsive Web", {
  x: 1.0,
  y: 2.92,
  w: 1.14,
  h: 0.42,
  fontFace: "Malgun Gothic",
  fontSize: 10.3,
  bold: true,
  color: C.ink,
  align: "center",
  breakLine: true,
  margin: 0,
});
slide.addText("Next.js 기반 웹 화면", {
  x: 0.96,
  y: 3.3,
  w: 1.24,
  h: 0.16,
  fontFace: "Malgun Gothic",
  fontSize: 7.8,
  color: C.sub,
  align: "center",
  margin: 0,
});

slide.addText("backend-api", {
  x: 2.62,
  y: 1.98,
  w: 1.3,
  h: 0.2,
  fontFace: "Malgun Gothic",
  fontSize: 14,
  bold: true,
  color: C.accent,
  align: "center",
  margin: 0,
});
addRoundRect(slide, 2.42, 2.24, 1.52, 1.36, C.blueSoft, "CFE0FF");
addIconImage(slide, 2.84, 2.42, 0.68, 0.38, "spring.svg", 0.72);
slide.addText("Spring Boot public API", {
  x: 2.58,
  y: 2.92,
  w: 1.22,
  h: 0.18,
  fontFace: "Malgun Gothic",
  fontSize: 10.3,
  bold: true,
  color: C.ink,
  align: "center",
  margin: 0,
});
slide.addText("Auth · Diary · Calendar\nChat · Report · Save", {
  x: 2.52,
  y: 3.16,
  w: 1.34,
  h: 0.24,
  fontFace: "Malgun Gothic",
  fontSize: 8.2,
  color: C.sub,
  align: "center",
  breakLine: true,
  margin: 0,
});

slide.addText("ai-api", {
  x: 4.34,
  y: 1.98,
  w: 1.0,
  h: 0.2,
  fontFace: "Malgun Gothic",
  fontSize: 14,
  bold: true,
  color: C.teal,
  align: "center",
  margin: 0,
});
addRoundRect(slide, 4.14, 2.24, 1.52, 1.36, C.greenSoft, "CFEAD6");
addIconImage(slide, 4.56, 2.42, 0.68, 0.38, "spring-ai.svg", 0.9);
slide.addText("AI Orchestrator", {
  x: 4.3,
  y: 2.92,
  w: 1.2,
  h: 0.18,
  fontFace: "Malgun Gothic",
  fontSize: 10.4,
  bold: true,
  color: C.ink,
  align: "center",
  margin: 0,
});
slide.addText("Prompt · Memory · RAG\nSafety · Fallback · Compose", {
  x: 4.22,
  y: 3.16,
  w: 1.36,
  h: 0.24,
  fontFace: "Malgun Gothic",
  fontSize: 8.0,
  color: C.sub,
  align: "center",
  breakLine: true,
  margin: 0,
});

slide.addText("ai-api-fastapi", {
  x: 6.02,
  y: 1.98,
  w: 1.46,
  h: 0.2,
  fontFace: "Malgun Gothic",
  fontSize: 13.4,
  bold: true,
  color: C.orange,
  align: "center",
  margin: 0,
});
addRoundRect(slide, 5.86, 2.24, 1.52, 1.36, C.amberSoft, "F5D7AE");
addIconImage(slide, 6.28, 2.42, 0.64, 0.38, "pytorch-fastapi-combo.svg", 0.92);
slide.addText("Emotion Model API", {
  x: 6.0,
  y: 2.92,
  w: 1.24,
  h: 0.18,
  fontFace: "Malgun Gothic",
  fontSize: 10.3,
  bold: true,
  color: C.ink,
  align: "center",
  margin: 0,
});
slide.addText("emotion classify\nversioning · threshold", {
  x: 5.98,
  y: 3.16,
  w: 1.28,
  h: 0.24,
  fontFace: "Malgun Gothic",
  fontSize: 8.0,
  color: C.sub,
  align: "center",
  breakLine: true,
  margin: 0,
});

addStoreNode(slide, 7.04, 4.04, "postgresql.svg", "PostgreSQL", "공개 서비스 저장");
addStoreNode(slide, 7.04, 4.78, "postgresql.svg", "pgvector", "RAG 문맥 검색");
addStoreNode(slide, 7.04, 5.52, "openai.svg", "LLM Provider", "최종 응답 생성");

addArrow(slide, 2.22, 2.92, 2.42, 2.92, "");
addArrow(slide, 3.94, 2.92, 4.14, 2.92, "");
addArrow(slide, 5.66, 2.92, 5.86, 2.92, "");

addLineSegment(slide, 3.18, 3.58, 3.18, 4.38);
addArrow(slide, 3.18, 4.38, 7.04, 4.38, "SQL", "94A3B8", 0.24, -0.18);

addLineSegment(slide, 4.9, 3.58, 4.9, 5.12);
addArrow(slide, 4.9, 5.12, 7.04, 5.12, "벡터 검색", "94A3B8", 0.04, -0.18);

addLineSegment(slide, 6.62, 3.58, 6.62, 5.86);
addArrow(slide, 6.62, 5.86, 7.04, 5.86, "GPT 호출", "94A3B8", -0.2, -0.18);

slide.addText("요청 흐름", {
  x: 1.0,
  y: 5.92,
  w: 1.0,
  h: 0.18,
  fontFace: "Malgun Gothic",
  fontSize: 12.2,
  bold: true,
  color: C.teal,
  margin: 0,
});
slide.addText("Responsive Web -> backend-api -> ai-api -> ai-api-fastapi", {
  x: 1.0,
  y: 6.12,
  w: 5.4,
  h: 0.18,
  fontFace: "Malgun Gothic",
  fontSize: 10.4,
  bold: true,
  color: C.ink,
  margin: 0,
});
slide.addText("backend-api는 ai-api-fastapi를 직접 호출하지 않고, ai-api가 내부 AI 진입점 역할을 맡습니다.", {
  x: 1.0,
  y: 6.34,
  w: 5.9,
  h: 0.16,
  fontFace: "Malgun Gothic",
  fontSize: 8.6,
  color: C.sub,
  margin: 0,
});

addPhase(slide, 8.86, 1.68, "[Phase 1: 사용자 요청]", [
  "1. 사용자는 브라우저에서 감정캠퍼스 반응형 웹 화면을 엽니다.",
  "2. Next.js가 초기 화면을 렌더링하고, 로그인 상태 확인과 화면 데이터 조회는 backend-api REST API 호출로 이어집니다.",
]);

addPhase(slide, 8.86, 2.86, "[Phase 2: 공개 API 처리]", [
  "1. backend-api는 인증/인가, 회원, 일기, 채팅 세션, 캘린더, 리포트 저장과 조회를 담당합니다.",
  "2. 공개 API 진입점은 backend-api 하나이며, 웹은 ai-api나 ai-api-fastapi를 직접 호출하지 않습니다.",
]);

addPhase(slide, 8.86, 4.04, "[Phase 3: 내부 AI 오케스트레이션]", [
  "1. ai-api는 프롬프트 작성, 메모리 조합, RAG 문맥 조회, safety 분기, fallback 결정을 담당합니다.",
  "2. 감정분류가 필요한 diary/chat 흐름에서만 ai-api가 ai-api-fastapi를 내부 호출합니다.",
  "   - ai-api-fastapi는 감정분류 모델 서빙, 모델 버전 관리, threshold/calibration, inference metadata 반환을 맡습니다.",
]);

addPhase(slide, 8.86, 5.48, "[Phase 4: 저장소와 최종 응답]", [
  "1. backend-api는 공개 서비스 데이터를 PostgreSQL에 저장하고 조회합니다.",
  "2. ai-api는 pgvector 검색 결과와 LLM 응답을 조합해 최종 분석/상담 응답을 만든 뒤 backend-api로 반환합니다.",
  "3. AI 호출 실패 시에도 fallback을 적용해 diary/chat 전체 흐름이 끊기지 않게 유지합니다.",
]);

warnIfSlideHasOverlaps(slide, pptx);
warnIfSlideElementsOutOfBounds(slide, pptx);

async function main() {
  const fileName = "C:/programing/mindcompass/docs/slides/ai-api-system-architecture/ai-api-system-architecture-reference-style-v2.pptx";
  await pptx.writeFile({ fileName });
  console.log(`Wrote ${fileName}`);
}

main().catch((error) => {
  console.error(error);
  process.exit(1);
});
