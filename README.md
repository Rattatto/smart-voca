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
| VOCA-NFR-002 | SQLite 에 저장 | ✅ words, study_records, exam_answers, settings |
| VOCA-NFR-003 | 첫 실행 시 테이블 자동 생성 | ✅ 임시 동사 120개도 첫 실행 때 1회 적재 |
| VOCA-NFR-004 | 비밀값은 환경변수 | ✅ |
| VOCA-NFR-005 | 서버 배포 요건 | ✅ 점검 워크플로 통과 |
| VOCA-FR-001 | 영단어·뜻 직접 등록 | ✅ |
| VOCA-FR-002 | 목록, 수정·삭제 | ✅ |
| VOCA-FR-003 | CSV 일괄 등록 (설정 화면) | ✅ 중복 처리 방침은 아래 |
| VOCA-FR-004 | 영단어·뜻 검색 | ✅ |
| VOCA-FR-061 | 설정 화면 | 🔶 화면과 머리말 버튼만. 학습량·시험 기본값은 예정 |
| VOCA-FR-011 ~ 014 | 암기 (일차 진도, 복습 섞기, 이력) | ⬜ |
| VOCA-FR-021 ~ 026 | 시험 (문제 수, 가중 출제, 방향, 형식, 채점, 이력) | ⬜ |
| VOCA-FR-031 ~ 034 | 학습 진척 통계 | ⬜ |

## 단어 데이터와 중복 처리 (VOCA-FR-003)

- 첫 실행 때 직접 작성한 **임시 동사 120개**(`server/seed/starter_verbs.csv`)가 들어갑니다.
- 더 많은 단어는 **설정 → 단어 CSV 가져오기**에서 올립니다. 헤더는 `영단어,뜻` 또는 `word,meaning_ko,...` 를 받습니다.
- **같은 단어의 기준**: 철자가 대소문자 무시하고 같으면 같은 단어입니다.
- **파일 안 중복**: 하나로 합치고 뜻을 `; ` 로 잇습니다 (예: `like` → `좋아하다; ~처럼`). 빈도 순위는 작은 값을 씁니다.
- **이미 등록된 단어**: 가져오기는 뜻을 파일 값으로 바꿉니다 (학습 기록 유지). 직접 등록은 덮어쓰지 않고 수정 화면으로 안내합니다.
- 상세: `ai-agent/claude/update/261125_요구사항명세서.md` §4

## 저장소 구성

| 경로 | 내용 |
|---|---|
| `server/` | 제출용 Flask 앱 (Dockerfile 이 이 폴더만 이미지에 넣는다) |
| `docs/` | 개발 방법론 산출물 (기획서, 요구사항 명세서, 설계서) |
| `ai-agent/` | AI 에이전트 작업 문서와 스크립트 |
| `web/` | 이전 단계의 브라우저 전용 PWA (GitHub Pages, 참고용) |
| `CLAUDE.md` | AI 에이전트 작업 지침 |
