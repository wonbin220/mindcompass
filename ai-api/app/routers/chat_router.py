# 채팅 답변 생성 내부 API 라우트를 받는 FastAPI 라우터입니다.
from fastapi import APIRouter

from app.schemas.generate_reply import GenerateReplyRequest, GenerateReplyResponse
from app.services.reply_generation_service import ReplyGenerationService


router = APIRouter(prefix="/internal/ai", tags=["chat"])
service = ReplyGenerationService()


@router.post("/generate-reply", response_model=GenerateReplyResponse)
def generate_reply(request: GenerateReplyRequest) -> GenerateReplyResponse:
    return service.generate_reply(request)
