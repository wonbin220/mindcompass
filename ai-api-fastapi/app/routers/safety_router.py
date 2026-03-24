# 위험 신호 스코어링 내부 API 요청을 받는 FastAPI 라우터다.
from fastapi import APIRouter

from app.schemas.risk_score import RiskScoreRequest, RiskScoreResponse
from app.services.risk_scoring_service import RiskScoringService


router = APIRouter(prefix="/internal/ai", tags=["safety"])
service = RiskScoringService()


@router.post("/risk-score", response_model=RiskScoreResponse)
def risk_score(request: RiskScoreRequest) -> RiskScoreResponse:
    return service.score_risk(request)
