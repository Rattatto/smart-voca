#!/usr/bin/env python3
# -*- coding: utf-8 -*-
# 2026-11-25 - Smart Voca - 단어 등록·수정·삭제·검색·일괄 등록·첫 실행 적재 (VOCA-FR-001~004)
# 파일 위치: server/word_repository.py
# 규칙 상세: ai-agent/claude/update/261125_데이터설계서.md §3

import sqlite3
from dataclasses import dataclass
from pathlib import Path

import db
from csv_import import WordItem, decode_csv_bytes, normalize_word, parse_word_csv

PAGE_SIZE = 50
STARTER_CSV_PATH = Path(__file__).resolve().parent / "seed" / "starter_verbs.csv"
STARTER_SEEDED_KEY = "starter_seeded"


class DuplicateWordError(ValueError):
    """같은 철자(대소문자 무시)의 단어가 이미 있다."""

    def __init__(self, existing_id: int):
        super().__init__("이미 등록된 단어입니다")
        self.existing_id = existing_id


@dataclass
class WordPage:
    rows: list[sqlite3.Row]
    total: int
    page: int
    page_count: int


@dataclass
class ImportSummary:
    inserted: int
    updated: int


def find_id_by_word(connection: sqlite3.Connection, word: str) -> int | None:
    # words.word 가 COLLATE NOCASE 라서 = 비교가 대소문자를 무시한다
    row = connection.execute("SELECT id FROM words WHERE word = ?", (" ".join(word.split()),)).fetchone()
    return row["id"] if row else None


def count_words(connection: sqlite3.Connection) -> int:
    return connection.execute("SELECT COUNT(*) FROM words").fetchone()[0]


def get_word(connection: sqlite3.Connection, word_id: int) -> sqlite3.Row | None:
    return connection.execute("SELECT * FROM words WHERE id = ?", (word_id,)).fetchone()


def add_word(connection: sqlite3.Connection, word: str, meaning: str) -> int:
    """직접 등록 (FR-001). 같은 철자가 있으면 덮어쓰지 않고 DuplicateWordError."""
    word = " ".join(word.split())
    existing_id = find_id_by_word(connection, word)
    if existing_id is not None:
        raise DuplicateWordError(existing_id)
    cursor = connection.execute("INSERT INTO words (word, meaning) VALUES (?, ?)", (word, meaning.strip()))
    return cursor.lastrowid


def update_word(connection: sqlite3.Connection, word_id: int, word: str, meaning: str) -> None:
    """수정 (FR-002). 철자를 바꿔 다른 단어와 겹치면 DuplicateWordError."""
    word = " ".join(word.split())
    existing_id = find_id_by_word(connection, word)
    if existing_id is not None and existing_id != word_id:
        raise DuplicateWordError(existing_id)
    connection.execute(
        "UPDATE words SET word = ?, meaning = ?, updated_at = datetime('now', 'localtime') WHERE id = ?",
        (word, meaning.strip(), word_id),
    )


def delete_word(connection: sqlite3.Connection, word_id: int) -> None:
    """삭제 (FR-002). 암기·시험 이력은 ON DELETE CASCADE 로 함께 지워진다."""
    connection.execute("DELETE FROM words WHERE id = ?", (word_id,))


def _escape_like(text: str) -> str:
    # 검색어의 % _ 를 글자 그대로 찾도록 이스케이프한다
    return text.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_")


def search_words(connection: sqlite3.Connection, query: str = "", page: int = 1) -> WordPage:
    """목록·검색 (FR-002, FR-004). 영단어 또는 뜻 부분 일치, 대소문자 무시."""
    condition, params = "", []
    if query.strip():
        pattern = f"%{_escape_like(query.strip())}%"
        condition = "WHERE word LIKE ? ESCAPE '\\' OR meaning LIKE ? ESCAPE '\\'"
        params = [pattern, pattern]
    total = connection.execute(f"SELECT COUNT(*) FROM words {condition}", params).fetchone()[0]
    page_count = max(1, -(-total // PAGE_SIZE))
    page = min(max(1, page), page_count)
    rows = connection.execute(
        f"SELECT * FROM words {condition} ORDER BY freq_rank IS NULL, freq_rank, word LIMIT ? OFFSET ?",
        [*params, PAGE_SIZE, (page - 1) * PAGE_SIZE],
    ).fetchall()
    return WordPage(rows=rows, total=total, page=page, page_count=page_count)


def import_words(connection: sqlite3.Connection, items: list[WordItem]) -> ImportSummary:
    """일괄 등록 (FR-003). 새 철자는 추가, 기존 철자는 뜻 갱신 + 값이 있는 선택 항목만 갱신."""
    existing = {normalize_word(row["word"]): row["id"] for row in connection.execute("SELECT id, word FROM words")}
    inserted = updated = 0
    for item in items:
        values = (item.meaning, item.freq_rank, item.level, item.pos, item.example_en, item.example_ko)
        word_id = existing.get(normalize_word(item.word))
        if word_id is None:
            connection.execute(
                "INSERT INTO words (word, meaning, freq_rank, level, pos, example_en, example_ko) VALUES (?, ?, ?, ?, ?, ?, ?)",
                (item.word, *values),
            )
            inserted += 1
        else:
            connection.execute(
                "UPDATE words SET meaning = ?, freq_rank = COALESCE(?, freq_rank), level = COALESCE(?, level), "
                "pos = COALESCE(?, pos), example_en = COALESCE(?, example_en), example_ko = COALESCE(?, example_ko), "
                "updated_at = datetime('now', 'localtime') WHERE id = ?",
                (*values, word_id),
            )
            updated += 1
    return ImportSummary(inserted=inserted, updated=updated)


def seed_starter_words(connection: sqlite3.Connection) -> int:
    """첫 실행 때 임시 동사 120개를 한 번만 넣는다. 단어를 모두 지워도 다시 넣지 않는다."""
    if db.get_setting(connection, STARTER_SEEDED_KEY) is not None:
        return 0
    parsed = parse_word_csv(decode_csv_bytes(STARTER_CSV_PATH.read_bytes()))
    summary = import_words(connection, parsed.items)
    db.set_setting(connection, STARTER_SEEDED_KEY, "1")
    return summary.inserted
