# HAPPY/CALM 경계 후보를 baseline 예측 기반으로 추출하는 스크립트입니다.
from __future__ import annotations

import argparse
from pathlib import Path

import pandas as pd
from transformers import pipeline


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser()
    parser.add_argument("--input-csv", required=True)
    parser.add_argument("--model-dir", required=True)
    parser.add_argument("--output-csv", required=True)
    parser.add_argument("--source-label", default="CALM")
    parser.add_argument("--target-label", default="HAPPY")
    parser.add_argument("--min-score", type=float, default=0.37)
    parser.add_argument("--top-k", type=int, default=200)
    return parser.parse_args()


def count_keyword_hits(text: str, keywords: list[str]) -> int:
    return sum(1 for keyword in keywords if keyword in text)


def main() -> None:
    args = parse_args()
    dataframe = pd.read_csv(args.input_csv, encoding="utf-8-sig")
    source_df = dataframe[dataframe["service_label"] == args.source_label].copy()

    classifier = pipeline(
        "text-classification",
        model=args.model_dir,
        tokenizer=args.model_dir,
        top_k=1
    )

    joy_keywords = [
        "기뻐",
        "기쁘",
        "행복",
        "신나",
        "즐거",
        "뿌듯",
        "반갑",
        "감동",
        "기대돼",
        "기대되",
        "기분이 좋",
        "좋아"
    ]
    calm_keywords = [
        "편해",
        "편안",
        "안심",
        "안도",
        "느긋",
        "마음이 편",
        "긴장을 풀",
        "홀가분"
    ]

    predicted_labels: list[str] = []
    predicted_scores: list[float] = []
    joy_hits: list[int] = []
    calm_hits: list[int] = []
    for text in source_df["text"].fillna("").tolist():
        result = classifier(text, truncation=True, max_length=128)[0][0]
        predicted_labels.append(result["label"])
        predicted_scores.append(float(result["score"]))
        joy_hits.append(count_keyword_hits(text, joy_keywords))
        calm_hits.append(count_keyword_hits(text, calm_keywords))

    source_df["predicted_label"] = predicted_labels
    source_df["predicted_score"] = predicted_scores
    source_df["joy_signal_count"] = joy_hits
    source_df["calm_signal_count"] = calm_hits

    candidates = source_df[
        (source_df["predicted_label"] == args.target_label)
        & (source_df["predicted_score"] >= args.min_score)
    ].copy()
    candidates["candidate_bucket"] = f"{args.source_label}_TO_{args.target_label}_BOUNDARY"
    candidates["approved_label"] = ""
    candidates["review_note"] = ""

    sort_columns = [
        "predicted_score",
        "joy_signal_count",
        "calm_signal_count",
        "sample_id"
    ]
    candidates = candidates.sort_values(
        by=sort_columns,
        ascending=[False, False, True, True]
    ).head(args.top_k)

    output_columns = [
        "sample_id",
        "candidate_bucket",
        "service_label",
        "approved_label",
        "emotion_major",
        "emotion_minor",
        "predicted_label",
        "predicted_score",
        "joy_signal_count",
        "calm_signal_count",
        "review_note",
        "text"
    ]
    candidates = candidates[output_columns]

    output_path = Path(args.output_csv)
    output_path.parent.mkdir(parents=True, exist_ok=True)
    candidates.to_csv(output_path, index=False, encoding="utf-8-sig")

    print(f"input={args.input_csv}")
    print(f"model_dir={args.model_dir}")
    print(f"output={args.output_csv}")
    print(f"candidate_count={len(candidates)}")


if __name__ == "__main__":
    main()
