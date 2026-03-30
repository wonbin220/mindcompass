# 감정분류 모델의 내부 라벨과 서비스 라벨 규칙을 관리하는 유틸 파일입니다.
from __future__ import annotations

from pathlib import Path
import json
from typing import Dict, List


DEFAULT_LABEL_MAP = {
    "id_to_label": {
        "0": "HAPPY",
        "1": "CALM",
        "2": "ANXIOUS",
        "3": "SAD",
        "4": "ANGRY",
        "5": "TIRED",
    },
    "label_to_tags": {
        "HAPPY": ["HAPPY"],
        "CALM": ["CALM", "RELIEVED"],
        "ANXIOUS": ["ANXIOUS", "OVERWHELMED"],
        "SAD": ["SAD", "LONELY"],
        "ANGRY": ["ANGRY"],
        "TIRED": ["TIRED", "NUMB"],
    },
}


def load_label_map(path: Path) -> Dict[str, Dict[str, List[str]]]:
    if not path.exists():
        return DEFAULT_LABEL_MAP

    with path.open("r", encoding="utf-8") as file:
        return json.load(file)


def resolve_label(label_map: Dict[str, Dict[str, List[str]]], class_id: int) -> str:
    return label_map["id_to_label"].get(str(class_id), "CALM")


def resolve_tags(label_map: Dict[str, Dict[str, List[str]]], label: str) -> List[str]:
    return label_map["label_to_tags"].get(label, [label])
