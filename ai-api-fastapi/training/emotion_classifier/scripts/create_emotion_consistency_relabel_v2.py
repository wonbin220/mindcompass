# 감정 소분류와 본문 힌트로 보수적으로 재라벨링하는 비교용 스크립트입니다.
from __future__ import annotations

import argparse
from collections import Counter
from pathlib import Path

import pandas as pd


SAD_TEXT_HINTS = ("속상", "서운", "슬퍼", "외롭", "후회", "눈물", "위축", "실망", "힘이 빠")
ANGRY_BLOCK_HINTS = ("화가 나", "화나", "짜증", "억울", "분해", "구역질", "환멸", "악의적", "노여", "성가", "질투")
ANXIOUS_HEALTH_HINTS = ("병원", "건강", "치료", "검진", "암", "합병증", "수술", "혈액", "아프", "혼수", "응급")


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser()
    parser.add_argument("--input-csv", required=True)
    parser.add_argument("--output-csv", required=True)
    return parser.parse_args()


def relabel_row(row: pd.Series) -> tuple[str, str]:
    service_label = row["service_label"]
    emotion_minor = row["emotion_minor"]
    text = row["text"]

    if service_label == "SAD" and emotion_minor in {"충격 받은", "당황"}:
        return "ANXIOUS", "SAD_TO_ANXIOUS_MINOR_V2"

    if service_label == "ANGRY" and emotion_minor == "한심한":
        return "SAD", "ANGRY_TO_SAD_MINOR_V2"

    if service_label == "ANGRY" and emotion_minor == "억울한":
        if any(hint in text for hint in SAD_TEXT_HINTS):
            return "SAD", "ANGRY_TO_SAD_TEXT_V2"

    if service_label == "ANGRY" and emotion_minor in {"방어적인", "당황"}:
        if any(hint in text for hint in ANXIOUS_HEALTH_HINTS):
            return "ANXIOUS", "ANGRY_TO_ANXIOUS_HEALTH_V2"

    if service_label == "ANGRY" and any(hint in text for hint in ANXIOUS_HEALTH_HINTS):
        if any(hint in text for hint in ("막막", "걱정", "무섭", "놀랐", "불안")):
            if not any(hint in text for hint in ANGRY_BLOCK_HINTS):
                return "ANXIOUS", "ANGRY_TO_ANXIOUS_TEXT_V2"

    return service_label, ""


def main() -> None:
    args = parse_args()
    dataframe = pd.read_csv(args.input_csv, encoding="utf-8-sig")
    original_distribution = Counter(dataframe["service_label"].tolist())

    relabeled_labels = []
    relabel_reasons = []
    for _, row in dataframe.iterrows():
        new_label, reason = relabel_row(row)
        relabeled_labels.append(new_label)
        relabel_reasons.append(reason)

    dataframe["service_label"] = relabeled_labels
    dataframe["relabel_reason"] = relabel_reasons

    output_path = Path(args.output_csv)
    output_path.parent.mkdir(parents=True, exist_ok=True)
    dataframe.to_csv(output_path, index=False, encoding="utf-8-sig")

    updated_distribution = Counter(dataframe["service_label"].tolist())
    changed_count = sum(1 for reason in relabel_reasons if reason)
    reason_counts = Counter(reason for reason in relabel_reasons if reason)

    print(f"input={args.input_csv}")
    print(f"output={args.output_csv}")
    print(f"changed_count={changed_count}")
    print(f"reason_counts={dict(reason_counts)}")
    print(f"original_distribution={dict(original_distribution)}")
    print(f"updated_distribution={dict(updated_distribution)}")


if __name__ == "__main__":
    main()
