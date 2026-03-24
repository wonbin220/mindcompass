# analyze-diary 내부 API의 요청/응답 계약을 정의하는 스키마 파일입니다.
from decimal import Decimal
from typing import List

from pydantic import BaseModel


class AnalyzeDiaryRequest(BaseModel):
    userId: int
    diaryId: int
    content: str
    writtenAt: str


class AnalyzeDiaryResponse(BaseModel):
    primaryEmotion: str
    emotionIntensity: int
    emotionTags: List[str]
    summary: str
    confidence: Decimal
