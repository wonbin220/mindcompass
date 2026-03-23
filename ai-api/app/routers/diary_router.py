# 일기 분석 내부 API 엔드포인트를 받는 FastAPI 라우터입니다.
from fastapi import APIRouter

from app.schemas.analyze_diary import AnalyzeDiaryRequest, AnalyzeDiaryResponse
from app.services.emotion_analysis_service import EmotionAnalysisService


router = APIRouter(prefix="/internal/ai", tags=["diary"])
service = EmotionAnalysisService()


@router.post("/analyze-diary", response_model=AnalyzeDiaryResponse)
def analyze_diary(request: AnalyzeDiaryRequest) -> AnalyzeDiaryResponse:
    return service.analyze_diary(request)
