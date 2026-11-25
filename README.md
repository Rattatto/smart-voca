# 스마트보카 (Smart Voca)

> 동사만 알아도 대박이다

영어 일상 대화에 자주 쓰는 **동사 위주의 단어**를 매일 노출하고, 시험으로 복습하는 단어장 웹 앱입니다.
AI기반 프로그래밍 기말 과제로 만들고 있으며, 학교 서버에서 Docker 이미지로 실행됩니다.

## 기술 스택

| 구분 | 내용 |
|---|---|
| 서버 | Python 3.13, Flask 3.1, gunicorn |
| 화면 | Jinja2 템플릿 + 바닐라 JS |
| DB | **SQLite** (파일 위치는 환경변수 `DB_PATH`, 서버에서는 `/data/voca.db`) |
| 배포 | 리포 루트 `Dockerfile`, 점검 워크플로 `.github/workflows/docker-check.yml` |

## 실행 방법

### 개발 PC (Windows)

```bash
cd server
python -m venv .venv
.venv\Scripts\activate
pip install -r requirements.txt
python app.py                         # http://127.0.0.1:8080
python -m unittest discover tests     # 테스트
```

`DB_PATH` 를 주지 않으면 `server/data/voca.db` 를 만들어 씁니다 (커밋하지 않음).

### Docker

```bash
docker build -t smart-voca .
docker run --rm -p 8080:8080 -v voca-data:/data smart-voca
```

| 환경변수 | 기본값 | 설명 |
|---|---|---|
| `DB_PATH` | `/data/voca.db` | SQLite 파일 위치. 처음 실행하면 테이블을 자동으로 만든다 |
| `SECRET_KEY` | (없으면 기동할 때 무작위 생성) | 세션 키. 코드와 이미지에 넣지 않는다 |

## 구현한 요구사항

ID는 과제 「소프트웨어 요건」 v1.1 기준입니다. ✅ 완료 · 🔶 일부 · ⬜ 예정

| ID | 내용 | 상태 |
|---|---|---|
| VOCA-NFR-001 | PC 크롬에서 동작 | ✅ |
| VOCA-NFR-002 | SQLite 에 저장 | 🔶 테이블 준비 (words, study_records, exam_answers) |
| VOCA-NFR-003 | 첫 실행 시 테이블 자동 생성 | ✅ |
| VOCA-NFR-004 | 비밀값은 환경변수 | ✅ |
| VOCA-NFR-005 | 서버 배포 요건 | 🔶 Dockerfile·점검 워크플로 적용 |
| VOCA-FR-001 ~ 004 | 단어 등록·목록·CSV 가져오기·검색 | ⬜ |
| VOCA-FR-011 ~ 014 | 암기 (일차 진도, 복습 섞기, 이력) | ⬜ |
| VOCA-FR-021 ~ 026 | 시험 (문제 수, 가중 출제, 방향, 형식, 채점, 이력) | ⬜ |
| VOCA-FR-031 ~ 034 | 학습 진척 통계 | ⬜ |

## 저장소 구성

| 경로 | 내용 |
|---|---|
| `server/` | 제출용 Flask 앱 (Dockerfile 이 이 폴더만 이미지에 넣는다) |
| `docs/` | 개발 방법론 산출물 (기획서, 요구사항 명세서, 설계서) |
| `ai-agent/` | AI 에이전트 작업 문서와 스크립트 |
| `web/` | 이전 단계의 브라우저 전용 PWA (GitHub Pages, 참고용) |
| `CLAUDE.md` | AI 에이전트 작업 지침 |
