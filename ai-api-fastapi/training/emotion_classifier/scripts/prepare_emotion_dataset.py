# 감성대화 원본을 KcELECTRA 학습용 csv로 변환하는 준비 스크립트입니다.
from __future__ import annotations

import argparse
import csv
import json
from pathlib import Path
from typing import Dict, Iterable, List
from zipfile import ZipFile
import xml.etree.ElementTree as ET
import re


SERVICE_LABEL_MAP = {
    "기쁨": "HAPPY",
    "만족": "HAPPY",
    "설렘": "HAPPY",
    "신이 난": "HAPPY",
    "감사하는": "HAPPY",
    "만족스러운": "HAPPY",
    "자신하는": "HAPPY",
    "안도": "CALM",
    "편안": "CALM",
    "편안한": "CALM",
    "평온": "CALM",
    "신뢰하는": "CALM",
    "불안": "ANXIOUS",
    "걱정": "ANXIOUS",
    "걱정스러운": "ANXIOUS",
    "긴장": "ANXIOUS",
    "초조": "ANXIOUS",
    "초조한": "ANXIOUS",
    "두려움": "ANXIOUS",
    "두려운": "ANXIOUS",
    "당황": "ANXIOUS",
    "당혹스러운": "ANXIOUS",
    "혼란스러운": "ANXIOUS",
    "안달하는": "ANXIOUS",
    "조심스러운": "ANXIOUS",
    "남의 시선을 의식하는": "ANXIOUS",
    "취약한": "ANXIOUS",
    "슬픔": "SAD",
    "우울": "SAD",
    "우울한": "SAD",
    "상실감": "SAD",
    "외로움": "SAD",
    "외로운": "SAD",
    "눈물이 나는": "SAD",
    "괴로워하는": "SAD",
    "비통한": "SAD",
    "상처": "SAD",
    "후회되는": "SAD",
    "낙담한": "SAD",
    "죄책감의": "SAD",
    "가난한, 불우한": "SAD",
    "배신당한": "SAD",
    "버려진": "SAD",
    "고립된": "SAD",
    "실망한": "SAD",
    "좌절한": "SAD",
    "열등감": "SAD",
    "분노": "ANGRY",
    "짜증": "ANGRY",
    "짜증내는": "ANGRY",
    "억울함": "ANGRY",
    "억울한": "ANGRY",
    "노여움": "ANGRY",
    "노여워하는": "ANGRY",
    "성가신": "ANGRY",
    "툴툴대는": "ANGRY",
    "한심한": "ANGRY",
    "질투하는": "ANGRY",
    "환멸을 느끼는": "ANGRY",
    "구역질 나는": "ANGRY",
    "악의적인": "ANGRY",
    "피로": "TIRED",
    "지침": "TIRED",
    "무기력": "TIRED",
    "기운없음": "TIRED",
    "지친": "TIRED"
}

MAJOR_LABEL_MAP = {
    "기쁨": "HAPPY",
    "불안": "ANXIOUS",
    "분노": "ANGRY",
    "슬픔": "SAD",
    "상처": "SAD",
    "당황": "ANXIOUS"
}

KEYWORD_LABEL_RULES = [
    ("피로", "TIRED"),
    ("지침", "TIRED"),
    ("무기력", "TIRED"),
    ("기운없", "TIRED"),
    ("지친", "TIRED"),
    ("안도", "CALM"),
    ("편안", "CALM"),
    ("평온", "CALM"),
    ("신뢰", "CALM"),
    ("불안", "ANXIOUS"),
    ("걱정", "ANXIOUS"),
    ("긴장", "ANXIOUS"),
    ("초조", "ANXIOUS"),
    ("두려", "ANXIOUS"),
    ("당황", "ANXIOUS"),
    ("당혹", "ANXIOUS"),
    ("혼란", "ANXIOUS"),
    ("안달", "ANXIOUS"),
    ("조심", "ANXIOUS"),
    ("의식", "ANXIOUS"),
    ("취약", "ANXIOUS"),
    ("우울", "SAD"),
    ("슬픔", "SAD"),
    ("외로", "SAD"),
    ("눈물", "SAD"),
    ("괴로", "SAD"),
    ("비통", "SAD"),
    ("상처", "SAD"),
    ("후회", "SAD"),
    ("낙담", "SAD"),
    ("죄책", "SAD"),
    ("고립", "SAD"),
    ("실망", "SAD"),
    ("좌절", "SAD"),
    ("버려", "SAD"),
    ("배신", "SAD"),
    ("열등감", "SAD"),
    ("가난", "SAD"),
    ("분노", "ANGRY"),
    ("짜증", "ANGRY"),
    ("억울", "ANGRY"),
    ("노여", "ANGRY"),
    ("성가", "ANGRY"),
    ("툴툴", "ANGRY"),
    ("한심", "ANGRY"),
    ("질투", "ANGRY"),
    ("환멸", "ANGRY"),
    ("구역질", "ANGRY"),
    ("악의", "ANGRY"),
    ("기쁨", "HAPPY"),
    ("만족", "HAPPY"),
    ("설렘", "HAPPY"),
    ("신이 난", "HAPPY"),
    ("감사", "HAPPY"),
    ("자신", "HAPPY")
]


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser()
    parser.add_argument("--xlsx", required=True)
    parser.add_argument("--json", required=False)
    parser.add_argument("--split", required=True, choices=["train", "valid"])
    parser.add_argument("--output", required=True)
    return parser.parse_args()


def normalize_text(text: str) -> str:
    return " ".join((text or "").strip().split())


def read_cell_value(cell: ET.Element, ns: Dict[str, str], shared_strings: List[str]) -> str:
    cell_type = cell.attrib.get("t")
    raw_value = cell.find("a:v", ns)
    value = "" if raw_value is None else raw_value.text or ""
    if cell_type == "s" and value:
        return shared_strings[int(value)]
    return value


def extract_column_ref(cell_ref: str) -> str:
    match = re.match(r"([A-Z]+)", cell_ref)
    return match.group(1) if match else cell_ref


def load_xlsx_rows(xlsx_path: Path) -> Iterable[Dict[str, str]]:
    ns = {
        "a": "http://schemas.openxmlformats.org/spreadsheetml/2006/main",
        "r": "http://schemas.openxmlformats.org/officeDocument/2006/relationships"
    }
    with ZipFile(xlsx_path) as archive:
        shared_strings: List[str] = []
        if "xl/sharedStrings.xml" in archive.namelist():
            root = ET.fromstring(archive.read("xl/sharedStrings.xml"))
            for si in root.findall("a:si", ns):
                texts = [node.text or "" for node in si.findall(".//a:t", ns)]
                shared_strings.append("".join(texts))

        workbook = ET.fromstring(archive.read("xl/workbook.xml"))
        rels = ET.fromstring(archive.read("xl/_rels/workbook.xml.rels"))
        rel_map = {rel.attrib["Id"]: rel.attrib["Target"] for rel in rels}
        first_sheet = workbook.find("a:sheets", ns)[0]
        rel_id = first_sheet.attrib[
            "{http://schemas.openxmlformats.org/officeDocument/2006/relationships}id"
        ]
        target = rel_map[rel_id]
        if not target.startswith("xl/"):
            target = "xl/" + target

        worksheet = ET.fromstring(archive.read(target))
        sheet_data = worksheet.find("a:sheetData", ns)
        rows = sheet_data.findall("a:row", ns)

        header_map = {}
        for cell in rows[0].findall("a:c", ns):
            column_ref = extract_column_ref(cell.attrib.get("r", ""))
            header_map[column_ref] = read_cell_value(cell, ns, shared_strings)

        for row in rows[1:]:
            record: Dict[str, str] = {}
            for cell in row.findall("a:c", ns):
                column_ref = extract_column_ref(cell.attrib.get("r", ""))
                header = header_map.get(column_ref)
                if header:
                    record[header] = read_cell_value(cell, ns, shared_strings)
            yield record


def load_json_metadata(json_path: Path | None) -> Dict[str, Dict[str, str]]:
    if json_path is None or not json_path.exists():
        return {}

    with json_path.open("r", encoding="utf-8-sig") as file:
        items = json.load(file)

    metadata: Dict[str, Dict[str, str]] = {}
    for item in items:
        content = item.get("talk", {}).get("content", {})
        key = normalize_text(
            " ".join(
                filter(None, [content.get("HS01", ""), content.get("HS02", ""), content.get("HS03", "")])
            )
        )
        metadata[key] = {
            "persona_id": item.get("profile", {}).get("persona-id", ""),
            "talk_id": item.get("talk", {}).get("id", {}).get("talk-id", ""),
            "emotion_code": item.get("profile", {}).get("emotion", {}).get("type", ""),
            "situation_code": "_".join(item.get("profile", {}).get("emotion", {}).get("situation", []))
        }
    return metadata


def map_service_label(major: str, minor: str) -> str:
    normalized_minor = normalize_text(minor)
    normalized_major = normalize_text(major)

    exact_match = SERVICE_LABEL_MAP.get(normalized_minor) or SERVICE_LABEL_MAP.get(normalized_major)
    if exact_match:
        return exact_match

    for keyword, label in KEYWORD_LABEL_RULES:
        if keyword in normalized_minor:
            return label

    for keyword, label in KEYWORD_LABEL_RULES:
        if keyword in normalized_major:
            return label

    return MAJOR_LABEL_MAP.get(normalized_major, "CALM")


def build_sample(row: Dict[str, str], split: str, metadata: Dict[str, Dict[str, str]], index: int) -> Dict[str, str]:
    turns = [
        normalize_text(row.get("사람문장1", "")),
        normalize_text(row.get("사람문장2", "")),
        normalize_text(row.get("사람문장3", ""))
    ]
    dialogue_text = " ".join([turn for turn in turns if turn])
    meta = metadata.get(dialogue_text, {})
    major = normalize_text(row.get("감정_대분류", ""))
    minor = normalize_text(row.get("감정_소분류", ""))
    return {
        "sample_id": f"{split}-{index:06d}",
        "split": split,
        "age_group": normalize_text(row.get("연령", "")),
        "gender": normalize_text(row.get("성별", "")),
        "situation_keyword": normalize_text(row.get("상황키워드", "")),
        "physical_disease": normalize_text(row.get("신체질환", "")),
        "emotion_major": major,
        "emotion_minor": minor,
        "service_label": map_service_label(major, minor),
        "text": dialogue_text,
        "persona_id": meta.get("persona_id", ""),
        "talk_id": meta.get("talk_id", ""),
        "emotion_code": meta.get("emotion_code", ""),
        "situation_code": meta.get("situation_code", "")
    }


def main() -> None:
    args = parse_args()
    metadata = load_json_metadata(Path(args.json)) if args.json else {}
    rows = load_xlsx_rows(Path(args.xlsx))
    output_path = Path(args.output)
    output_path.parent.mkdir(parents=True, exist_ok=True)

    fieldnames = [
        "sample_id",
        "split",
        "age_group",
        "gender",
        "situation_keyword",
        "physical_disease",
        "emotion_major",
        "emotion_minor",
        "service_label",
        "text",
        "persona_id",
        "talk_id",
        "emotion_code",
        "situation_code"
    ]

    with output_path.open("w", encoding="utf-8-sig", newline="") as file:
        writer = csv.DictWriter(file, fieldnames=fieldnames)
        writer.writeheader()
        for index, row in enumerate(rows, start=1):
            sample = build_sample(row, args.split, metadata, index)
            if sample["text"]:
                writer.writerow(sample)


if __name__ == "__main__":
    main()
