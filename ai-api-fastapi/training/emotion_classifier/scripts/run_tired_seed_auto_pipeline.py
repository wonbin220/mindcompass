# TIRED seed 생성부터 balanced compare 학습/평가까지 자동으로 재현하는 파이프라인 스크립트입니다.
from __future__ import annotations

import argparse
import subprocess
import sys
from pathlib import Path

import pandas as pd


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser()
    parser.add_argument("--seed-version", choices=["manual_v1", "auto_v2"], default="auto_v2")
    parser.add_argument("--run-name", required=True)
    return parser.parse_args()


def run_command(command: list[str]) -> None:
    print("running:", " ".join(command))
    subprocess.run(command, check=True)


def build_balanced_compare(seed_dir: Path, version: str) -> tuple[Path, Path, int, int]:
    train_merged = seed_dir / f"train_emotion_mvp_tired_seed_{version}_merged.csv"
    valid_merged = seed_dir / f"valid_emotion_mvp_tired_seed_{version}_merged.csv"

    train = pd.read_csv(train_merged, encoding="utf-8-sig")
    valid = pd.read_csv(valid_merged, encoding="utf-8-sig")

    labels = ["ANGRY", "ANXIOUS", "CALM", "HAPPY", "SAD", "TIRED"]
    train_n = min(train["service_label"].value_counts()[label] for label in labels)
    valid_n = min(valid["service_label"].value_counts()[label] for label in labels)

    train_balanced = pd.concat(
        [group.sample(n=train_n, random_state=42) for label, group in train.groupby("service_label") if label in labels],
        ignore_index=True,
    ).sample(frac=1, random_state=42).reset_index(drop=True)
    valid_balanced = pd.concat(
        [group.sample(n=valid_n, random_state=42) for label, group in valid.groupby("service_label") if label in labels],
        ignore_index=True,
    ).sample(frac=1, random_state=42).reset_index(drop=True)

    train_output = seed_dir / f"train_emotion_mvp_tired_seed_{version}_cpu_compare.csv"
    valid_output = seed_dir / f"valid_emotion_mvp_tired_seed_{version}_cpu_compare.csv"
    train_balanced.to_csv(train_output, index=False, encoding="utf-8-sig")
    valid_balanced.to_csv(valid_output, index=False, encoding="utf-8-sig")

    print(f"train_n_per_label={train_n}")
    print(f"valid_n_per_label={valid_n}")
    print(f"train_rows={len(train_balanced)}")
    print(f"valid_rows={len(valid_balanced)}")
    return train_output, valid_output, train_n, valid_n


def main() -> None:
    args = parse_args()
    script_dir = Path(__file__).resolve().parent
    training_dir = script_dir.parent
    project_root = training_dir.parents[2]

    train_csv = training_dir / "processed" / "train_emotion_mvp.csv"
    valid_csv = training_dir / "processed" / "valid_emotion_mvp.csv"
    seed_dir = training_dir / "processed" / f"tired_seed_{args.seed_version}"
    artifacts_dir = training_dir / "artifacts" / args.run_name
    evaluation_path = training_dir / "artifacts" / "evaluation" / f"valid_metrics_{args.run_name}.json"

    build_script = script_dir / "build_tired_seed_set.py"
    train_script = script_dir / "train_emotion_classifier.py"
    evaluate_script = script_dir / "evaluate_emotion_classifier.py"
    summarize_script = script_dir / "summarize_tired_experiments.py"
    config_path = training_dir / "configs" / "training_config_cpu.json"
    label_map_path = training_dir / "configs" / "label_map.json"
    evaluation_dir = training_dir / "artifacts" / "evaluation"

    run_command(
        [
            sys.executable,
            str(build_script),
            "--train-csv",
            str(train_csv),
            "--valid-csv",
            str(valid_csv),
            "--output-dir",
            str(seed_dir),
            "--version",
            args.seed_version,
        ]
    )

    train_compare, valid_compare, _, _ = build_balanced_compare(seed_dir, args.seed_version)

    run_command(
        [
            sys.executable,
            str(train_script),
            "--train-csv",
            str(train_compare),
            "--valid-csv",
            str(valid_compare),
            "--config",
            str(config_path),
            "--label-map",
            str(label_map_path),
            "--output-dir",
            str(artifacts_dir),
        ]
    )

    run_command(
        [
            sys.executable,
            str(evaluate_script),
            "--model-dir",
            str(artifacts_dir / "best"),
            "--input-csv",
            str(valid_compare),
            "--output-json",
            str(evaluation_path),
        ]
    )

    run_command(
        [
            sys.executable,
            str(summarize_script),
            "--evaluation-dir",
            str(evaluation_dir),
            "--output-md",
            str(evaluation_dir / "tired_experiment_summary.md"),
            "--output-json",
            str(evaluation_dir / "tired_experiment_summary.json"),
        ]
    )

    print(f"seed_dir={seed_dir}")
    print(f"artifacts_dir={artifacts_dir}")
    print(f"evaluation_json={evaluation_path}")
    print(f"summary_md={evaluation_dir / 'tired_experiment_summary.md'}")
    print(f"summary_json={evaluation_dir / 'tired_experiment_summary.json'}")
    print(f"workspace_root={project_root}")


if __name__ == "__main__":
    main()
