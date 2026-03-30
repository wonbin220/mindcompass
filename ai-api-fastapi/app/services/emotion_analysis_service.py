# 일기 감정 분석 응답에 감정분류 결과와 fallback 정책을 연결하는 서비스 파일입니다.
from app.schemas.analyze_diary import AnalyzeDiaryRequest, AnalyzeDiaryResponse
from app.schemas.emotion_classify import EmotionClassifyRequest
from app.services.emotion_classifier_service import EmotionClassifierService


class EmotionAnalysisService:
    def __init__(self) -> None:
        self.classifier_service = EmotionClassifierService()

    def analyze_diary(self, request: AnalyzeDiaryRequest) -> AnalyzeDiaryResponse:
        content = request.content.strip()
        if not content:
            return AnalyzeDiaryResponse(
                primaryEmotion="CALM",
                emotionIntensity=1,
                emotionTags=["CALM"],
                summary="내용이 비어 있어 기본 감정으로 처리했습니다.",
                confidence=0.100,
            )

        classified = self.classifier_service.classify_text(
            EmotionClassifyRequest(text=content, returnTopK=3)
        )

        return AnalyzeDiaryResponse(
            primaryEmotion=classified.primaryEmotion,
            emotionIntensity=self._resolve_intensity(float(classified.confidence)),
            emotionTags=classified.emotionTags,
            summary=self._build_summary(
                classified.primaryEmotion,
                classified.fallbackUsed,
                classified.fallbackReason,
            ),
            confidence=classified.confidence,
        )

    def _resolve_intensity(self, confidence: float) -> int:
        if confidence >= 0.85:
            return 5
        if confidence >= 0.65:
            return 4
        if confidence >= 0.45:
            return 3
        if confidence >= 0.25:
            return 2
        return 1

    def _build_summary(
        self,
        primary_emotion: str,
        fallback_used: bool,
        fallback_reason: str | None,
    ) -> str:
        if fallback_used:
            if fallback_reason == EmotionClassifierService.TIRED_FALLBACK_REASON:
                return "TIRED는 아직 fallback 전용으로 취급되어 보수적인 기본 감정 결과로 처리했습니다."
            return "모델 응답이 불안정해 기본 감정 분석 결과로 처리했습니다."
        return f"입력 문장에서 {primary_emotion} 감정이 가장 두드러진 것으로 분석했습니다."
