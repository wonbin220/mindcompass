# 감정분류 모델 서빙용 요청/응답 계약을 정의하는 스키마 파일입니다.
from decimal import Decimal
from typing import List, Optional

from pydantic import BaseModel, Field


class EmotionScore(BaseModel):
    label: str
    score: Decimal


class EmotionClassifyRequest(BaseModel):
    text: str = Field(..., min_length=1)
    returnTopK: int = Field(default=3, ge=1, le=6)


class EmotionClassifyResponse(BaseModel):
    primaryEmotion: str
    confidence: Decimal
    emotionTags: List[str]
    scores: List[EmotionScore]
    modelName: str
    fallbackUsed: bool = False
    fallbackReason: Optional[str] = None
