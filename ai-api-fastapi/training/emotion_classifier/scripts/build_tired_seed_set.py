# 현재 processed CSV에서 physical fatigue 중심 TIRED seed set을 재현 가능하게 생성하는 스크립트입니다.
from __future__ import annotations

import argparse
from pathlib import Path

import pandas as pd


STRICT_INCLUDE_PATTERNS = [
    "잠이 부족",
    "잠을 못 자",
    "잠을 잘 자지 못",
    "잠이 안 오",
    "잠이 오지",
    "불면증",
    "불면",
    "야근",
    "출퇴근",
    "퇴근하고 오면",
    "밤 새워",
    "잔업",
    "퇴근 후에도 업무",
    "기력이 없어",
    "기력이 없",
    "체력이 떨어",
    "체력 때문에",
    "몸이 무겁",
    "몸도 무겁",
    "몸이 붓",
    "현기증",
    "졸려",
    "녹초",
    "탈진",
    "코피",
]

STRICT_ANCHOR_PATTERNS = [
    "피곤",
    "지치",
    "졸려",
    "녹초",
    "탈진",
    "기력이 없",
    "체력이 떨어",
    "몸이 무겁",
    "현기증",
    "코피",
    "힘들어",
    "힘들고",
]

STRICT_EXCLUDE_PATTERNS = [
    "신나",
    "설레",
    "행복",
    "만족",
    "감사",
    "좋아",
    "어머니 드리려고",
    "어머니 생신",
    "유럽으로 여행",
    "결혼해",
    "화장실에서 잠들",
    "수면 내시경",
    "코골이",
    "위층 소음",
    "폭행",
    "때렸",
    "따돌림",
    "사기",
    "삼각관계",
]


# 수동 검토로 제외한 샘플들입니다. physical fatigue보다 다른 감정/맥락이 더 강한 경우만 배제합니다.
MANUAL_REJECT_SAMPLE_IDS = {
    "train-000365",
    "train-002616",
    "train-002712",
    "train-003417",
    "train-003647",
    "train-005661",
    "train-007099",
    "train-007296",
    "train-008860",
    "train-011636",
    "train-014697",
    "train-015107",
    "train-016794",
    "train-016918",
    "train-018052",
    "train-020353",
    "train-021087",
    "train-022710",
    "train-023195",
    "train-023616",
    "train-023898",
    "train-032805",
    "train-034867",
    "train-038945",
    "train-039814",
    "train-039993",
    "train-040119",
    "train-040183",
    "train-042698",
    "train-047224",
    "train-047416",
    "train-049945",
    "valid-001180",
    "valid-001640",
    "valid-002273",
    "valid-004376",
    "valid-006067",
    "valid-006190",
}


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser()
    parser.add_argument("--train-csv", required=True)
    parser.add_argument("--valid-csv", required=True)
    parser.add_argument("--output-dir", required=True)
    parser.add_argument("--version", choices=["manual_v1", "auto_v2"], default="manual_v1")
    return parser.parse_args()


def normalize_text(text: str) -> str:
    return " ".join((text or "").split())


def contains_any(text: str, patterns: list[str]) -> bool:
    return any(pattern in text for pattern in patterns)


def detect_strict_candidate(text: str) -> tuple[bool, str]:
    normalized = normalize_text(text)
    if not normalized:
        return False, "empty-text"

    if not contains_any(normalized, STRICT_INCLUDE_PATTERNS):
        return False, "missing-include-pattern"

    if not contains_any(normalized, STRICT_ANCHOR_PATTERNS):
        return False, "missing-anchor-pattern"

    if contains_any(normalized, STRICT_EXCLUDE_PATTERNS):
        return False, "matched-exclude-pattern"

    return True, "strict-physical-fatigue-candidate"


AUTO_V2_STRONG_PHYSICAL_PATTERNS = [
    "현기증",
    "코피",
    "녹초",
    "탈진",
    "몸이 무겁",
    "몸도 무겁",
    "기력이 없어",
    "기력이 없",
    "체력이 떨어",
    "쉽게 피곤",
    "피로감이 심",
    "아침에 일어나기도 힘들",
    "몸이 축 쳐",
    "몽롱",
]

AUTO_V2_CONTEXT_PATTERNS = [
    "잠이 부족",
    "잠을 못 자",
    "잠을 잘 자지 못",
    "잠이 안 오",
    "잠이 오지 않",
    "불면증",
    "불면",
    "날밤",
    "밤새",
    "야근",
    "잔업",
    "출퇴근",
    "퇴근하고 오면",
    "과중한 업무",
    "업무량",
    "밤 새워",
]

AUTO_V2_FATIGUE_ANCHORS = [
    "피곤",
    "지치",
    "힘들어",
    "힘들고",
    "졸려",
    "기력이 없",
    "체력이 떨어",
    "몸이 무겁",
    "현기증",
    "코피",
    "녹초",
    "탈진",
    "몽롱",
]

AUTO_V2_EXCLUDE_PATTERNS = [
    "죽고 싶",
    "우울증",
    "배신",
    "질투",
    "죄책감",
    "헤어지",
    "버려진",
    "외로워",
    "상처받",
    "너무나 불안",
    "미래가 불안",
    "시험 날짜",
    "딸 때문에",
    "아들 얼굴",
    "장모님",
    "와이프",
    "남편 얼굴",
    "승진",
    "축하",
    "기뻐",
    "좋아",
    "감사",
]


def detect_auto_v2_candidate(text: str) -> tuple[bool, str]:
    normalized = normalize_text(text)
    if not normalized:
        return False, "empty-text"

    if contains_any(normalized, AUTO_V2_STRONG_PHYSICAL_PATTERNS):
        if contains_any(normalized, AUTO_V2_EXCLUDE_PATTERNS):
            return False, "auto-v2-strong-physical-but-excluded"
        return True, "auto-v2-strong-physical-fatigue"

    has_context = contains_any(normalized, AUTO_V2_CONTEXT_PATTERNS)
    has_anchor = contains_any(normalized, AUTO_V2_FATIGUE_ANCHORS)
    has_excluded = contains_any(normalized, AUTO_V2_EXCLUDE_PATTERNS)
    if has_context and has_anchor and not has_excluded:
        return True, "auto-v2-context-plus-fatigue"

    return False, "auto-v2-missing-signal"


def build_seed_frame(
    dataframe: pd.DataFrame,
    version: str,
) -> tuple[pd.DataFrame, pd.DataFrame, pd.DataFrame]:
    candidate_flags: list[bool] = []
    candidate_reasons: list[str] = []
    manual_keep_flags: list[bool] = []
    manual_notes: list[str] = []

    for sample_id, text in zip(
        dataframe["sample_id"].astype(str),
        dataframe["text"].fillna("").astype(str),
    ):
        if version == "auto_v2":
            is_candidate, reason = detect_auto_v2_candidate(text)
        else:
            is_candidate, reason = detect_strict_candidate(text)
        candidate_flags.append(is_candidate)
        candidate_reasons.append(reason)

        if not is_candidate:
            manual_keep_flags.append(False)
            manual_notes.append("")
            continue

        if version == "manual_v1" and sample_id in MANUAL_REJECT_SAMPLE_IDS:
            manual_keep_flags.append(False)
            manual_notes.append("manual-reject-non-physical-or-mixed-context")
            continue

        manual_keep_flags.append(True)
        if version == "auto_v2":
            manual_notes.append("auto-keep-physical-fatigue-seed")
        else:
            manual_notes.append("manual-keep-physical-fatigue-seed")

    review_frame = dataframe.copy()
    review_frame["tired_seed_candidate"] = candidate_flags
    review_frame["tired_seed_candidate_reason"] = candidate_reasons
    review_frame["tired_seed_manual_keep"] = manual_keep_flags
    review_frame["tired_seed_manual_note"] = manual_notes
    review_frame["service_label_original"] = review_frame["service_label"]

    candidate_frame = review_frame.loc[review_frame["tired_seed_candidate"]].copy()
    seed_frame = candidate_frame.loc[candidate_frame["tired_seed_manual_keep"]].copy()
    seed_frame["service_label"] = "TIRED"
    seed_frame["tired_seed_version"] = version
    merged_frame = review_frame.copy()
    merged_frame["tired_seed_version"] = version
    merged_frame["tired_seed_applied"] = merged_frame["tired_seed_manual_keep"]
    merged_frame.loc[merged_frame["tired_seed_applied"], "service_label"] = "TIRED"
    return candidate_frame, seed_frame, merged_frame


def save_outputs(
    input_name: str,
    candidate_frame: pd.DataFrame,
    seed_frame: pd.DataFrame,
    merged_frame: pd.DataFrame,
    output_dir: Path,
) -> None:
    stem = Path(input_name).stem
    version_suffix = str(seed_frame["tired_seed_version"].iloc[0] if len(seed_frame) > 0 else merged_frame["tired_seed_version"].iloc[0])
    candidate_path = output_dir / f"{stem}_tired_seed_candidates_{version_suffix}.csv"
    seed_path = output_dir / f"{stem}_tired_seed_{version_suffix}.csv"
    merged_path = output_dir / f"{stem}_tired_seed_{version_suffix}_merged.csv"
    candidate_frame.to_csv(candidate_path, index=False, encoding="utf-8-sig")
    seed_frame.to_csv(seed_path, index=False, encoding="utf-8-sig")
    merged_frame.to_csv(merged_path, index=False, encoding="utf-8-sig")
    print(f"candidate_output={candidate_path}")
    print(f"seed_output={seed_path}")
    print(f"merged_output={merged_path}")
    print(f"candidate_rows={len(candidate_frame)}")
    print(f"seed_rows={len(seed_frame)}")
    if len(seed_frame) > 0:
        print(seed_frame["service_label_original"].value_counts().to_string())
    print("merged_service_label_distribution")
    print(merged_frame["service_label"].value_counts().to_string())


def main() -> None:
    args = parse_args()
    output_dir = Path(args.output_dir)
    output_dir.mkdir(parents=True, exist_ok=True)

    for input_csv in [args.train_csv, args.valid_csv]:
        dataframe = pd.read_csv(input_csv, encoding="utf-8-sig")
        candidate_frame, seed_frame, merged_frame = build_seed_frame(dataframe, args.version)
        print(f"input={input_csv}")
        save_outputs(Path(input_csv).name, candidate_frame, seed_frame, merged_frame, output_dir)


if __name__ == "__main__":
    main()
