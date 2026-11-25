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

# 1주차 뼈대 스키마. 이후 데이터 설계서에 맞춰 늘린다.
# CREATE TABLE IF NOT EXISTS 라서 재시작해도 기존 데이터를 지우지 않는다.
SCHEMA = """
CREATE TABLE IF NOT EXISTS words (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    word TEXT NOT NULL UNIQUE,
    meaning TEXT NOT NULL,
    created_at TEXT NOT NULL DEFAULT (datetime('now', 'localtime'))
);
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
"""


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
    """DB 파일이 없으면 만들고 테이블을 준비한다. 여러 번 불러도 안전하다."""
    db_path.parent.mkdir(parents=True, exist_ok=True)
    with connect(db_path) as connection:
        connection.executescript(SCHEMA)
