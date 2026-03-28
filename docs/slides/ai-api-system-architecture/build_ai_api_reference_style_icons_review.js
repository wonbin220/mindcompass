// Mind Compass 웹 기반 AI 아키텍처 슬라이드를 생성하는 스크립트입니다.
const path = require("path");
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
  soft: "EEF3F8",
  white: "FFFFFF",
  tealSoft: "E6F8F8",
  blueSoft: "EAF2FF",
  amberSoft: "FFF3E4",
  violetSoft: "F3EDFF",
  greenSoft: "EAF8EF",
  accent: "2563EB",
  orange: "F59E0B",
  teal: "0F766E",
  grayText: "6B7280",
  red: "FF4D4F",
  yellow: "FFC53D",
  green: "22C55E",
};

function addRoundRect(slide, x, y, w, h, fill, line = C.line, radius = 0.08) {
  slide.addShape(pptx.ShapeType.roundRect, {
    x, y, w, h,
    rectRadius: radius,
    fill: { color: fill },
    line: { color: line, width: 1 },
  });
}

function addIconPlaceholder(slide, x, y, w, h, label) {
  slide.addShape(pptx.ShapeType.roundRect, {
    x, y, w, h,
    rectRadius: 0.03,
    fill: { color: "FBFCFE" },
    line: { color: "94A3B8", width: 1, dash: "dash" },
  });
  slide.addText(`[아이콘]\n${label}`, {
    x: x + 0.03, y: y + 0.02, w: w - 0.06, h: h - 0.04,
    fontFace: "Malgun Gothic",
    fontSize: 6.4,
    color: C.grayText,
    bold: true,
    align: "center",
    valign: "mid",
    breakLine: true,
    margin: 0,
  });
}

function addIconImage(slide, x, y, w, h, fileName, scale = 0.8) {
  slide.addShape(pptx.ShapeType.roundRect, {
    x, y, w, h,
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
    x: x1, y: y1, w: x2 - x1, h: y2 - y1,
    line: {
      color,
      width: 1.4,
      beginArrowType: "none",
      endArrowType: "triangle",
    },
  });
  if (label) {
    const tx = Math.min(x1, x2) + Math.abs(x2 - x1) / 2 - 0.5 + labelDx;
    const ty = Math.min(y1, y2) + Math.abs(y2 - y1) / 2 - 0.14 + labelDy;
    slide.addText(label, {
      x: tx, y: ty, w: 1.0, h: 0.16,
      fontFace: "Malgun Gothic",
      fontSize: 8,
      color: C.grayText,
      align: "center",
      margin: 0,
      fill: { color: C.bg, transparency: 5 },
    });
  }
}

function addBulletList(slide, x, y, w, items, fontSize = 12.5, color = C.ink, gap = 0.56) {
  items.forEach((item, index) => {
    slide.addText([{ text: item, options: { bullet: { indent: 16 } } }], {
      x, y: y + index * gap, w, h: 0.44,
      fontFace: "Malgun Gothic",
      fontSize,
      color,
      margin: 0,
      breakLine: false,
      valign: "mid",
    });
  });
}

function addCardTitle(slide, x, y, w, title, color = C.ink) {
  slide.addText(title, {
    x, y, w, h: 0.22,
    fontFace: "Malgun Gothic",
    fontSize: 13.5,
    bold: true,
    color,
    margin: 0,
  });
}

const slide = pptx.addSlide();
slide.background = { color: C.bg };

slide.addShape(pptx.ShapeType.rect, {
  x: 0, y: 0.03, w: 13.333, h: 0.2,
  fill: { color: C.strip },
  line: { color: C.strip },
});

slide.addText("시스템 아키텍처", {
  x: 0.75, y: 0.54, w: 3.0, h: 0.38,
  fontFace: "Malgun Gothic",
  fontSize: 23,
  bold: true,
  color: C.ink,
  margin: 0,
});

slide.addText("감정 기록부터 AI 상담까지 이어지는 웹 서비스와 AI 계층 구조", {
  x: 0.76, y: 1.18, w: 6.0, h: 0.22,
  fontFace: "Malgun Gothic",
  fontSize: 11.3,
  color: C.sub,
  margin: 0,
});

[
  { x: 10.85, color: C.red },
  { x: 11.28, color: C.yellow },
  { x: 11.71, color: C.green },
].forEach((dot) => {
  slide.addShape(pptx.ShapeType.ellipse, {
    x: dot.x, y: 0.58, w: 0.22, h: 0.22,
    fill: { color: dot.color },
    line: { color: dot.color },
  });
});

addRoundRect(slide, 0.68, 1.72, 7.2, 4.65, C.white, C.line);
slide.addShape(pptx.ShapeType.rect, {
  x: 0.68, y: 1.72, w: 7.2, h: 0.06,
  fill: { color: "D9F3F4" },
  line: { color: "D9F3F4" },
});

addRoundRect(slide, 8.12, 1.72, 4.45, 2.1, C.white, C.line);
addRoundRect(slide, 8.12, 4.05, 4.45, 2.32, C.white, C.line);

slide.addText("반응형 웹", {
  x: 1.18, y: 2.08, w: 1.2, h: 0.2,
  fontFace: "Malgun Gothic",
  fontSize: 14,
  bold: true,
  color: C.teal,
  align: "center",
  margin: 0,
});
addRoundRect(slide, 0.98, 2.34, 1.55, 1.25, C.tealSoft, "BCE8E8");
addIconImage(slide, 1.43, 2.5, 0.64, 0.38, "browser-chrome.svg", 0.72);
slide.addText("Mind Compass\nResponsive Web", {
  x: 1.12, y: 2.96, w: 1.28, h: 0.42,
  fontFace: "Malgun Gothic",
  fontSize: 10.5,
  bold: true,
  color: C.ink,
  align: "center",
  breakLine: true,
  margin: 0,
});
slide.addText("Next.js 기반 웹 화면", {
  x: 1.1, y: 3.32, w: 1.3, h: 0.16,
  fontFace: "Malgun Gothic",
  fontSize: 8.2,
  color: C.sub,
  align: "center",
  margin: 0,
});

slide.addText("backend-api", {
  x: 2.82, y: 2.0, w: 1.4, h: 0.2,
  fontFace: "Malgun Gothic",
  fontSize: 14,
  bold: true,
  color: C.accent,
  align: "center",
  margin: 0,
});
addRoundRect(slide, 2.5, 2.24, 1.92, 1.48, C.blueSoft, "CFE0FF");
addIconImage(slide, 3.12, 2.42, 0.68, 0.38, "spring.svg", 0.72);
slide.addText("Spring Boot public API", {
  x: 2.72, y: 2.94, w: 1.48, h: 0.18,
  fontFace: "Malgun Gothic",
  fontSize: 10.5,
  bold: true,
  color: C.ink,
  align: "center",
  margin: 0,
});
slide.addText("Auth · Diary · Calendar\nChat · Report · Save", {
  x: 2.62, y: 3.18, w: 1.7, h: 0.4,
  fontFace: "Malgun Gothic",
  fontSize: 9.1,
  color: C.sub,
  align: "center",
  breakLine: true,
  margin: 0,
});

slide.addText("ai-api", {
  x: 5.02, y: 2.0, w: 1.3, h: 0.2,
  fontFace: "Malgun Gothic",
  fontSize: 14,
  bold: true,
  color: C.teal,
  align: "center",
  margin: 0,
});
addRoundRect(slide, 4.64, 2.24, 1.92, 1.72, C.greenSoft, "CFEAD6");
addIconImage(slide, 5.26, 2.42, 0.68, 0.38, "spring-ai.svg", 0.9);
slide.addText("AI Orchestrator", {
  x: 4.92, y: 2.94, w: 1.36, h: 0.18,
  fontFace: "Malgun Gothic",
  fontSize: 10.8,
  bold: true,
  color: C.ink,
  align: "center",
  margin: 0,
});
slide.addText("Prompt · Memory · RAG\nSafety · Fallback · Compose", {
  x: 4.74, y: 3.18, w: 1.72, h: 0.44,
  fontFace: "Malgun Gothic",
  fontSize: 9.0,
  color: C.sub,
  align: "center",
  breakLine: true,
  margin: 0,
});

slide.addText("ai-api-fastapi", {
  x: 4.9, y: 4.26, w: 1.8, h: 0.2,
  fontFace: "Malgun Gothic",
  fontSize: 13.4,
  bold: true,
  color: C.orange,
  align: "center",
  margin: 0,
});
addRoundRect(slide, 4.84, 4.5, 1.82, 1.2, C.amberSoft, "F5D7AE");
addIconImage(slide, 5.43, 4.68, 0.64, 0.38, "pytorch-fastapi-combo.svg", 0.92);
slide.addText("Emotion Model API", {
  x: 5.02, y: 5.14, w: 1.42, h: 0.18,
  fontFace: "Malgun Gothic",
  fontSize: 10.6,
  bold: true,
  color: C.ink,
  align: "center",
  margin: 0,
});
slide.addText("emotion classify\nversioning · threshold", {
  x: 5.02, y: 5.36, w: 1.42, h: 0.28,
  fontFace: "Malgun Gothic",
  fontSize: 8.7,
  color: C.sub,
  align: "center",
  breakLine: true,
  margin: 0,
});

slide.addText("RAG / LLM", {
  x: 7.05, y: 2.0, w: 0.78, h: 0.2,
  fontFace: "Malgun Gothic",
  fontSize: 14,
  bold: true,
  color: "7C3AED",
  align: "center",
  margin: 0,
});
addRoundRect(slide, 7.02, 2.24, 0.72, 1.35, C.violetSoft, "DDD2FF");
addIconImage(slide, 7.15, 2.44, 0.46, 0.3, "postgresql.svg", 0.82);
slide.addText("RAG store", {
  x: 7.13, y: 2.86, w: 0.5, h: 0.12,
  fontFace: "Malgun Gothic",
  fontSize: 8.2,
  bold: true,
  color: C.ink,
  align: "center",
  margin: 0,
});
slide.addText("retrieval\ncontext", {
  x: 7.13, y: 3.02, w: 0.5, h: 0.24,
  fontFace: "Malgun Gothic",
  fontSize: 7.3,
  color: C.sub,
  align: "center",
  breakLine: true,
  margin: 0,
});
addRoundRect(slide, 7.02, 4.08, 0.72, 1.15, C.soft, "DCE4EE");
addIconImage(slide, 7.15, 4.25, 0.46, 0.3, "openai.svg", 0.8);
slide.addText("LLM", {
  x: 7.27, y: 4.67, w: 0.24, h: 0.12,
  fontFace: "Malgun Gothic",
  fontSize: 8.2,
  bold: true,
  color: C.ink,
  align: "center",
  margin: 0,
});
slide.addText("provider", {
  x: 7.15, y: 4.84, w: 0.48, h: 0.12,
  fontFace: "Malgun Gothic",
  fontSize: 7.2,
  color: C.sub,
  align: "center",
  margin: 0,
});

addArrow(slide, 2.53, 2.97, 2.58, 2.97, "");
addArrow(slide, 4.42, 2.97, 4.62, 2.97, "");
addArrow(slide, 5.75, 3.96, 5.75, 4.48, "");
addArrow(slide, 6.62, 2.96, 7.0, 2.96, "");
addArrow(slide, 6.62, 4.64, 7.0, 4.64, "");

slide.addText("요청 흐름", {
  x: 1.0, y: 5.98, w: 1.0, h: 0.18,
  fontFace: "Malgun Gothic",
  fontSize: 12.5,
  bold: true,
  color: C.teal,
  margin: 0,
});
slide.addText("Responsive Web -> backend-api -> ai-api -> ai-api-fastapi", {
  x: 1.0, y: 6.18, w: 5.4, h: 0.18,
  fontFace: "Malgun Gothic",
  fontSize: 10.8,
  bold: true,
  color: C.ink,
  margin: 0,
});
slide.addText("RAG store와 LLM Provider는 ai-api가 필요할 때만 내부적으로 호출합니다.", {
  x: 1.0, y: 6.44, w: 5.9, h: 0.16,
  fontFace: "Malgun Gothic",
  fontSize: 9.0,
  color: C.sub,
  margin: 0,
});

addCardTitle(slide, 8.38, 2.0, 1.5, "역할 분리 원칙", C.teal);
addBulletList(slide, 8.35, 2.4, 3.95, [
  "감정캠퍼스는 모바일 앱이 아니라 반응형 웹 서비스입니다.",
  "backend-api가 공개 API 진입점이며 인증, 저장, 비즈니스 흐름을 담당합니다.",
  "ai-api는 AI 오케스트레이터로서 프롬프트, 메모리, RAG, safety, fallback을 조합합니다.",
], 11.2, C.ink, 0.52);

addCardTitle(slide, 8.38, 4.34, 2.0, "모델 서빙 분리", C.orange);
addBulletList(slide, 8.35, 4.74, 3.95, [
  "ai-api-fastapi는 단순 비교 서버가 아니라 감정분류 모델 서빙 계층입니다.",
  "역할은 감정분류 추론, 모델 버전 관리, threshold 실험, 메타데이터 반환입니다.",
  "backend-api가 FastAPI를 직접 호출하지 않고, AI 관련 진입점은 ai-api 하나로 고정합니다.",
], 11.2, C.ink, 0.52);

slide.addText("아이콘 삽입 위치: 각 카드 상단 점선 [아이콘] 박스에 웹/브라우저, Spring, Spring AI, PyTorch/FastAPI, pgvector, OpenAI 아이콘을 넣으면 됩니다.", {
  x: 0.74, y: 7.03, w: 11.0, h: 0.14,
  fontFace: "Malgun Gothic",
  fontSize: 8.2,
  color: C.grayText,
  margin: 0,
});

warnIfSlideHasOverlaps(slide, pptx);
warnIfSlideElementsOutOfBounds(slide, pptx);

async function main() {
  const fileName = "C:/programing/mindcompass/docs/slides/ai-api-system-architecture/ai-api-system-architecture-reference-style-v2-icons-review.pptx";
  await pptx.writeFile({ fileName });
  console.log(`Wrote ${fileName}`);
}

main().catch((error) => {
  console.error(error);
  process.exit(1);
});
