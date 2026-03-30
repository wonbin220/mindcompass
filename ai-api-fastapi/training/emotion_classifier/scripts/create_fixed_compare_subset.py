# 기준 compare subset sample_id를 유지하면서 relabel 데이터셋에 동일 샘플만 다시 투영하는 스크립트입니다.
from __future__ import annotations

import argparse
from pathlib import Path

import pandas as pd


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser()
    parser.add_argument("--reference-csv", required=True)
    parser.add_argument("--source-csv", required=True)
    parser.add_argument("--output-csv", required=True)
    return parser.parse_args()


def main() -> None:
    args = parse_args()
    reference_df = pd.read_csv(args.reference_csv, encoding="utf-8-sig")
    source_df = pd.read_csv(args.source_csv, encoding="utf-8-sig")

    source_by_id = source_df.set_index("sample_id")
    missing_ids = [
        sample_id
        for sample_id in reference_df["sample_id"].tolist()
        if sample_id not in source_by_id.index
    ]
    if missing_ids:
        preview = ", ".join(missing_ids[:10])
        raise ValueError(f"missing sample_id in source csv: {preview}")

    output_df = source_by_id.loc[reference_df["sample_id"].tolist()].reset_index()

    output_path = Path(args.output_csv)
    output_path.parent.mkdir(parents=True, exist_ok=True)
    output_df.to_csv(output_path, index=False, encoding="utf-8-sig")

    changed_mask = (
        reference_df.set_index("sample_id")["service_label"]
        != output_df.set_index("sample_id")["service_label"]
    )
    changed_ids = changed_mask[changed_mask].index.tolist()

    print(f"reference={args.reference_csv}")
    print(f"source={args.source_csv}")
    print(f"output={args.output_csv}")
    print(f"row_count={len(output_df)}")
    print(f"changed_count={len(changed_ids)}")
    if changed_ids:
        print(f"changed_preview={','.join(changed_ids[:10])}")


if __name__ == "__main__":
    main()
