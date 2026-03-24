# 일기 감정 분석 결과를 만드는 MVP 서비스 골격입니다.
from app.schemas.analyze_diary import AnalyzeDiaryRequest, AnalyzeDiaryResponse


class EmotionAnalysisService:
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

        return AnalyzeDiaryResponse(
            primaryEmotion="CALM",
            emotionIntensity=2,
            emotionTags=["CALM"],
            summary="MVP 골격 단계의 기본 분석 결과입니다.",
            confidence=0.500,
        )
