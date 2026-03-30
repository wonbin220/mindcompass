# KcELECTRA 감정분류 모델 추론과 fallback 규칙을 조정하는 서비스 파일입니다.
from __future__ import annotations

from decimal import Decimal
from pathlib import Path
from typing import List
import os

from app.inference.label_mapper import load_label_map, resolve_label, resolve_tags
from app.inference.predictor import build_predictor
from app.schemas.emotion_classify import (
    EmotionClassifyRequest,
    EmotionClassifyResponse,
    EmotionScore,
)
from app.schemas.runtime_info import EmotionModelRuntimeInfoResponse


class EmotionClassifierService:
    TIRED_FALLBACK_REASON = "TIRED_FALLBACK_ONLY"

    def __init__(self) -> None:
        label_map_path = Path(
            os.getenv(
                "EMOTION_LABEL_MAP_PATH",
                "ai-api-fastapi/training/emotion_classifier/configs/label_map.json",
            )
        )
        self.label_map = load_label_map(label_map_path)
        self.predictor = build_predictor()

    def classify_text(self, request: EmotionClassifyRequest) -> EmotionClassifyResponse:
        text = request.text.strip()
        if not text:
            return self._fallback_response(reason="EMPTY_TEXT")

        try:
            prediction = self.predictor.predict(text, top_k=request.returnTopK)
            primary_label = resolve_label(self.label_map, prediction.predicted_index)
            score_items = self._build_scores(prediction.score_pairs)
            if primary_label == "TIRED":
                return self._fallback_response(reason=self.TIRED_FALLBACK_REASON)
            return EmotionClassifyResponse(
                primaryEmotion=primary_label,
                confidence=Decimal(str(round(prediction.confidence, 4))),
                emotionTags=resolve_tags(self.label_map, primary_label),
                scores=score_items,
                modelName=self.predictor.model_name,
                fallbackUsed=False,
            )
        except Exception:
            return self._fallback_response(reason="MODEL_ERROR")

    def get_runtime_info(self) -> EmotionModelRuntimeInfoResponse:
        label_map_path = Path(
            os.getenv(
                "EMOTION_LABEL_MAP_PATH",
                "ai-api-fastapi/training/emotion_classifier/configs/label_map.json",
            )
        )
        resolved_label_map_path = label_map_path.expanduser().resolve()
        return EmotionModelRuntimeInfoResponse(
            modelDirConfigured=str(self.predictor.model_dir),
            modelDirResolved=str(self.predictor.model_dir_resolved()),
            modelDirExists=self.predictor.model_dir_resolved().exists(),
            labelMapPathConfigured=str(label_map_path),
            labelMapPathResolved=str(resolved_label_map_path),
            labelMapPathExists=resolved_label_map_path.exists(),
            modelName=self.predictor.model_name,
            modelLoadSource=self.predictor.model_load_source(),
            maxLength=self.predictor.max_length,
        )

    def _build_scores(self, score_pairs: List[tuple[int, float]]) -> List[EmotionScore]:
        items: List[EmotionScore] = []
        for class_id, score in score_pairs:
            label = resolve_label(self.label_map, class_id)
            items.append(
                EmotionScore(
                    label=label,
                    score=Decimal(str(round(score, 4))),
                )
            )
        return items

    def _fallback_response(self, reason: str) -> EmotionClassifyResponse:
        return EmotionClassifyResponse(
            primaryEmotion="CALM",
            confidence=Decimal("0.10"),
            emotionTags=["CALM"],
            scores=[EmotionScore(label="CALM", score=Decimal("0.10"))],
            modelName=self.predictor.model_name,
            fallbackUsed=True,
            fallbackReason=reason,
        )
