# 학습된 감정분류 모델의 검증 지표를 확인하는 평가 스크립트 스켈레톤입니다.
# 감정 분류 평가 산출물 생성 스크립트입니다.
from __future__ import annotations

import argparse
import json
from pathlib import Path

import pandas as pd
from sklearn.metrics import classification_report, confusion_matrix, f1_score
from transformers import AutoConfig, pipeline


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser()
    parser.add_argument("--model-dir", required=True)
    parser.add_argument("--input-csv", required=True)
    parser.add_argument("--output-json", required=True)
    return parser.parse_args()


def main() -> None:
    args = parse_args()
    dataframe = pd.read_csv(args.input_csv, encoding="utf-8-sig")
    model_config = AutoConfig.from_pretrained(args.model_dir)
    classifier = pipeline(
        "text-classification",
        model=args.model_dir,
        tokenizer=args.model_dir,
        top_k=1
    )

    predictions = []
    for text in dataframe["text"].tolist():
        result = classifier(text, truncation=True, max_length=128)[0][0]
        predictions.append(result["label"])

    labels = dataframe["service_label"].tolist()
    configured_labels = [
        label
        for _, label in sorted(model_config.id2label.items(), key=lambda item: int(item[0]))
    ]
    evaluation_labels = [
        label
        for label in configured_labels
        if label in set(labels) or label in set(predictions)
    ]
    report = classification_report(
        labels,
        predictions,
        labels=evaluation_labels,
        target_names=evaluation_labels,
        output_dict=True,
        zero_division=0
    )
    matrix = confusion_matrix(labels, predictions, labels=evaluation_labels)
    focused_metrics = {}
    if "HAPPY" in evaluation_labels and "CALM" in evaluation_labels:
        happy_to_calm_count = sum(
            1 for gold, pred in zip(labels, predictions)
            if gold == "HAPPY" and pred == "CALM"
        )
        calm_to_happy_count = sum(
            1 for gold, pred in zip(labels, predictions)
            if gold == "CALM" and pred == "HAPPY"
        )
        happy_support = sum(1 for label in labels if label == "HAPPY")
        calm_support = sum(1 for label in labels if label == "CALM")
        happy_calm_pairs = [
            (gold, pred)
            for gold, pred in zip(labels, predictions)
            if gold in {"HAPPY", "CALM"}
        ]
        focused_metrics = {
            "happy_calm_macro_f1": (
                f1_score(
                    [gold for gold, _ in happy_calm_pairs],
                    [pred for _, pred in happy_calm_pairs],
                    labels=["HAPPY", "CALM"],
                    average="macro",
                    zero_division=0
                )
                if happy_calm_pairs
                else 0.0
            ),
            "happy_to_calm_count": happy_to_calm_count,
            "happy_to_calm_rate": (
                happy_to_calm_count / happy_support if happy_support else 0.0
            ),
            "calm_to_happy_count": calm_to_happy_count,
            "calm_to_happy_rate": (
                calm_to_happy_count / calm_support if calm_support else 0.0
            )
        }
        happy_guard_score = 1.0 - focused_metrics["happy_to_calm_rate"]
        calm_guard_score = 1.0 - focused_metrics["calm_to_happy_rate"]
        balanced_guard_score = (happy_guard_score + calm_guard_score) / 2.0
        focused_metrics["happy_calm_balanced_guard_score"] = (
            2.0
            * focused_metrics["happy_calm_macro_f1"]
            * balanced_guard_score
            / (focused_metrics["happy_calm_macro_f1"] + balanced_guard_score)
            if (focused_metrics["happy_calm_macro_f1"] + balanced_guard_score) > 0
            else 0.0
        )

    output = {
        "evaluated_labels": evaluation_labels,
        "classification_report": report,
        "confusion_matrix": matrix.tolist(),
        "focused_metrics": focused_metrics
    }

    output_path = Path(args.output_json)
    output_path.parent.mkdir(parents=True, exist_ok=True)
    with output_path.open("w", encoding="utf-8") as file:
        json.dump(output, file, ensure_ascii=False, indent=2)


if __name__ == "__main__":
    main()
