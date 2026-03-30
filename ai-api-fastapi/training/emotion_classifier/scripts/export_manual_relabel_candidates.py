# 수동 승인용 감정 재라벨 후보를 CSV로 내보내는 스크립트입니다.
from __future__ import annotations

import argparse
from pathlib import Path

import pandas as pd


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser()
    parser.add_argument("--input-csv", required=True)
    parser.add_argument("--output-csv", required=True)
    return parser.parse_args()


def build_candidate_mask(dataframe: pd.DataFrame) -> pd.Series:
    angry_to_sad = (
        (dataframe["service_label"] == "ANGRY")
        & (
            dataframe["emotion_minor"].isin(["한심한", "억울한", "툴툴대는", "방어적인"])
            | dataframe["text"].str.contains("속상|서운|슬퍼|비참|후회|답답|힘이 빠", na=False)
        )
    )
    angry_to_anxious = (
        (dataframe["service_label"] == "ANGRY")
        & (
            dataframe["emotion_minor"].isin(["방어적인", "당황"])
            | dataframe["text"].str.contains("놀랐|두려|막막|걱정|불안|무섭", na=False)
        )
        & dataframe["text"].str.contains("건강|병원|검사|치료|암|합병증|다칠", na=False)
    )
    sad_to_anxious = (
        (dataframe["service_label"] == "SAD")
        & (
            dataframe["emotion_minor"].isin(["충격 받은", "당황", "후회되는"])
            | dataframe["text"].str.contains("걱정|고민|막막|불안|무섭|충격|당황", na=False)
        )
    )
    return angry_to_sad | angry_to_anxious | sad_to_anxious


def build_candidate_bucket(row: pd.Series) -> str:
    if row["service_label"] == "ANGRY" and (
        row["emotion_minor"] in {"방어적인", "당황"}
        or any(keyword in row["text"] for keyword in ("놀랐", "두려", "막막", "걱정", "불안", "무섭"))
    ) and any(keyword in row["text"] for keyword in ("건강", "병원", "검사", "치료", "암", "합병증", "다칠")):
        return "ANGRY_TO_ANXIOUS"
    if row["service_label"] == "ANGRY":
        return "ANGRY_TO_SAD"
    return "SAD_TO_ANXIOUS"


def main() -> None:
    args = parse_args()
    dataframe = pd.read_csv(args.input_csv, encoding="utf-8-sig")
    candidates = dataframe.loc[build_candidate_mask(dataframe)].copy()
    candidates["candidate_bucket"] = candidates.apply(build_candidate_bucket, axis=1)
    candidates["approved_label"] = ""
    candidates["review_note"] = ""

    columns = [
        "sample_id",
        "candidate_bucket",
        "service_label",
        "approved_label",
        "emotion_major",
        "emotion_minor",
        "situation_keyword",
        "review_note",
        "text"
    ]
    candidates = candidates[columns].sort_values(
        by=["candidate_bucket", "emotion_minor", "sample_id"]
    )

    output_path = Path(args.output_csv)
    output_path.parent.mkdir(parents=True, exist_ok=True)
    candidates.to_csv(output_path, index=False, encoding="utf-8-sig")

    print(f"input={args.input_csv}")
    print(f"output={args.output_csv}")
    print(f"candidate_count={len(candidates)}")


if __name__ == "__main__":
    main()
