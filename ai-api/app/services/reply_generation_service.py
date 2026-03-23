# 채팅 답변 생성 결과를 만드는 MVP 서비스 골격입니다.
from app.schemas.generate_reply import GenerateReplyRequest, GenerateReplyResponse


class ReplyGenerationService:
    def generate_reply(self, request: GenerateReplyRequest) -> GenerateReplyResponse:
        message = request.message.strip()
        if not message:
            return GenerateReplyResponse(
                reply="지금 어떤 마음인지 한 문장으로만 다시 적어주셔도 괜찮아요.",
                confidence=0.150,
                responseType="FALLBACK",
            )

        return GenerateReplyResponse(
            reply=f"지금 '{message}' 때문에 마음이 많이 흔들리셨겠어요. 가장 크게 남는 장면을 하나만 같이 정리해볼까요?",
            confidence=0.650,
            responseType="NORMAL",
        )
