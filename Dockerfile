# 2026-11-25 - Smart Voca - 학교 서버 배포용 이미지 (Flask + gunicorn + SQLite). 규칙 R-01~R-16
# 파일 위치: Dockerfile (리포 루트, R-01)

# R-02: 공식 이미지 + 버전 태그
FROM python:3.13-slim

ENV PYTHONDONTWRITEBYTECODE=1 \
    PYTHONUNBUFFERED=1 \
    DB_PATH=/data/voca.db

WORKDIR /app

# R-13: 의존성 파일을 먼저 복사해 설치한다 (소스만 바뀌면 이 단계는 캐시를 쓴다)
COPY server/requirements.txt ./requirements.txt
RUN pip install --no-cache-dir -r requirements.txt

COPY server/ ./

# R-05, R-06: UID 10001 사용자를 만들고 /data 소유자를 넘긴다 (빌드 때 root 로 미리 해 둔다)
RUN useradd --uid 10001 --no-create-home --shell /usr/sbin/nologin voca \
    && mkdir -p /data \
    && chown 10001:10001 /data

USER 10001

# R-03: 0.0.0.0:8080. R-14: 운영 서버 gunicorn. R-15: exec 형식
# --preload: 마스터가 앱을 한 번 만들어 테이블 생성을 한 번만 한다
CMD ["gunicorn", "--bind", "0.0.0.0:8080", "--workers", "2", "--preload", "app:app"]
