#!/usr/bin/env python3
# -*- coding: utf-8 -*-
# 2026-11-25 - Smart Voca - 작업 문서를 과제 제출용 이름(분류_yymmdd_제목 vX.X.md)으로 docs/ 에 복사
# 파일 위치: ai-agent/scripts/export_submission_docs.py
#
# 원본 마크다운은 고치지 않는다. 내용은 그대로 두고 파일 이름만 과제 규칙(VOCA-SUB 4.2)에 맞춘다.
# 버전은 원본 헤더의 **버전** 줄에서 읽는다.
#
# 사용법 (리포 루트에서):
#   python ai-agent/scripts/export_submission_docs.py            # 1차 제출 대상만
#   python ai-agent/scripts/export_submission_docs.py --all      # 등록된 문서 전부
#   python ai-agent/scripts/export_submission_docs.py --dry-run  # 복사하지 않고 결과만 출력

import argparse
import re
import shutil
import sys
from pathlib import Path

REPO_ROOT = Path(__file__).resolve().parents[2]
DOCS_DIR = REPO_ROOT / "docs"

# (원본 경로, 분류, 제출용 제목, 1차 제출 포함 여부)
EXPORT_TARGETS = [
    ("ai-agent/claude/update/261021_기획서.md", "기획", "스마트보카 프로젝트 기획서", True),
    ("ai-agent/claude/update/261021_요구사항명세서.md", "요구", "스마트보카 요구사항 명세서", False),
    ("ai-agent/claude/update/261021_아키텍처설계서.md", "설계", "스마트보카 시스템 아키텍처 설계서", False),
    ("ai-agent/claude/update/261021_데이터설계서.md", "설계", "스마트보카 데이터 설계서", False),
]

VERSION_PATTERN = re.compile(r"^\*\*버전\*\*:\s*v(\d+\.\d+)", re.MULTILINE)
DATE_PREFIX_PATTERN = re.compile(r"^(\d{6})_")


def submission_name(source: Path, category: str, title: str) -> str:
    """원본 파일명의 yymmdd 와 헤더 버전으로 제출용 파일명을 만든다."""
    date_match = DATE_PREFIX_PATTERN.match(source.name)
    if date_match is None:
        raise ValueError(f"파일명이 yymmdd_ 로 시작하지 않음: {source.name}")
    version_match = VERSION_PATTERN.search(source.read_text(encoding="utf-8"))
    if version_match is None:
        raise ValueError(f"헤더에 **버전** 줄이 없음: {source.name}")
    return f"{category}_{date_match.group(1)}_{title} v{version_match.group(1)}.md"


def main() -> int:
    parser = argparse.ArgumentParser(description="과제 제출용 문서 익스포트")
    parser.add_argument("--all", action="store_true", help="1차 제출 대상이 아닌 문서도 내보낸다")
    parser.add_argument("--dry-run", action="store_true", help="복사하지 않고 결과만 출력한다")
    args = parser.parse_args()
    sys.stdout.reconfigure(encoding="utf-8")  # Windows 콘솔(cp949)에서 한글 파일명이 깨지지 않게

    DOCS_DIR.mkdir(exist_ok=True)
    for relative_path, category, title, in_first_round in EXPORT_TARGETS:
        if not (args.all or in_first_round):
            continue
        source = REPO_ROOT / relative_path
        target = DOCS_DIR / submission_name(source, category, title)
        print(f"{relative_path} -> docs/{target.name}")
        if not args.dry_run:
            shutil.copyfile(source, target)
    return 0


if __name__ == "__main__":
    sys.exit(main())
