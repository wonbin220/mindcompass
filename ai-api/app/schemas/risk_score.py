# risk-score 내부 API의 요청/응답 계약을 정의하는 스키마 파일이다.
from decimal import Decimal
from typing import List, Optional

from pydantic import BaseModel


class RiskScoreRequest(BaseModel):
    userId: int
    sessionId: Optional[int] = None
    text: str
    sourceType: str


class RiskScoreResponse(BaseModel):
    riskLevel: str
    riskScore: Decimal
    signals: List[str]
    recommendedAction: str
