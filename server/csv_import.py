#!/usr/bin/env python3
# -*- coding: utf-8 -*-
# 2026-11-25 - Smart Voca - 단어 CSV 해석, 행 검증, 파일 안 중복 합치기 (VOCA-FR-003)
# 파일 위치: server/csv_import.py
# 규칙 상세: ai-agent/claude/update/261125_요구사항명세서.md §4.2~4.3

import csv
import io
import re
from dataclasses import dataclass, field

LEVELS = ("basic", "intermediate", "advanced")
MEANING_SEPARATOR = "; "

# 헤더 별칭. 과제 제공 CSV(영단어,뜻)와 프로젝트 CSV(word,meaning_ko,...)를 둘 다 받는다
HEADER_ALIASES = {
    "word": ("영단어", "단어", "word"),
    "meaning": ("뜻", "의미", "meaning", "meaning_ko"),
    "freq_rank": ("freq_rank",),
    "level": ("level",),
    "pos": ("pos",),
    "example_en": ("example_en",),
    "example_ko": ("example_ko",),
}
REQUIRED_FIELDS = ("word", "meaning")
OPTIONAL_TEXT_FIELDS = ("pos", "example_en", "example_ko")

WORD_PATTERN = re.compile(r"^[A-Za-z][A-Za-z' .\-]*$")


@dataclass
class WordItem:
    """CSV 한 단어. 선택 항목은 값이 없으면 None."""
    word: str
    meaning: str
    freq_rank: int | None = None
    level: str | None = None
    pos: str | None = None
    example_en: str | None = None
    example_ko: str | None = None


@dataclass
class SkippedRow:
    line_no: int
    reason: str


@dataclass
class ParseResult:
    items: list[WordItem] = field(default_factory=list)
    skipped: list[SkippedRow] = field(default_factory=list)
    merged_count: int = 0           # 파일 안에서 같은 철자라 합쳐진 행 수
    missing_columns: list[str] = field(default_factory=list)


class CsvFormatError(ValueError):
    """파일을 UTF-8 로 읽을 수 없는 경우."""


def normalize_word(word: str) -> str:
    """같은 단어 판정 키. 앞뒤 공백 제거, 연속 공백 하나로, 소문자."""
    return " ".join(word.split()).lower()


def merge_meanings(existing: str, new: str) -> str:
    """뜻을 '; ' 로 이어 붙이되 이미 있는 뜻은 다시 넣지 않는다."""
    parts = existing.split(MEANING_SEPARATOR)
    if new in parts:
        return existing
    return existing + MEANING_SEPARATOR + new


def decode_csv_bytes(raw: bytes) -> str:
    # utf-8-sig 는 맨 앞 BOM 이 있으면 지우고, 없으면 그냥 UTF-8 로 읽는다
    try:
        return raw.decode("utf-8-sig")
    except UnicodeDecodeError as error:
        raise CsvFormatError("UTF-8 CSV 파일이 아닙니다. 엑셀에서 'CSV UTF-8' 로 저장해 주세요.") from error


def _map_header(header: list[str]) -> dict[str, int]:
    """헤더 이름을 내부 필드 이름 → 열 번호로 바꾼다. 처음 맞는 열을 쓴다."""
    column_by_field: dict[str, int] = {}
    for index, name in enumerate(header):
        key = name.strip().lower()
        for field_name, aliases in HEADER_ALIASES.items():
            if key in aliases and field_name not in column_by_field:
                column_by_field[field_name] = index
    return column_by_field


def _cell(row: list[str], column: int | None) -> str:
    if column is None or column >= len(row):
        return ""
    return row[column].strip()


def _to_item(row: list[str], columns: dict[str, int]) -> WordItem | str:
    """행 하나를 WordItem 으로. 건너뛸 행이면 사유 문자열을 돌려준다."""
    word = " ".join(_cell(row, columns["word"]).split())
    meaning = _cell(row, columns["meaning"])
    if not word:
        return "영단어가 비어 있음"
    if not meaning:
        return "뜻이 비어 있음"
    if not WORD_PATTERN.match(word):
        return f"영단어에 쓸 수 없는 문자가 있음 ({word})"
    rank_text = _cell(row, columns.get("freq_rank"))
    level = _cell(row, columns.get("level")).lower()
    item = WordItem(
        word=word,
        meaning=meaning,
        freq_rank=int(rank_text) if rank_text.isdigit() else None,
        level=level if level in LEVELS else None,
    )
    for name in OPTIONAL_TEXT_FIELDS:
        setattr(item, name, _cell(row, columns.get(name)) or None)
    return item


def _merge_into(target: WordItem, source: WordItem) -> None:
    """파일 안 중복(방침 A): 뜻은 합치고, 빈도는 작은 값, 나머지는 처음 값 유지."""
    target.meaning = merge_meanings(target.meaning, source.meaning)
    if source.freq_rank is not None and (target.freq_rank is None or source.freq_rank < target.freq_rank):
        target.freq_rank = source.freq_rank
    for name in ("level", *OPTIONAL_TEXT_FIELDS):
        if getattr(target, name) is None:
            setattr(target, name, getattr(source, name))


def parse_word_csv(text: str) -> ParseResult:
    """CSV 문자열을 해석한다. 필수 열이 없으면 items 없이 missing_columns 만 채운다."""
    result = ParseResult()
    rows = list(csv.reader(io.StringIO(text)))
    if not rows:
        result.missing_columns = list(REQUIRED_FIELDS)
        return result
    columns = _map_header(rows[0])
    result.missing_columns = [name for name in REQUIRED_FIELDS if name not in columns]
    if result.missing_columns:
        return result

    by_key: dict[str, WordItem] = {}
    for line_no, row in enumerate(rows[1:], start=2):   # 사람이 보는 줄 번호 (헤더가 1번)
        if not any(cell.strip() for cell in row):
            continue
        parsed = _to_item(row, columns)
        if isinstance(parsed, str):
            result.skipped.append(SkippedRow(line_no, parsed))
            continue
        key = normalize_word(parsed.word)
        if key in by_key:
            _merge_into(by_key[key], parsed)
            result.merged_count += 1
        else:
            by_key[key] = parsed
    result.items = list(by_key.values())
    return result
