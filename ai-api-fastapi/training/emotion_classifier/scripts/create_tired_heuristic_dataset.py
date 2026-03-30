# 현재 processed CSV에서 TIRED heuristic 실험용 별도 CSV를 생성하는 스크립트입니다.
from __future__ import annotations

import argparse
from pathlib import Path

import pandas as pd


V1_TIRED_KEYWORDS = [
    "피곤",
    "지치",
    "녹초",
    "탈진",
]

V1_PHYSICAL_CONTEXT_KEYWORDS = [
    "잠",
    "수면",
    "졸려",
    "몸",
    "체력",
    "기력",
    "컨디션",
    "휴식",
    "쉬",
    "푹 잘",
    "야근",
    "퇴근",
    "출퇴근",
    "업무",
    "근무",
]

V1_NEGATED_TIRED_PATTERNS = [
    "피곤하지 않",
    "피곤한 줄 모르",
    "지치지 않",
    "안 피곤",
]

V1_DISTRESS_KEYWORDS = [
    "우울",
    "슬프",
    "눈물",
    "외롭",
    "불안",
    "걱정",
    "화가",
    "짜증",
    "분노",
    "괴롭",
    "속상",
    "충격",
    "배신",
    "실망",
    "죽고 싶",
    "혼란",
    "초조",
    "두려",
    "환멸",
    "억울",
    "고립",
    "한심",
    "부끄",
    "염세",
]

V2_PRIMARY_PATTERNS = [
    "잠이 부족",
    "잠을 잘 자지 못",
    "잠을 못 자",
    "잠이 오",
    "잠이 안 오",
    "잠들",
    "불면",
    "야근",
    "퇴근하고 오면",
    "출퇴근",
    "녹초",
    "탈진",
    "현기증",
    "코피",
    "기력이 없어",
    "기력이 없",
    "체력이 떨어",
    "체력 때문에",
    "몸이 붓",
    "몸이 무겁",
    "몸이 아프",
    "몸이 천근만근",
    "몸이 가뿐",
    "졸려",
]

V2_FATIGUE_PATTERNS = [
    "피곤",
    "지치",
    "녹초",
    "탈진",
    "기력이 없",
]

V2_REQUIRED_SUPPORT_PATTERNS = [
    "잠",
    "수면",
    "졸려",
    "야근",
    "퇴근",
    "출퇴근",
    "업무",
    "근무",
    "몸",
    "체력",
    "기력",
    "건강",
    "현기증",
    "코피",
    "휴식",
    "쉬",
]

V2_NEGATED_PATTERNS = V1_NEGATED_TIRED_PATTERNS + [
    "피곤하긴 한데",
    "피곤했는데 어젯밤엔 푹 잘 잤",
]

V2_EXCLUDED_CONTEXT_PATTERNS = V1_DISTRESS_KEYWORDS + [
    "외롭",
    "속상",
    "미안",
    "질투",
    "배신",
    "관계",
    "친구",
    "남편",
    "아내",
    "자식",
    "엄마",
    "아빠",
    "딸",
    "아들",
    "상사",
    "과장님",
    "부장님",
    "학교",
    "폭력",
    "맞았",
    "따돌림",
    "실패",
    "후회",
]


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser()
    parser.add_argument("--input-csv", required=True)
    parser.add_argument("--output-csv", required=True)
    parser.add_argument("--version", choices=["v1", "v2"], default="v1")
    return parser.parse_args()


def contains_any(text: str, keywords: list[str]) -> bool:
    return any(keyword in text for keyword in keywords)


def classify_tired_heuristic_v1(text: str) -> tuple[bool, str]:
    normalized = " ".join((text or "").split())
    if not normalized:
        return False, ""

    if contains_any(normalized, V1_NEGATED_TIRED_PATTERNS):
        return False, "negated-fatigue"

    if not contains_any(normalized, V1_TIRED_KEYWORDS):
        return False, "missing-fatigue-keyword"

    if not contains_any(normalized, V1_PHYSICAL_CONTEXT_KEYWORDS):
        return False, "missing-physical-context"

    if contains_any(normalized, V1_DISTRESS_KEYWORDS):
        return False, "contains-distress-keyword"

    return True, "fatigue+physical-context"


def classify_tired_heuristic_v2(text: str) -> tuple[bool, str]:
    normalized = " ".join((text or "").split())
    if not normalized:
        return False, ""

    if contains_any(normalized, V2_NEGATED_PATTERNS):
        return False, "v2-negated-fatigue"

    if contains_any(normalized, V2_EXCLUDED_CONTEXT_PATTERNS):
        return False, "v2-excluded-context"

    has_primary_pattern = contains_any(normalized, V2_PRIMARY_PATTERNS)
    has_fatigue_pattern = contains_any(normalized, V2_FATIGUE_PATTERNS)
    has_support_pattern = contains_any(normalized, V2_REQUIRED_SUPPORT_PATTERNS)

    if has_primary_pattern:
        return True, "v2-primary-physical-fatigue"

    if has_fatigue_pattern and has_support_pattern:
        return True, "v2-fatigue+support-context"

    return False, "v2-missing-physical-fatigue-signal"


def classify_tired_heuristic(text: str, version: str) -> tuple[bool, str]:
    if version == "v2":
        return classify_tired_heuristic_v2(text)
    return classify_tired_heuristic_v1(text)


def main() -> None:
    args = parse_args()
    dataframe = pd.read_csv(args.input_csv, encoding="utf-8-sig")
    dataframe["service_label_original"] = dataframe["service_label"]

    applied = []
    reasons = []
    for text in dataframe["text"].fillna("").astype(str):
        should_relabel, reason = classify_tired_heuristic(text, args.version)
        applied.append(should_relabel)
        reasons.append(reason)

    dataframe["tired_heuristic_applied"] = applied
    dataframe["tired_heuristic_reason"] = reasons
    dataframe.loc[dataframe["tired_heuristic_applied"], "service_label"] = "TIRED"

    output_path = Path(args.output_csv)
    output_path.parent.mkdir(parents=True, exist_ok=True)
    dataframe.to_csv(output_path, index=False, encoding="utf-8-sig")

    relabeled_count = int(dataframe["tired_heuristic_applied"].sum())
    print(f"version={args.version}")
    print(f"output={output_path}")
    print(f"rows={len(dataframe)}")
    print(f"relabeled_to_tired={relabeled_count}")
    print(dataframe["service_label"].value_counts().to_string())


if __name__ == "__main__":
    main()
