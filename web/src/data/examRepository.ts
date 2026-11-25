// 2026-11-25 - Smart Voca - 시험 출제 구성, 시험 이력과 단계 저장 (FR-031, FR-036, FR-037, FR-038). Kotlin ExamRepository.kt 이관
// 파일 위치: web/src/data/examRepository.ts

import { type ExamQuestion, planExam } from '../domain/examRules'
import type { LocalDate } from '../domain/localDate'
import type { Rng } from '../domain/random'
import { afterAnswer, DEFAULT_STAGE, type StageState } from '../domain/stageRules'
import type { QuestionType, Word } from '../domain/types'
import type { ExamAttempt, SmartVocaDb } from './db'
import { findDueWords } from './reviewQueries'

/** 답 한 번의 입력 값. */
export interface AttemptInput {
  wordId: number
  date: LocalDate
  shownMeaning: string
  userInput: string
  isCorrect: boolean
  type: QuestionType
  now?: Date
}

export class ExamRepository {
  private readonly db: SmartVocaDb

  constructor(db: SmartVocaDb) {
    this.db = db
  }

  /**
   * 오늘 시험 문제를 만든다 (P4). 후보는 `todayWords` 와 오늘 재노출 대상이다.
   * 단계 (객관식/주관식)에 따라 문제 형식을 정하고, 객관식 보기는 전체 단어에서 뽑는다.
   */
  async buildQuestions(date: LocalDate, examSize: number, todayWords: readonly Word[], rng: Rng = Math.random): Promise<ExamQuestion[]> {
    const { db } = this
    const reviewWords = await findDueWords(db, date)
    const candidates = [...new Map([...todayWords, ...reviewWords].map((word) => [word.id, word])).values()]
    const typedIds = new Set((await db.wordStages.toArray()).filter((stage) => stage.type === 'typed').map((stage) => stage.wordId))
    const pool = await db.words.orderBy('freqRank').toArray()
    return planExam(candidates, examSize, (word) => typedIds.has(word.id), pool, rng)
  }

  /**
   * 답 한 번을 저장한다 (FR-036). 같은 트랜잭션에서 단어의 시험 단계를 갱신하고 새 단계를 돌려준다 (FR-038).
   * 재노출 일정은 바꾸지 않는다 (FR-037).
   */
  async recordAttempt(input: AttemptInput): Promise<StageState> {
    const { db } = this
    const now = input.now ?? new Date()
    return db.transaction('rw', [db.examAttempts, db.wordStages], async () => {
      await db.examAttempts.add({
        wordId: input.wordId,
        attemptDate: input.date,
        attemptedAt: now.toISOString(),
        shownMeaning: input.shownMeaning,
        userInput: input.userInput,
        isCorrect: input.isCorrect,
        questionType: input.type,
      })
      const before = await this.stageOf(input.wordId)
      const after = afterAnswer(before, input.type, input.isCorrect, input.date, now)
      await db.wordStages.put({ ...after, wordId: input.wordId })
      return after
    })
  }

  /** 단어의 현재 시험 단계. 기록이 없으면 객관식. */
  async stageOf(wordId: number): Promise<StageState> {
    const row = await this.db.wordStages.get(wordId)
    if (!row) return DEFAULT_STAGE
    const { wordId: _ignored, ...state } = row
    return state
  }

  /** 날짜의 시험 이력 (입력 순서). */
  attemptsOn(date: LocalDate): Promise<ExamAttempt[]> {
    return this.db.examAttempts.where('attemptDate').equals(date).sortBy('id')
  }
}
