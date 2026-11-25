#!/usr/bin/env python3
# -*- coding: utf-8 -*-
# 2026-11-25 - Smart Voca - 화면 라우트 테스트: 첫 화면, 테이블 생성, 단어 등록·수정·삭제·검색, CSV 가져오기, 주소 규칙(R-04)
# 파일 위치: server/tests/test_app.py
# 실행: server 폴더에서 python -m unittest discover tests

import io
import os
import re
import sqlite3
import sys
import tempfile
import unittest
from contextlib import closing
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent.parent))

STARTER_COUNT = 120


class AppTestCase(unittest.TestCase):
    def setUp(self):
        # 빈 임시 폴더에서 시작해 "처음 실행" 상황을 만든다 (VOCA-NFR-003)
        self.temp_dir = tempfile.TemporaryDirectory()
        self.db_path = Path(self.temp_dir.name) / "voca.db"
        os.environ["DB_PATH"] = str(self.db_path)
        import app as app_module
        self.app_module = app_module
        self.client = app_module.create_app().test_client()

    def tearDown(self):
        os.environ.pop("DB_PATH", None)
        self.temp_dir.cleanup()

    def query(self, sql, params=()):
        with closing(sqlite3.connect(self.db_path)) as connection:
            return connection.execute(sql, params).fetchall()

    def upload(self, text, file_name="words.csv"):
        data = {"csv_file": (io.BytesIO(text.encode("utf-8")), file_name)}
        return self.client.post("/settings/import", data=data, content_type="multipart/form-data")


class FirstRunTest(AppTestCase):
    def test_index_returns_ok(self):
        response = self.client.get("/")
        self.assertEqual(response.status_code, 200)
        self.assertIn("스마트보카", response.get_data(as_text=True))

    def test_tables_created_on_first_run(self):
        names = {row[0] for row in self.query("SELECT name FROM sqlite_master WHERE type = 'table'")}
        self.assertTrue({"words", "study_records", "exam_answers", "settings"} <= names)
        self.assertEqual(self.query("PRAGMA user_version")[0][0], 1)

    def test_starter_verbs_seeded_once(self):
        self.assertEqual(self.query("SELECT COUNT(*) FROM words")[0][0], STARTER_COUNT)
        # 모두 지운 뒤 앱을 다시 만들어도 다시 넣지 않는다
        with closing(sqlite3.connect(self.db_path)) as connection:
            connection.execute("DELETE FROM words")
            connection.commit()
        self.app_module.create_app()
        self.assertEqual(self.query("SELECT COUNT(*) FROM words")[0][0], 0)

    def test_settings_button_and_page(self):
        self.assertIn('href="/settings"', self.client.get("/").get_data(as_text=True))
        self.assertEqual(self.client.get("/settings").status_code, 200)


class WordCrudTest(AppTestCase):
    def test_add_word(self):
        response = self.client.post("/words", data={"word": " Apple ", "meaning": "사과"}, follow_redirects=True)
        self.assertIn("등록했습니다", response.get_data(as_text=True))
        self.assertEqual(self.query("SELECT word, meaning FROM words WHERE word = 'Apple'"), [("Apple", "사과")])

    def test_add_duplicate_redirects_to_edit_without_overwrite(self):
        response = self.client.post("/words", data={"word": "GO", "meaning": "덮어쓰기"})
        self.assertEqual(response.status_code, 302)
        self.assertRegex(response.headers["Location"], r"^/words/\d+/edit$")
        self.assertEqual(self.query("SELECT meaning FROM words WHERE word = 'go'"), [("가다",)])

    def test_update_and_delete(self):
        word_id = self.query("SELECT id FROM words WHERE word = 'go'")[0][0]
        self.client.post(f"/words/{word_id}/edit", data={"word": "go", "meaning": "가다; 진행되다"})
        self.assertEqual(self.query("SELECT meaning FROM words WHERE id = ?", (word_id,)), [("가다; 진행되다",)])
        self.client.post(f"/words/{word_id}/delete")
        self.assertEqual(self.query("SELECT COUNT(*) FROM words WHERE id = ?", (word_id,))[0][0], 0)

    def test_update_to_existing_spelling_is_rejected(self):
        go_id = self.query("SELECT id FROM words WHERE word = 'go'")[0][0]
        self.client.post(f"/words/{go_id}/edit", data={"word": "Come", "meaning": "x"})
        self.assertEqual(self.query("SELECT word FROM words WHERE id = ?", (go_id,)), [("go",)])

    def test_search_by_word_and_meaning(self):
        by_word = self.client.get("/words?q=GO").get_data(as_text=True)
        self.assertIn(">go<", by_word)
        by_meaning = self.client.get("/words?q=가다").get_data(as_text=True)
        self.assertIn(">go<", by_meaning)
        self.assertNotIn(">come<", self.client.get("/words?q=zzz").get_data(as_text=True))

    def test_missing_word_returns_404(self):
        self.assertEqual(self.client.get("/words/99999/edit").status_code, 404)


class CsvImportRouteTest(AppTestCase):
    def test_import_new_merged_and_updated(self):
        text = "﻿영단어,뜻\nbank,은행\nbank,둑\ngo,가다; 떠나다\n,빈칸\n"
        page = self.upload(text).get_data(as_text=True)
        self.assertEqual(self.query("SELECT meaning FROM words WHERE word = 'bank'"), [("은행; 둑",)])
        self.assertEqual(self.query("SELECT meaning FROM words WHERE word = 'go'"), [("가다; 떠나다",)])
        self.assertRegex(page, r"새로 등록: <strong>1</strong>")
        self.assertRegex(page, r"뜻 갱신\(이미 있던 단어\): <strong>1</strong>")
        self.assertIn("5번째 줄: 영단어가 비어 있음", page)

    def test_import_keeps_word_id(self):
        go_id = self.query("SELECT id FROM words WHERE word = 'go'")[0][0]
        self.upload("영단어,뜻\nGo,가다\n")
        self.assertEqual(self.query("SELECT id FROM words WHERE word = 'go'")[0][0], go_id)

    def test_missing_columns_saves_nothing(self):
        before = self.query("SELECT COUNT(*) FROM words")[0][0]
        page = self.upload("word,level\nzebra,basic\n").get_data(as_text=True)
        self.assertIn("필수 열이 없어", page)
        self.assertEqual(self.query("SELECT COUNT(*) FROM words")[0][0], before)


class AddressRuleTest(AppTestCase):
    """R-04: 호스트를 쓰지 않고, 속성은 따옴표로 감싸고, 주소는 / 로 시작한다."""

    def test_pages_follow_address_rules(self):
        for path in ("/", "/words", "/settings", "/words?q=go"):
            html = self.client.get(path).get_data(as_text=True)
            self.assertNotIn("localhost", html, path)
            self.assertIsNone(re.search(r"\b(href|src|action)=[^\"']", html), path)
            for url in re.findall(r"\b(?:href|src|action)=\"([^\"]*)\"", html):
                self.assertTrue(url.startswith("/"), f"{path}: {url}")

    def test_static_css_served(self):
        response = self.client.get("/static/style.css")
        self.assertEqual(response.status_code, 200)
        response.close()  # 정적 파일은 파일 핸들로 응답하므로 닫아 준다


if __name__ == "__main__":
    unittest.main()
