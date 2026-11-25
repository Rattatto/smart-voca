#!/usr/bin/env python3
# -*- coding: utf-8 -*-
# 2026-11-25 - Smart Voca - 배포용 임시 동사 묶음 CSV 생성 (직접 작성한 단어·뜻, 출처 문제 없음)
# 파일 위치: ai-agent/scripts/261125-make_starter_verbs.py
# 사용: python ai-agent/scripts/261125-make_starter_verbs.py  (저장소 루트에서 실행)
import csv

# (영단어, 뜻). 순서가 곧 freq_rank 다 (일상 대화에서 자주 쓰는 순서를 직접 정함)
BASIC = [
    ('go', '가다'), ('come', '오다'), ('get', '얻다, 받다'), ('make', '만들다'), ('take', '가져가다, 타다'),
    ('see', '보다'), ('know', '알다'), ('think', '생각하다'), ('want', '원하다'), ('say', '말하다'),
    ('tell', '말해 주다'), ('give', '주다'), ('find', '찾다, 발견하다'), ('look', '쳐다보다'), ('use', '사용하다'),
    ('work', '일하다'), ('call', '전화하다, 부르다'), ('try', '해 보다'), ('ask', '묻다, 부탁하다'), ('need', '필요하다'),
    ('feel', '느끼다'), ('leave', '떠나다, 두고 가다'), ('put', '놓다, 두다'), ('mean', '의미하다'), ('keep', '계속하다, 간직하다'),
    ('let', '하게 해 주다'), ('help', '돕다'), ('talk', '이야기하다'), ('turn', '돌다, 돌리다'), ('start', '시작하다'),
    ('show', '보여 주다'), ('hear', '듣다, 들리다'), ('play', '놀다, 경기하다'), ('run', '달리다'), ('move', '움직이다, 이사하다'),
    ('like', '좋아하다'), ('live', '살다'), ('believe', '믿다'), ('bring', '가져오다'), ('eat', '먹다'),
]
INTERMEDIATE = [
    ('happen', '일어나다, 생기다'), ('write', '쓰다, 적다'), ('sit', '앉다'), ('stand', '서다'), ('lose', '잃어버리다, 지다'),
    ('pay', '돈을 내다'), ('meet', '만나다'), ('learn', '배우다'), ('change', '바꾸다'), ('understand', '이해하다'),
    ('watch', '지켜보다, 시청하다'), ('follow', '따라가다'), ('stop', '멈추다'), ('speak', '(언어를) 말하다'), ('read', '읽다'),
    ('spend', '(돈·시간을) 쓰다'), ('grow', '자라다, 키우다'), ('open', '열다'), ('walk', '걷다'), ('win', '이기다'),
    ('remember', '기억하다'), ('buy', '사다'), ('wait', '기다리다'), ('send', '보내다'), ('decide', '결정하다'),
    ('sleep', '자다'), ('drink', '마시다'), ('cook', '요리하다'), ('wear', '입다, 착용하다'), ('choose', '고르다'),
    ('forget', '잊다'), ('carry', '들고 다니다'), ('catch', '잡다'), ('close', '닫다'), ('drive', '운전하다'),
    ('visit', '방문하다'), ('sell', '팔다'), ('finish', '끝내다'), ('worry', '걱정하다'), ('love', '사랑하다'),
]
ADVANCED = [
    ('explain', '설명하다'), ('agree', '동의하다'), ('allow', '허락하다'), ('suggest', '제안하다'), ('borrow', '빌리다'),
    ('lend', '빌려주다'), ('prefer', '더 좋아하다'), ('recommend', '추천하다'), ('mention', '언급하다'), ('expect', '기대하다, 예상하다'),
    ('avoid', '피하다'), ('apologize', '사과하다'), ('complain', '불평하다'), ('invite', '초대하다'), ('prepare', '준비하다'),
    ('imagine', '상상하다'), ('describe', '묘사하다'), ('compare', '비교하다'), ('admit', '인정하다'), ('deny', '부인하다'),
    ('refuse', '거절하다'), ('promise', '약속하다'), ('deliver', '배달하다'), ('recover', '회복하다'), ('delay', '미루다'),
    ('cancel', '취소하다'), ('arrange', '마련하다, 정리하다'), ('handle', '다루다, 처리하다'), ('manage', '해내다, 관리하다'), ('encourage', '격려하다'),
    ('persuade', '설득하다'), ('guess', '추측하다'), ('realize', '깨닫다'), ('notice', '알아차리다'), ('celebrate', '축하하다'),
    ('rely', '의지하다'), ('appreciate', '고마워하다'), ('hesitate', '망설이다'), ('convince', '확신시키다'), ('solve', '해결하다'),
]

# 직접 작성한 예문 초안에서 같은 단어의 예문을 가져온다 (검수 전 표시는 빼고 문장만)
examples = {}
with open('ai-agent/claude/update/261111_예문초안.csv', encoding='utf-8-sig') as f:
    for row in csv.DictReader(f):
        examples[row['word']] = (row['example_en'], row['example_ko'])

rows = []
for level, words in (('basic', BASIC), ('intermediate', INTERMEDIATE), ('advanced', ADVANCED)):
    for word, meaning in words:
        en, ko = examples.get(word, ('', ''))
        rows.append([word, 'verb', meaning, en, ko, len(rows) + 1, level])

assert len({r[0] for r in rows}) == len(rows) == 120, '단어 중복 또는 개수 오류'
assert len({r[2] for r in rows}) == len(rows), '뜻 중복 (객관식 보기가 헷갈림)'
with open('web/public/starter_verbs.csv', 'w', encoding='utf-8', newline='') as f:
    writer = csv.writer(f, lineterminator='\n')
    writer.writerow(['word', 'pos', 'meaning_ko', 'example_en', 'example_ko', 'freq_rank', 'level'])
    writer.writerows(rows)
print(len(rows), '개, 예문', sum(1 for r in rows if r[3]), '개')
