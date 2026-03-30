# TIRED 관련 평가 JSON들을 모아서 자동 요약과 다음 권고안을 생성하는 스크립트입니다.
from __future__ import annotations

import argparse
import json
from pathlib import Path


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser()
    parser.add_argument("--evaluation-dir", required=True)
    parser.add_argument("--output-md", required=True)
    parser.add_argument("--output-json", required=True)
    return parser.parse_args()


def read_metrics(path: Path) -> dict:
    data = json.loads(path.read_text(encoding="utf-8"))
    report = data["classification_report"]
    return {
        "file": str(path),
        "name": path.name,
        "accuracy": report["accuracy"],
        "macro_f1": report["macro avg"]["f1-score"],
        "weighted_f1": report["weighted avg"]["f1-score"],
        "tired_precision": report["TIRED"]["precision"],
        "tired_recall": report["TIRED"]["recall"],
        "tired_f1": report["TIRED"]["f1-score"],
        "calm_f1": report["CALM"]["f1-score"],
        "sad_f1": report["SAD"]["f1-score"],
    }


def build_recommendation(rows: list[dict]) -> str:
    if not rows:
        return "no-data"

    best_tired = max(rows, key=lambda row: row["tired_f1"])
    best_macro = max(rows, key=lambda row: row["macro_f1"])

    if best_tired["tired_f1"] == 0.0:
        return (
            "blind-expansion-stop: all current tired experiments still have TIRED F1=0.0, "
            "so further unattended keyword expansion should pause and fallback-first handling should stay in place."
        )

    return (
        "continue-best-seed: at least one tired experiment recovered non-zero TIRED F1, "
        f"so continue from {best_tired['name']} while tracking macro F1 leader {best_macro['name']}."
    )


def write_markdown(rows: list[dict], recommendation: str, output_path: Path) -> None:
    lines = [
        "# TIRED Experiment Summary",
        "",
        f"- recommendation: `{recommendation}`",
        "",
        "| run | accuracy | macro_f1 | tired_f1 | tired_recall | file |",
        "| --- | ---: | ---: | ---: | ---: | --- |",
    ]
    for row in rows:
        lines.append(
            f"| {row['name']} | {row['accuracy']:.4f} | {row['macro_f1']:.4f} | {row['tired_f1']:.4f} | {row['tired_recall']:.4f} | {row['file']} |"
        )
    output_path.write_text("\n".join(lines) + "\n", encoding="utf-8")


def main() -> None:
    args = parse_args()
    evaluation_dir = Path(args.evaluation_dir)
    rows = [
        read_metrics(path)
        for path in sorted(evaluation_dir.glob("valid_metrics_cpu_compare_tired*.json"))
    ]
    rows.sort(key=lambda row: (row["macro_f1"], row["accuracy"]), reverse=True)
    recommendation = build_recommendation(rows)

    output_json = Path(args.output_json)
    output_json.parent.mkdir(parents=True, exist_ok=True)
    output_json.write_text(
        json.dumps({"recommendation": recommendation, "rows": rows}, ensure_ascii=False, indent=2),
        encoding="utf-8",
    )

    output_md = Path(args.output_md)
    output_md.parent.mkdir(parents=True, exist_ok=True)
    write_markdown(rows, recommendation, output_md)

    print(f"rows={len(rows)}")
    print(f"recommendation={recommendation}")
    if rows:
        print(f"best_macro={rows[0]['name']}")
    print(f"output_md={output_md}")
    print(f"output_json={output_json}")


if __name__ == "__main__":
    main()
