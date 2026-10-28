#!/usr/bin/env python3
# -*- coding: utf-8 -*-
# 2026-10-21 - Smart Voca - "필수 영어단어 5000" PDF 5종을 단어 CSV 초안으로 변환 (1회성)
# 파일 위치: ai-agent/scripts/261021-extract_word_pdf.py
#
# 사용법: python 261021-extract_word_pdf.py <PDF 폴더> <출력 CSV 경로>
# 참고: PDF 텍스트 레이어에 한국어 뜻이 들어 있어 pymupdf 로 직접 추출한다 (OCR 불필요).
#       pos, example_en, example_ko 는 원본에 없으므로 빈 값으로 둔다.

import csv
import glob
import os
import re
import sys

import pymupdf

STAR_LINE = re.compile(r"^[★☆△]+$")
RANK_LINE = re.compile(r"^(\d{1,3}(,\d{3})+|\d{1,4})$")
WORD_LINE = re.compile(r"^[A-Za-z][A-Za-z'\- ]*$")
HEADER_LINES = {"순위", "낱말", "뜻", "평점"}
FOOTER_START = ("①", "②", "③", "④", "⑤", "http")
PAGE_MARK = re.compile(r"^\d+/\d+$")
DATE_LINE = re.compile(r"^\d{4}-\d{2}-\d{2}$")


def is_noise(line):
    if line in HEADER_LINES or line.startswith("필수 영어단어"):
        return True
    if DATE_LINE.match(line) or PAGE_MARK.match(line):
        return True
    return line.startswith(FOOTER_START)


def read_pdf_lines(pdf_path):
    lines = []
    with pymupdf.open(pdf_path) as doc:
        for page in doc:
            for raw in page.get_text().split("\n"):
                line = raw.strip()
                if line and not is_noise(line):
                    lines.append(line)
    return lines


def parse_entries(lines):
    entries, rejected = [], []
    i = 0
    while i < len(lines):
        if not RANK_LINE.match(lines[i]):
            i += 1
            continue
        rank = int(lines[i].replace(",", ""))
        word = lines[i + 1] if i + 1 < len(lines) else ""
        j = i + 2
        meaning_parts = []
        while j < len(lines) and not STAR_LINE.match(lines[j]) and not RANK_LINE.match(lines[j]):
            meaning_parts.append(lines[j])
            j += 1
        if j < len(lines) and STAR_LINE.match(lines[j]) and WORD_LINE.match(word) and meaning_parts:
            meaning = re.sub(r"\s+", " ", " ".join(meaning_parts)).strip()
            entries.append({"rank": rank, "word": word, "meaning_ko": meaning, "rating": lines[j]})
            i = j + 1
        else:
            rejected.append((rank, word, " / ".join(meaning_parts)))
            i = j
    return entries, rejected


def level_of(rank):
    # 제안 구간 (OI-03 확정 후 세부 기준 미정): 1~1000 기초, 1001~3000 중급, 3001~ 심화
    if rank <= 1000:
        return "basic"
    if rank <= 3000:
        return "intermediate"
    return "advanced"


def main():
    input_dir, output_csv = sys.argv[1], sys.argv[2]
    pdf_files = glob.glob(os.path.join(input_dir, "영어단어 5000*.pdf"))
    pdf_files.sort(key=lambda p: int(re.search(r"\((\d+)\s*~", p).group(1)))

    all_entries, all_rejected = [], []
    for pdf_path in pdf_files:
        entries, rejected = parse_entries(read_pdf_lines(pdf_path))
        print(f"{os.path.basename(pdf_path)}: entries={len(entries)} rejected={len(rejected)}")
        all_entries.extend(entries)
        all_rejected.extend(rejected)

    ranks = [e["rank"] for e in all_entries]
    missing = sorted(set(range(1, 5001)) - set(ranks))
    duplicated_ranks = sorted({r for r in ranks if ranks.count(r) > 1})
    print(f"total entries={len(all_entries)} unique ranks={len(set(ranks))}")
    print(f"missing ranks={len(missing)} {missing[:30]}")
    print(f"duplicated ranks={duplicated_ranks[:30]}")
    print(f"rejected rows={len(all_rejected)}")
    for rank, word, meaning in all_rejected[:30]:
        print(f"  rejected: rank={rank} word={word!r} meaning={meaning!r}")

    os.makedirs(os.path.dirname(output_csv), exist_ok=True)
    columns = ["word", "pos", "meaning_ko", "example_en", "example_ko", "freq_rank", "level", "rating"]
    with open(output_csv, "w", newline="", encoding="utf-8-sig") as f:
        writer = csv.DictWriter(f, fieldnames=columns)
        writer.writeheader()
        for entry in sorted(all_entries, key=lambda e: e["rank"]):
            writer.writerow({
                "word": entry["word"],
                "pos": "",
                "meaning_ko": entry["meaning_ko"],
                "example_en": "",
                "example_ko": "",
                "freq_rank": entry["rank"],
                "level": level_of(entry["rank"]),
                "rating": entry["rating"],
            })
    print(f"saved: {output_csv}")


if __name__ == "__main__":
    main()
