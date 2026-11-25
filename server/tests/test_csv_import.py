#!/usr/bin/env python3
# -*- coding: utf-8 -*-
# 2026-11-25 - Smart Voca - CSV 해석·검증·파일 안 중복 합치기 테스트 (VOCA-FR-003)
# 파일 위치: server/tests/test_csv_import.py

import sys
import unittest
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent.parent))

from csv_import import CsvFormatError, decode_csv_bytes, merge_meanings, parse_word_csv  # noqa: E402


class HeaderTest(unittest.TestCase):
    def test_assignment_format_with_bom(self):
        raw = "﻿영단어,뜻\napple,사과\n".encode("utf-8")
        result = parse_word_csv(decode_csv_bytes(raw))
        self.assertEqual(result.missing_columns, [])
        self.assertEqual([(item.word, item.meaning) for item in result.items], [("apple", "사과")])

    def test_project_format_reads_optional_columns(self):
        text = "word,pos,meaning_ko,example_en,example_ko,freq_rank,level,rating\nbe,,있다,,,1,basic,★★★★\n"
        item = parse_word_csv(text).items[0]
        self.assertEqual((item.word, item.meaning, item.freq_rank, item.level), ("be", "있다", 1, "basic"))
        self.assertIsNone(item.pos)

    def test_missing_required_column(self):
        result = parse_word_csv("word,level\napple,basic\n")
        self.assertEqual(result.missing_columns, ["meaning"])
        self.assertEqual(result.items, [])

    def test_non_utf8_is_rejected(self):
        with self.assertRaises(CsvFormatError):
            decode_csv_bytes("영단어,뜻\n".encode("cp949"))


class RowValidationTest(unittest.TestCase):
    def test_bad_rows_are_skipped_with_line_numbers(self):
        text = "영단어,뜻\napple,사과\n,빈 단어\nbanana,\n사과,apple\n\n"
        result = parse_word_csv(text)
        self.assertEqual([item.word for item in result.items], ["apple"])
        self.assertEqual([row.line_no for row in result.skipped], [3, 4, 5])

    def test_invalid_optional_values_become_none(self):
        item = parse_word_csv("word,meaning_ko,freq_rank,level\ngo,가다,abc,expert\n").items[0]
        self.assertIsNone(item.freq_rank)
        self.assertIsNone(item.level)

    def test_phrases_and_apostrophes_allowed(self):
        result = parse_word_csv("영단어,뜻\ngive up,포기하다\ndon't,하지 마라\n")
        self.assertEqual(len(result.items), 2)


class DuplicateMergeTest(unittest.TestCase):
    """방침 A: 같은 철자(대소문자 무시)는 뜻을 '; ' 로 합치고 빈도는 작은 값."""

    def test_same_word_meanings_are_merged(self):
        text = "word,meaning_ko,freq_rank,level\nlike,좋아하다,90,basic\nLike,~처럼,40,intermediate\n"
        result = parse_word_csv(text)
        self.assertEqual(len(result.items), 1)
        item = result.items[0]
        self.assertEqual((item.word, item.meaning, item.freq_rank, item.level), ("like", "좋아하다; ~처럼", 40, "basic"))
        self.assertEqual(result.merged_count, 1)

    def test_identical_meaning_not_repeated(self):
        self.assertEqual(merge_meanings("가다", "가다"), "가다")
        self.assertEqual(merge_meanings("가다; 떠나다", "떠나다"), "가다; 떠나다")


if __name__ == "__main__":
    unittest.main()
