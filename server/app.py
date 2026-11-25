#!/usr/bin/env python3
# -*- coding: utf-8 -*-
# 2026-11-25 - Smart Voca - Flask 앱 생성, 비밀키·DB 준비, 화면 라우트 (첫 화면, 단어, 설정)
# 파일 위치: server/app.py
# 주소는 "/words" 처럼 / 로 시작하는 문자열로 쓴다. 학교 서버가 앞에 /2026/voca-<학번>/ 을 붙인다 (R-04)

import os
import secrets

from flask import Flask, abort, flash, redirect, render_template, request

import db
import word_repository as words
from csv_import import CsvFormatError, decode_csv_bytes, parse_word_csv

MAX_UPLOAD_BYTES = 20 * 1024 * 1024   # 학교 서버 업로드 한도와 같게


def create_app() -> Flask:
    """앱 팩토리. 테스트에서는 DB_PATH 를 임시 파일로 바꿔 새 앱을 만든다."""
    app = Flask(__name__)
    # R-08: 세션 키는 환경변수로 받고, 없으면 기동할 때마다 무작위로 만든다
    app.config["SECRET_KEY"] = os.environ.get("SECRET_KEY") or secrets.token_hex(32)
    app.config["MAX_CONTENT_LENGTH"] = MAX_UPLOAD_BYTES
    app.config["DB_PATH"] = db.resolve_db_path()
    db.init_db(app.config["DB_PATH"])
    with db.connect(app.config["DB_PATH"]) as connection:
        words.seed_starter_words(connection)

    def open_db():
        return db.connect(app.config["DB_PATH"])

    @app.get("/")
    def index():
        with open_db() as connection:
            word_count = words.count_words(connection)
        return render_template("index.html", word_count=word_count)

    # ── 단어 (VOCA-FR-001, 002, 004) ──

    @app.get("/words")
    def word_list():
        query = request.args.get("q", "")
        page = request.args.get("page", 1, type=int)
        with open_db() as connection:
            result = words.search_words(connection, query, page)
        return render_template("words.html", result=result, query=query)

    @app.post("/words")
    def word_add():
        word, meaning = request.form.get("word", ""), request.form.get("meaning", "")
        if not word.strip() or not meaning.strip():
            flash("영단어와 뜻을 모두 입력해 주세요.", "error")
            return redirect("/words")
        try:
            with open_db() as connection:
                words.add_word(connection, word, meaning)
            flash(f"'{word.strip()}' 을(를) 등록했습니다.", "ok")
        except words.DuplicateWordError as error:
            flash(f"'{word.strip()}' 은(는) 이미 등록된 단어입니다. 뜻을 바꾸려면 수정하세요.", "error")
            return redirect(f"/words/{error.existing_id}/edit")
        return redirect("/words")

    @app.get("/words/<int:word_id>/edit")
    def word_edit(word_id: int):
        with open_db() as connection:
            word = words.get_word(connection, word_id)
        if word is None:
            abort(404)
        return render_template("word_edit.html", word=word)

    @app.post("/words/<int:word_id>/edit")
    def word_update(word_id: int):
        word, meaning = request.form.get("word", ""), request.form.get("meaning", "")
        if not word.strip() or not meaning.strip():
            flash("영단어와 뜻을 모두 입력해 주세요.", "error")
            return redirect(f"/words/{word_id}/edit")
        try:
            with open_db() as connection:
                if words.get_word(connection, word_id) is None:
                    abort(404)
                words.update_word(connection, word_id, word, meaning)
        except words.DuplicateWordError:
            flash(f"'{word.strip()}' 은(는) 다른 단어로 이미 등록되어 있습니다.", "error")
            return redirect(f"/words/{word_id}/edit")
        flash(f"'{word.strip()}' 을(를) 수정했습니다.", "ok")
        return redirect("/words")

    @app.post("/words/<int:word_id>/delete")
    def word_delete(word_id: int):
        with open_db() as connection:
            word = words.get_word(connection, word_id)
            if word is None:
                abort(404)
            words.delete_word(connection, word_id)
        flash(f"'{word['word']}' 을(를) 삭제했습니다.", "ok")
        return redirect("/words")

    # ── 설정 (CSV 가져오기 VOCA-FR-003, 설정 항목은 VOCA-FR-061 에서 추가) ──

    @app.get("/settings")
    def settings():
        return render_template("settings.html", report=None)

    @app.post("/settings/import")
    def settings_import():
        upload = request.files.get("csv_file")
        if upload is None or not upload.filename:
            flash("가져올 CSV 파일을 골라 주세요.", "error")
            return redirect("/settings")
        try:
            # 서버는 /data, /tmp 에만 쓸 수 있으므로 파일로 저장하지 않고 메모리에서 처리한다
            parsed = parse_word_csv(decode_csv_bytes(upload.read()))
        except CsvFormatError as error:
            flash(str(error), "error")
            return redirect("/settings")
        summary = None
        if not parsed.missing_columns:
            with open_db() as connection:
                summary = words.import_words(connection, parsed.items)
        report = {"file_name": upload.filename, "parsed": parsed, "summary": summary}
        return render_template("settings.html", report=report)

    @app.errorhandler(413)
    def upload_too_large(_error):
        flash("파일이 너무 큽니다 (20MB 까지).", "error")
        return redirect("/settings")

    return app


# gunicorn 이 "app:app" 으로 이 객체를 찾는다
app = create_app()

if __name__ == "__main__":
    # 개발 PC 확인용. 서버에서는 Dockerfile 의 gunicorn 으로 띄운다 (R-14)
    app.run(host="127.0.0.1", port=8080)
