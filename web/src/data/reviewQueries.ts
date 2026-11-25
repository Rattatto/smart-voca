// 2026-11-25 - Smart Voca - 오늘 재노출 대상 단어 조회 (FR-021). 일일 목록과 시험이 함께 쓴다. Kotlin ReviewDao.dueWords 이관
// 파일 위치: web/src/data/reviewQueries.ts

import type { LocalDate } from '../domain/localDate'
import type { Word } from '../domain/types'
import type { SmartVocaDb } from './db'

/**
 * 날짜가 되었거나 지난 재노출 대상 단어. 다음 재노출 날짜가 빠른 순, 같으면 빈도 순.
 * `YYYY-MM-DD` 문자열은 사전 순서와 날짜 순서가 같아서 문자열 비교(`belowOrEqual`)로 찾을 수 있다.
 * 재노출이 끝난 단어(`nextReviewDate` = null)는 IndexedDB 인덱스에 들어가지 않으므로 자동으로 빠진다.
 */
export async function findDueWords(db: SmartVocaDb, date: LocalDate): Promise<Word[]> {
  const schedules = await db.reviewSchedules.where('nextReviewDate').belowOrEqual(date).toArray()
  const words = await db.words.bulkGet(schedules.map((schedule) => schedule.wordId))
  const dueDateById = new Map(schedules.map((schedule) => [schedule.wordId, schedule.nextReviewDate ?? '']))
  return words
    .filter((word): word is Word => word !== undefined)
    .sort((a, b) => dueDateById.get(a.id)!.localeCompare(dueDateById.get(b.id)!) || a.freqRank - b.freqRank)
}
