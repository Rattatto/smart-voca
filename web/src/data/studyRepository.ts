// 2026-11-25 - Smart Voca - 학습 완료 기록과 재노출 일정 저장 (FR-020, FR-022, PR-03). Kotlin StudyRepository.kt 이관
// 파일 위치: web/src/data/studyRepository.ts

import type { LocalDate } from '../domain/localDate'
import { nextReviewDate, nextStep } from '../domain/reviewScheduler'
import type { SmartVocaDb } from './db'

export class StudyRepository {
  private readonly db: SmartVocaDb

  constructor(db: SmartVocaDb) {
    this.db = db
  }

  /**
   * 학습 완료를 기록한다.
   * 학습 행은 매번 추가한다. 재노출 일정은 같은 날 같은 단어가 처음 완료될 때만 올린다 (PR-03).
   * 시험 결과는 이 함수와 관계없다 (FR-022, FR-037).
   */
  async markStudied(wordId: number, date: LocalDate, now: Date = new Date()): Promise<void> {
    const { db } = this
    await db.transaction('rw', [db.studyRecords, db.reviewSchedules], async () => {
      const alreadyStudiedToday = (await db.studyRecords.where('[wordId+studyDate]').equals([wordId, date]).count()) > 0
      await db.studyRecords.add({ wordId, studiedAt: now.toISOString(), studyDate: date })
      if (alreadyStudiedToday) return

      const current = await db.reviewSchedules.get(wordId)
      const step = nextStep(current?.step ?? null)
      await db.reviewSchedules.put({
        wordId,
        step,
        nextReviewDate: nextReviewDate(date, step),
        updatedAt: now.toISOString(),
      })
    })
  }

  /** 날짜에 학습 완료한 단어 id 들. 목록의 완료 표시에 쓴다. */
  async studiedIdsOn(date: LocalDate): Promise<Set<number>> {
    const records = await this.db.studyRecords.where('studyDate').equals(date).toArray()
    return new Set(records.map((record) => record.wordId))
  }
}
