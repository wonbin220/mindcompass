// Mind Compass 시스템 아키텍처 슬라이드 v3 안전 시안을 생성하는 스크립트입니다.
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
pptx.subject = "Mind Compass web ai architecture v3-safe";
pptx.title = "Mind Compass Web AI Architecture v3-safe";
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
  addRoundRect(slide, x, y, 1.38, 0.64, C.white, "D5DDE8", 0.05);
  addIconImage(slide, x + 0.08, y + 0.1, 0.34, 0.22, iconFile, 0.82);
  slide.addText(title, {
    x: x + 0.48,
    y: y + 0.12,
    w: 0.8,
    h: 0.14,
    fontFace: "Malgun Gothic",
    fontSize: 8.4,
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

slide.addShape(pptx.ShapeType.line, {
  x: 9.18,
  y: 1.54,
  w: 0,
  h: 5.24,
  line: { color: "D6DCE5", width: 0.8 },
});

addRoundRect(slide, 0.58, 1.72, 8.5, 4.28, C.white, C.line);
slide.addShape(pptx.ShapeType.rect, {
  x: 0.58,
  y: 1.72,
  w: 8.5,
  h: 0.06,
  fill: { color: "D9F3F4" },
  line: { color: "D9F3F4" },
});

addTag(slide, 0.86, 1.84, "FRONTEND");
addTag(slide, 2.8, 1.84, "AI PLATFORM");
addTag(slide, 7.42, 3.72, "DATABASE");

addRoundRect(slide, 0.94, 2.34, 1.16, 1.04, C.white, "BCE8E8");
addIconImage(slide, 1.32, 2.55, 0.52, 0.32, "browser-chrome.svg", 0.72);
slide.addText("Next.js", {
  x: 1.1,
  y: 2.96,
  w: 0.96,
  h: 0.16,
  fontFace: "Malgun Gothic",
  fontSize: 10.6,
  bold: true,
  color: C.ink,
  align: "center",
  margin: 0,
});
addRoundRect(slide, 2.52, 2.3, 1.36, 1.12, C.white, "CFE0FF");
addIconImage(slide, 2.91, 2.5, 0.58, 0.34, "spring.svg", 0.72);
slide.addText("backend-api", {
  x: 2.63,
  y: 2.96,
  w: 1.1,
  h: 0.16,
  fontFace: "Malgun Gothic",
  fontSize: 9.8,
  bold: true,
  color: C.ink,
  align: "center",
  margin: 0,
});

addRoundRect(slide, 4.2, 2.3, 1.36, 1.12, C.white, "CFEAD6");
addIconImage(slide, 4.59, 2.5, 0.58, 0.34, "spring-ai.svg", 0.9);
slide.addText("ai-api", {
  x: 4.38,
  y: 2.96,
  w: 1.0,
  h: 0.16,
  fontFace: "Malgun Gothic",
  fontSize: 10.0,
  bold: true,
  color: C.ink,
  align: "center",
  margin: 0,
});

addRoundRect(slide, 5.96, 2.3, 1.46, 1.12, C.white, "F5D7AE");
addIconImage(slide, 6.37, 2.5, 0.6, 0.34, "pytorch-fastapi-combo.svg", 0.92);
slide.addText("ai-api-fastapi", {
  x: 6.1,
  y: 2.96,
  w: 1.18,
  h: 0.16,
  fontFace: "Malgun Gothic",
  fontSize: 8.8,
  bold: true,
  color: C.ink,
  align: "center",
  margin: 0,
});

addStoreNode(slide, 7.42, 3.9, "postgresql.svg", "PostgreSQL", "");
addStoreNode(slide, 7.42, 4.68, "postgresql.svg", "pgvector", "");
addStoreNode(slide, 7.42, 5.46, "openai.svg", "LLM Provider", "");

addArrow(slide, 2.1, 2.87, 2.52, 2.87, "");
addArrow(slide, 3.88, 2.87, 4.2, 2.87, "");
addArrow(slide, 5.56, 2.87, 5.96, 2.87, "");

addLineSegment(slide, 3.09, 3.42, 3.09, 4.22);
addArrow(slide, 3.09, 4.22, 7.42, 4.22, "SQL", "94A3B8", 0.2, -0.18);

addLineSegment(slide, 4.9, 3.58, 4.9, 5.0);
addArrow(slide, 4.9, 5.12, 7.04, 5.12, "벡터 검색", "94A3B8", 0.04, -0.18);

addLineSegment(slide, 6.62, 3.58, 6.62, 5.78);
addArrow(slide, 6.62, 5.86, 7.04, 5.86, "GPT 호출", "94A3B8", -0.2, -0.18);

slide.addText("핵심 흐름", {
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

addPhase(slide, 8.86, 1.68, "[Phase 1: 사용자 요청]", [
  "1. 브라우저에서 반응형 웹에 접속합니다.",
  "2. 모든 화면 요청은 backend-api로 전달됩니다.",
]);

addPhase(slide, 8.86, 2.86, "[Phase 2: 공개 API 처리]", [
  "1. 공개 API 진입점은 backend-api 하나입니다.",
  "2. 인증, 일기, 채팅, 캘린더, 리포트를 처리합니다.",
]);

addPhase(slide, 8.86, 4.04, "[Phase 3: 내부 AI 오케스트레이션]", [
  "1. ai-api가 내부 AI 흐름을 조합합니다.",
  "2. 감정 분류가 필요할 때만 ai-api-fastapi를 호출합니다.",
]);

addPhase(slide, 8.86, 5.48, "[Phase 4: 저장소와 최종 응답]", [
  "1. PostgreSQL, pgvector, LLM Provider를 함께 사용합니다.",
  "2. 최종 응답은 다시 backend-api를 통해 반환됩니다.",
]);

slide.addShape(pptx.ShapeType.rect, {
  x: 0.78,
  y: 5.74,
  w: 6.4,
  h: 0.56,
  fill: { color: C.bg },
  line: { color: C.bg, transparency: 100 },
});

slide.addText("3. backend-api만 외부에 공개되고 내부 AI 호출은 서버 내부에서만 이어집니다.", {
  x: 9.28,
  y: 2.18,
  w: 3.98,
  h: 0.34,
  fontFace: "Malgun Gothic",
  fontSize: 10.1,
  color: C.ink,
  margin: 0,
});

slide.addText("3. 사용자는 ai-api나 ai-api-fastapi를 직접 호출하지 않습니다.", {
  x: 8.9,
  y: 3.36,
  w: 3.98,
  h: 0.34,
  fontFace: "Malgun Gothic",
  fontSize: 10.1,
  color: C.ink,
  margin: 0,
});

slide.addText("3. ai-api-fastapi는 감정분류 모델 서빙, 버전 관리, threshold 조정 역할을 맡습니다.", {
  x: 8.9,
  y: 4.9,
  w: 3.98,
  h: 0.34,
  fontFace: "Malgun Gothic",
  fontSize: 9.9,
  color: C.ink,
  margin: 0,
});

slide.addText("3. 최종 응답은 다시 backend-api를 거쳐 반응형 웹 화면에 전달됩니다.", {
  x: 8.9,
  y: 6.32,
  w: 3.98,
  h: 0.3,
  fontFace: "Malgun Gothic",
  fontSize: 10.0,
  color: C.ink,
  margin: 0,
});

slide.addShape(pptx.ShapeType.rect, {
  x: 8.72,
  y: 1.5,
  w: 4.2,
  h: 5.35,
  fill: { color: C.bg },
  line: { color: C.bg, transparency: 100 },
});

slide.addShape(pptx.ShapeType.line, {
  x: 9.18,
  y: 1.54,
  w: 0,
  h: 5.24,
  line: { color: "D6DCE5", width: 0.8 },
});

slide.addText("[Phase 1: 사용자 요청]", {
  x: 8.9,
  y: 1.66,
  w: 3.42,
  h: 0.22,
  fontFace: "Malgun Gothic",
  fontSize: 12.0,
  bold: true,
  color: C.ink,
  margin: 0,
});
slide.addText("1. 브라우저에서 감정캠퍼스 반응형 웹 화면을 엽니다.\n2. Next.js 화면이 필요한 데이터를 backend-api로 요청합니다.", {
  x: 9.3,
  y: 2.02,
  w: 3.32,
  h: 0.62,
  fontFace: "Malgun Gothic",
  fontSize: 9.2,
  color: C.ink,
  margin: 0,
  breakLine: true,
});

slide.addText("[Phase 2: 공개 API 처리]", {
  x: 9.28,
  y: 2.78,
  w: 3.42,
  h: 0.22,
  fontFace: "Malgun Gothic",
  fontSize: 12.0,
  bold: true,
  color: C.ink,
  margin: 0,
});
slide.addText("1. 공개 API 진입점은 backend-api 하나입니다.\n2. 인증, 회원, 일기, 채팅 세션, 캘린더, 리포트 저장/조회를 처리합니다.", {
  x: 9.3,
  y: 3.14,
  w: 3.32,
  h: 0.68,
  fontFace: "Malgun Gothic",
  fontSize: 9.2,
  color: C.ink,
  margin: 0,
  breakLine: true,
});

slide.addText("[Phase 3: 내부 AI 오케스트레이션]", {
  x: 9.28,
  y: 3.98,
  w: 3.42,
  h: 0.22,
  fontFace: "Malgun Gothic",
  fontSize: 12.0,
  bold: true,
  color: C.ink,
  margin: 0,
});
slide.addText("1. backend-api가 내부적으로 ai-api를 호출합니다.\n2. ai-api는 프롬프트, 메모리, RAG, safety, fallback을 조합합니다.\n3. 감정 분류가 필요할 때만 ai-api-fastapi 모델 서빙 계층을 호출합니다.", {
  x: 9.3,
  y: 4.34,
  w: 3.32,
  h: 1.0,
  fontFace: "Malgun Gothic",
  fontSize: 8.95,
  color: C.ink,
  margin: 0,
  breakLine: true,
});

slide.addText("[Phase 4: 저장소와 최종 응답]", {
  x: 9.28,
  y: 5.56,
  w: 3.42,
  h: 0.22,
  fontFace: "Malgun Gothic",
  fontSize: 12.0,
  bold: true,
  color: C.ink,
  margin: 0,
});
slide.addText("1. PostgreSQL, pgvector, LLM Provider를 함께 사용합니다.\n2. 저장/조회는 backend-api가, AI 응답 조합은 ai-api가 담당합니다.\n3. 최종 응답은 다시 backend-api를 통해 웹으로 반환됩니다.", {
  x: 9.3,
  y: 5.92,
  w: 3.32,
  h: 0.86,
  fontFace: "Malgun Gothic",
  fontSize: 8.95,
  color: C.ink,
  margin: 0,
  breakLine: true,
});

warnIfSlideHasOverlaps(slide, pptx);
warnIfSlideElementsOutOfBounds(slide, pptx);

async function main() {
  const fileName = "C:/programing/mindcompass/docs/slides/ai-api-system-architecture/ai-api-system-architecture-reference-style-v3.pptx";
  await pptx.writeFile({ fileName });
  console.log(`Wrote ${fileName}`);
}

main().catch((error) => {
  console.error(error);
  process.exit(1);
});
