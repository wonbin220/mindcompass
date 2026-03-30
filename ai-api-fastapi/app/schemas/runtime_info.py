# 감정 모델 서빙 runtime 설정 조회 응답 스키마를 정의하는 파일입니다.
from pydantic import BaseModel


class EmotionModelRuntimeInfoResponse(BaseModel):
    modelDirConfigured: str
    modelDirResolved: str
    modelDirExists: bool
    labelMapPathConfigured: str
    labelMapPathResolved: str
    labelMapPathExists: bool
    modelName: str
    modelLoadSource: str
    maxLength: int
