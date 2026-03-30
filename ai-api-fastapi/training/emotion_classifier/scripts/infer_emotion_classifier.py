# 학습된 감정분류 모델을 단건 텍스트로 빠르게 확인하는 추론 스크립트 스켈레톤입니다.
from __future__ import annotations

import argparse

from transformers import pipeline


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser()
    parser.add_argument("--model-dir", required=True)
    parser.add_argument("--text", required=True)
    return parser.parse_args()


def main() -> None:
    args = parse_args()
    classifier = pipeline(
        "text-classification",
        model=args.model_dir,
        tokenizer=args.model_dir,
        top_k=3
    )
    results = classifier(args.text, truncation=True, max_length=128)[0]
    for item in results:
        print(f"{item['label']}: {item['score']:.4f}")


if __name__ == "__main__":
    main()
