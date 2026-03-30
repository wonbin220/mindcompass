# 학습된 감정분류 모델을 내부적으로 호출하기 위한 FastAPI 라우터 파일입니다.
from fastapi import APIRouter

from app.schemas.emotion_classify import EmotionClassifyRequest, EmotionClassifyResponse
from app.schemas.runtime_info import EmotionModelRuntimeInfoResponse
from app.services.emotion_classifier_service import EmotionClassifierService


router = APIRouter(prefix="/internal/model", tags=["model"])
service = EmotionClassifierService()


@router.post("/emotion-classify", response_model=EmotionClassifyResponse)
def emotion_classify(request: EmotionClassifyRequest) -> EmotionClassifyResponse:
    return service.classify_text(request)


@router.get("/runtime-info", response_model=EmotionModelRuntimeInfoResponse)
def runtime_info() -> EmotionModelRuntimeInfoResponse:
    return service.get_runtime_info()
