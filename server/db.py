#!/usr/bin/env python3
# -*- coding: utf-8 -*-
# 2026-11-25 - Smart Voca - SQLite 연결과 첫 실행 시 테이블 자동 생성 (VOCA-NFR-002, VOCA-NFR-003)
# 파일 위치: server/db.py

import os
import sqlite3
from collections.abc import Iterator
from contextlib import contextmanager
from pathlib import Path

# 서버에서는 Dockerfile 이 DB_PATH=/data/voca.db 를 준다 (R-06).
# 개발 PC 에서는 환경변수가 없으므로 server/data/voca.db 를 쓴다 (커밋하지 않음).
DEFAULT_DB_PATH = Path(__file__).resolve().parent / "data" / "voca.db"

# 스키마 버전 (PRAGMA user_version). 테이블을 바꿀 때는 숫자를 올리고
# MIGRATIONS 에 "이전 버전 → 새 버전" 단계를 추가한다. 상세: 261125_데이터설계서.md
SCHEMA_VERSION = 1

SCHEMA_V1 = """
CREATE TABLE IF NOT EXISTS words (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    word TEXT NOT NULL UNIQUE COLLATE NOCASE,
    meaning TEXT NOT NULL,
    freq_rank INTEGER,
    level TEXT CHECK (level IN ('basic', 'intermediate', 'advanced')),
    pos TEXT,
    example_en TEXT,
    example_ko TEXT,
    created_at TEXT NOT NULL DEFAULT (datetime('now', 'localtime')),
    updated_at TEXT NOT NULL DEFAULT (datetime('now', 'localtime'))
);
CREATE INDEX IF NOT EXISTS idx_words_freq_rank ON words (freq_rank);
CREATE TABLE IF NOT EXISTS study_records (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    word_id INTEGER NOT NULL REFERENCES words(id) ON DELETE CASCADE,
    day_no INTEGER NOT NULL,
    studied_at TEXT NOT NULL DEFAULT (datetime('now', 'localtime'))
);
CREATE TABLE IF NOT EXISTS exam_answers (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    exam_no INTEGER NOT NULL,
    word_id INTEGER NOT NULL REFERENCES words(id) ON DELETE CASCADE,
    question_format TEXT NOT NULL,
    direction TEXT NOT NULL,
    is_correct INTEGER NOT NULL,
    answered_at TEXT NOT NULL DEFAULT (datetime('now', 'localtime'))
);
CREATE TABLE IF NOT EXISTS settings (
    key TEXT PRIMARY KEY,
    value TEXT NOT NULL
);
"""

# 버전 0(빈 DB) → 1. 이후 버전이 생기면 {1: "ALTER TABLE ..."} 처럼 이어 붙인다
MIGRATIONS = {0: SCHEMA_V1}


def resolve_db_path() -> Path:
    """환경변수 DB_PATH 가 있으면 그것을, 없으면 개발용 기본 경로를 쓴다."""
    configured = os.environ.get("DB_PATH")
    return Path(configured) if configured else DEFAULT_DB_PATH


@contextmanager
def connect(db_path: Path) -> Iterator[sqlite3.Connection]:
    """요청마다 새로 여는 연결. 성공하면 commit, 예외면 rollback 하고 항상 닫는다.

    sqlite3 연결의 기본 `with` 는 commit 만 하고 닫지 않아 파일이 잠긴 채 남는다.
    """
    connection = sqlite3.connect(db_path, timeout=10)
    connection.row_factory = sqlite3.Row
    connection.execute("PRAGMA foreign_keys = ON")
    try:
        yield connection
        connection.commit()
    except Exception:
        connection.rollback()
        raise
    finally:
        connection.close()


def init_db(db_path: Path) -> None:
    """DB 파일이 없으면 만들고, 스키마를 최신 버전까지 올린다. 여러 번 불러도 안전하다."""
    db_path.parent.mkdir(parents=True, exist_ok=True)
    with connect(db_path) as connection:
        version = connection.execute("PRAGMA user_version").fetchone()[0]
        while version < SCHEMA_VERSION:
            connection.executescript(MIGRATIONS[version])
            version += 1
            # PRAGMA 는 ? 자리표시자를 받지 않는다. 값은 코드 안의 정수라 안전하다
            connection.execute(f"PRAGMA user_version = {version}")


def get_setting(connection: sqlite3.Connection, key: str) -> str | None:
    row = connection.execute("SELECT value FROM settings WHERE key = ?", (key,)).fetchone()
    return row["value"] if row else None


def set_setting(connection: sqlite3.Connection, key: str, value: str) -> None:
    connection.execute(
        "INSERT INTO settings (key, value) VALUES (?, ?) ON CONFLICT(key) DO UPDATE SET value = excluded.value",
        (key, value),
    )
