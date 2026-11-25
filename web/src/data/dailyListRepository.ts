// 2026-11-25 - Smart Voca - 날짜별 일일 목록 조회와 생성 (FR-010, FR-011, FR-013, FR-014, FR-015). Kotlin DailyListRepository.kt 이관
// 파일 위치: web/src/data/dailyListRepository.ts

import { composeDailyList, type PickedWord } from '../domain/dailyWordSelector'
import type { LocalDate } from '../domain/localDate'
import type { Level, Word } from '../domain/types'
import type { DailyListRow, SmartVocaDb } from './db'
import { findDueWords } from './reviewQueries'

export class DailyListRepository {
  private readonly db: SmartVocaDb
  private readonly excludedWords: ReadonlySet<string>

  /** `excludedWords` 는 소문자 기능어 집합이다. 일일 목록에 들어가지 않는다. */
  constructor(db: SmartVocaDb, excludedWords: ReadonlySet<string> = new Set()) {
    this.db = db
    this.excludedWords = excludedWords
  }

  /**
   * `date` 의 일일 목록을 돌려준다.
   * 목록이 이미 있으면 저장된 그대로 돌려준다 (FR-013). 없으면 재노출 후보와 신규 단어로 새로 만들어 저장한다.
   * 난이도 필터와 기능어 제외는 재노출 후보와 신규 단어 모두에 적용한다 (FR-011).
   * 단어가 한 개도 없으면 목록을 저장하지 않고 빈 배열을 돌려준다.
   */
  async getOrCreate(
    date: LocalDate,
    dailyCount: number,
    levelFilter: Level | null,
    now: Date = new Date(),
  ): Promise<PickedWord[]> {
    const { db } = this
    return db.transaction('rw', [db.dailyList, db.words, db.studyRecords, db.reviewSchedules], async () => {
      const saved = await this.findRows(date)
      if (saved.length > 0) {
        const words = await db.words.bulkGet(saved.map((row) => row.wordId))
        return saved.flatMap((row, index) => {
          const word = words[index]
          return word ? [{ word, source: row.source }] : []
        })
      }

      const isUsable = (word: Word) =>
        (levelFilter === null || word.level === levelFilter) && !this.excludedWords.has(word.word.toLowerCase())
      const reviewCandidates = (await findDueWords(db, date)).filter(isUsable)
      const studiedIds = new Set(await db.studyRecords.orderBy('wordId').uniqueKeys())
      const newCandidates = (await db.words.orderBy('freqRank').toArray()).filter(
        (word) => !studiedIds.has(word.id) && isUsable(word),
      )
      const picked = composeDailyList(dailyCount, reviewCandidates, newCandidates)
      if (picked.length === 0) return []

      const rows: DailyListRow[] = picked.map((item, index) => ({
        date,
        wordId: item.word.id,
        position: index + 1,
        source: item.source,
        dailyCount,
        levelFilter: levelFilter ?? 'all',
        shownAt: now.toISOString(),
      }))
      await db.dailyList.bulkAdd(rows)
      return picked
    })
  }

  /** 날짜의 목록 행을 순서대로 돌려준다. 노출 시각 확인용 (FR-015). */
  findRows(date: LocalDate): Promise<DailyListRow[]> {
    return this.db.dailyList.where('date').equals(date).sortBy('position')
  }
}
