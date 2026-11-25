#!/usr/bin/env python3
# -*- coding: utf-8 -*-
# 2026-11-25 - Smart Voca - Flask 앱 생성, 비밀키·DB 준비, 첫 화면 라우트
# 파일 위치: server/app.py

import os
import secrets

from flask import Flask, render_template

import db


def create_app() -> Flask:
    """앱 팩토리. 테스트에서는 DB_PATH 를 임시 파일로 바꿔 새 앱을 만든다."""
    app = Flask(__name__)
    # R-08: 세션 키는 환경변수로 받고, 없으면 기동할 때마다 무작위로 만든다
    app.config["SECRET_KEY"] = os.environ.get("SECRET_KEY") or secrets.token_hex(32)
    app.config["DB_PATH"] = db.resolve_db_path()
    db.init_db(app.config["DB_PATH"])

    @app.get("/")
    def index():
        # 첫 화면에서 DB 를 실제로 읽어, 서버에서 DB 가 동작하는지 바로 보이게 한다
        with db.connect(app.config["DB_PATH"]) as connection:
            word_count = connection.execute("SELECT COUNT(*) FROM words").fetchone()[0]
        return render_template("index.html", word_count=word_count)

    return app


# gunicorn 이 "app:app" 으로 이 객체를 찾는다
app = create_app()

if __name__ == "__main__":
    # 개발 PC 확인용. 서버에서는 Dockerfile 의 gunicorn 으로 띄운다 (R-14)
    app.run(host="127.0.0.1", port=8080)
