# 학습된 KcELECTRA 모델을 로드하고 추론 결과를 반환하는 예측기 파일입니다.
from __future__ import annotations

from dataclasses import dataclass
from pathlib import Path
from typing import List, Optional
import os

import torch
from transformers import AutoModelForSequenceClassification, AutoTokenizer


@dataclass
class PredictionResult:
    predicted_index: int
    confidence: float
    score_pairs: List[tuple[int, float]]


class EmotionPredictor:
    def __init__(
        self,
        model_dir: Path,
        model_name: str = "beomi/KcELECTRA-base",
        max_length: int = 128,
    ) -> None:
        self.model_dir = model_dir
        self.model_name = model_name
        self.max_length = max_length
        self.device = torch.device("cuda" if torch.cuda.is_available() else "cpu")
        self._tokenizer: Optional[AutoTokenizer] = None
        self._model: Optional[AutoModelForSequenceClassification] = None

    def load(self) -> None:
        if self._tokenizer is not None and self._model is not None:
            return

        source = str(self.model_dir) if self.model_dir.exists() else self.model_name
        self._tokenizer = AutoTokenizer.from_pretrained(source)
        self._model = AutoModelForSequenceClassification.from_pretrained(source)
        self._model.to(self.device)
        self._model.eval()

    def predict(self, text: str, top_k: int = 3) -> PredictionResult:
        self.load()
        assert self._tokenizer is not None
        assert self._model is not None

        encoded = self._tokenizer(
            text,
            truncation=True,
            padding="max_length",
            max_length=self.max_length,
            return_tensors="pt",
        )
        encoded = {key: value.to(self.device) for key, value in encoded.items()}

        with torch.no_grad():
            logits = self._model(**encoded).logits
            probabilities = torch.softmax(logits, dim=-1).squeeze(0)

        values, indices = torch.topk(probabilities, k=min(top_k, probabilities.shape[-1]))
        score_pairs = [
            (int(class_index.item()), float(score.item()))
            for class_index, score in zip(indices, values)
        ]
        predicted_index, confidence = score_pairs[0]

        return PredictionResult(
            predicted_index=predicted_index,
            confidence=confidence,
            score_pairs=score_pairs,
        )

    def model_dir_resolved(self) -> Path:
        return self.model_dir.expanduser().resolve()

    def model_load_source(self) -> str:
        resolved_dir = self.model_dir_resolved()
        return str(resolved_dir) if resolved_dir.exists() else self.model_name


def build_predictor() -> EmotionPredictor:
    model_dir = Path(
        os.getenv(
            "EMOTION_MODEL_DIR",
            "ai-api-fastapi/training/emotion_classifier/artifacts/best",
        )
    )
    model_name = os.getenv("EMOTION_MODEL_NAME", "beomi/KcELECTRA-base")
    max_length = int(os.getenv("EMOTION_MODEL_MAX_LENGTH", "128"))
    return EmotionPredictor(model_dir=model_dir, model_name=model_name, max_length=max_length)
