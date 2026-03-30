# 감정 대분류와 서비스 라벨 충돌을 완화하는 비교용 재라벨링 스크립트입니다.
from __future__ import annotations

import argparse
from collections import Counter
from pathlib import Path

import pandas as pd


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser()
    parser.add_argument("--input-csv", required=True)
    parser.add_argument("--output-csv", required=True)
    return parser.parse_args()


def relabel_row(row: pd.Series) -> tuple[str, str]:
    service_label = row["service_label"]
    emotion_major = row["emotion_major"]

    if service_label == "ANGRY" and emotion_major in {"상처", "슬픔"}:
        return "SAD", "ANGRY_TO_SAD_BY_MAJOR"
    if service_label == "ANGRY" and emotion_major == "당황":
        return "ANXIOUS", "ANGRY_TO_ANXIOUS_BY_MAJOR"
    if service_label == "SAD" and emotion_major == "분노":
        return "ANGRY", "SAD_TO_ANGRY_BY_MAJOR"
    if service_label == "SAD" and emotion_major == "당황":
        return "ANXIOUS", "SAD_TO_ANXIOUS_BY_MAJOR"
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

    print(f"input={args.input_csv}")
    print(f"output={args.output_csv}")
    print(f"changed_count={changed_count}")
    print(f"original_distribution={dict(original_distribution)}")
    print(f"updated_distribution={dict(updated_distribution)}")


if __name__ == "__main__":
    main()
