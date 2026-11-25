// 2026-11-25 - Smart Voca - IndexedDB(Dexie) 데이터베이스 정의 (브라우저 내 저장, 서버 없음, ADR-015). Room SmartVocaDatabase 대체
// 파일 위치: web/src/data/db.ts

import Dexie, { type EntityTable, type Table } from 'dexie'
import type { WordSource } from '../domain/dailyWordSelector'
import type { LocalDate } from '../domain/localDate'
import type { StageState } from '../domain/stageRules'
import type { Level, QuestionType, Word } from '../domain/types'

/** 날짜별 일일 목록 한 칸. 같은 날짜의 행은 한 번 만들면 바뀌지 않는다 (FR-013). */
export interface DailyListRow {
  date: LocalDate
  wordId: number
  /** 목록 안의 순서. 1부터 */
  position: number
  source: WordSource
  /** 생성 시점의 설정 스냅샷 (FR-045) */
  dailyCount: number
  levelFilter: Level | 'all'
  /** 노출 시각 (ISO 8601, FR-015). 목록은 화면을 열 때 만들므로 만든 시각이 곧 노출 시각이다 */
  shownAt: string
}

/** 학습 완료 한 번. 같은 날 여러 번 눌러도 매번 남긴다. */
export interface StudyRecord {
  id: number
  wordId: number
  studiedAt: string
  studyDate: LocalDate
}

/** 단어별 재노출 일정. `nextReviewDate` 가 null 이면 재노출이 끝난 단어다 (FR-023). */
export interface ReviewSchedule {
  wordId: number
  step: number
  nextReviewDate: LocalDate | null
  updatedAt: string
}

/** 시험 답 한 번 (FR-036). 재노출 일정은 바꾸지 않는다 (FR-037). */
export interface ExamAttempt {
  id: number
  wordId: number
  attemptDate: LocalDate
  attemptedAt: string
  /** 문제로 보여 준 뜻 (스냅샷) */
  shownMeaning: string
  /** 사용자가 입력하거나 고른 값 (원문) */
  userInput: string
  isCorrect: boolean
  questionType: QuestionType
}

/** 단어별 시험 단계 (ADR-012). */
export interface WordStageRow extends StageState {
  wordId: number
}

/** 설정 한 항목 (키-값). DataStore 대신 같은 DB 에 둔다. */
export interface SettingRow {
  key: string
  value: string | number
}

export type SmartVocaDb = Dexie & {
  words: EntityTable<Word, 'id'>
  dailyList: Table<DailyListRow, [LocalDate, number]>
  studyRecords: EntityTable<StudyRecord, 'id'>
  reviewSchedules: EntityTable<ReviewSchedule, 'wordId'>
  examAttempts: EntityTable<ExamAttempt, 'id'>
  wordStages: EntityTable<WordStageRow, 'wordId'>
  settings: EntityTable<SettingRow, 'key'>
}

export const DATABASE_NAME = 'smart_voca'

/**
 * DB 를 연다. 테스트는 이름을 바꿔 매번 빈 DB 로 시작한다.
 *
 * 스키마 문자열: 첫 항목이 기본 키 (`++` 는 자동 증가, `&` 는 고유 인덱스, `[a+b]` 는 복합 키), 나머지는 인덱스.
 * 인덱스가 아닌 필드도 저장은 된다. 검색할 필드만 적는다.
 *
 * 스키마를 바꿀 때는 version(1) 을 고치지 말고 version(2).stores(...).upgrade(...) 를 추가한다.
 * 기존 사용자 데이터를 지우는 변경은 하지 않는다 (11/11 요청 2.2 C).
 */
export function openDatabase(name: string = DATABASE_NAME): SmartVocaDb {
  const db = new Dexie(name) as SmartVocaDb
  db.version(1).stores({
    words: '++id, &freqRank, level',
    dailyList: '[date+wordId], date',
    studyRecords: '++id, wordId, studyDate, [wordId+studyDate]',
    reviewSchedules: 'wordId, nextReviewDate',
    examAttempts: '++id, wordId, attemptDate',
    wordStages: 'wordId',
    settings: 'key',
  })
  return db
}
