# generate-reply 내부 API의 요청/응답 계약을 정의하는 스키마 파일입니다.
from decimal import Decimal
from typing import List, Optional

from pydantic import BaseModel


class ConversationTurn(BaseModel):
    role: str
    content: str


class GenerateReplyRequest(BaseModel):
    userId: int
    sessionId: int
    message: str
    conversationHistory: List[ConversationTurn]
    memorySummary: Optional[str] = None
    mode: str = "EMPATHETIC"


class GenerateReplyResponse(BaseModel):
    reply: str
    confidence: Decimal
    responseType: str
