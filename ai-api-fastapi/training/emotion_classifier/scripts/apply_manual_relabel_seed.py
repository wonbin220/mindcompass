# 승인된 수동 재라벨 seed를 원본 CSV에 적용하는 스크립트입니다.
from __future__ import annotations

import argparse
from pathlib import Path

import pandas as pd


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser()
    parser.add_argument("--input-csv", required=True)
    parser.add_argument("--seed-csv", required=True)
    parser.add_argument("--output-csv", required=True)
    return parser.parse_args()


def main() -> None:
    args = parse_args()
    dataframe = pd.read_csv(args.input_csv, encoding="utf-8-sig")
    seed_dataframe = pd.read_csv(args.seed_csv, encoding="utf-8-sig")
    seed_dataframe = seed_dataframe.dropna(subset=["sample_id", "approved_label"])
    seed_dataframe = seed_dataframe[seed_dataframe["approved_label"].astype(str).str.strip() != ""].copy()

    approved_map = dict(zip(seed_dataframe["sample_id"], seed_dataframe["approved_label"]))
    note_map = dict(zip(seed_dataframe["sample_id"], seed_dataframe.get("review_note", "")))

    dataframe["manual_relabel_reason"] = ""
    changed_count = 0
    for index, row in dataframe.iterrows():
        sample_id = row["sample_id"]
        if sample_id not in approved_map:
            continue
        new_label = approved_map[sample_id]
        dataframe.at[index, "service_label"] = new_label
        dataframe.at[index, "manual_relabel_reason"] = note_map.get(sample_id, "")
        changed_count += 1

    output_path = Path(args.output_csv)
    output_path.parent.mkdir(parents=True, exist_ok=True)
    dataframe.to_csv(output_path, index=False, encoding="utf-8-sig")

    print(f"input={args.input_csv}")
    print(f"seed={args.seed_csv}")
    print(f"output={args.output_csv}")
    print(f"changed_count={changed_count}")


if __name__ == "__main__":
    main()
