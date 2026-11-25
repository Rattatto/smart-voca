#!/usr/bin/env python3
# -*- coding: utf-8 -*-
# 2026-11-25 - Smart Voca - 첫 화면 응답, 테이블 자동 생성, 주소 규칙(R-04) 테스트
# 파일 위치: server/tests/test_app.py
# 실행: server 폴더에서 python -m unittest discover tests

import os
import re
import sqlite3
import sys
import tempfile
import unittest
from contextlib import closing
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent.parent))


class FirstScreenTest(unittest.TestCase):
    def setUp(self):
        # 빈 임시 폴더에서 시작해 "처음 실행" 상황을 만든다 (VOCA-NFR-003)
        self.temp_dir = tempfile.TemporaryDirectory()
        self.db_path = Path(self.temp_dir.name) / "voca.db"
        os.environ["DB_PATH"] = str(self.db_path)
        import app as app_module
        self.client = app_module.create_app().test_client()

    def tearDown(self):
        os.environ.pop("DB_PATH", None)
        self.temp_dir.cleanup()

    def test_index_returns_ok(self):
        response = self.client.get("/")
        self.assertEqual(response.status_code, 200)
        self.assertIn("스마트보카", response.get_data(as_text=True))

    def test_tables_created_on_first_run(self):
        with closing(sqlite3.connect(self.db_path)) as connection:
            names = {row[0] for row in connection.execute("SELECT name FROM sqlite_master WHERE type = 'table'")}
        self.assertTrue({"words", "study_records", "exam_answers"} <= names)

    def test_links_are_root_relative_and_quoted(self):
        html = self.client.get("/").get_data(as_text=True)
        self.assertNotIn("localhost", html)
        # 따옴표 없는 href/src 속성이 없어야 한다
        self.assertIsNone(re.search(r"\b(href|src|action)=[^\"']", html))

    def test_static_css_served(self):
        response = self.client.get("/static/style.css")
        self.assertEqual(response.status_code, 200)
        response.close()  # 정적 파일은 파일 핸들로 응답하므로 닫아 준다


if __name__ == "__main__":
    unittest.main()
