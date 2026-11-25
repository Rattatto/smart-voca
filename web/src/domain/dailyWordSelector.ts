// 2026-11-25 - Smart Voca - 일일 목록 구성 규칙 (FR-010, FR-011, FR-014, PR-02). Kotlin DailyWordSelector.kt 이관
// 파일 위치: web/src/domain/dailyWordSelector.ts

import type { Word } from './types'

export type WordSource = 'review' | 'new'

/** 목록 한 칸. `source` 는 재노출이면 `review`, 신규면 `new` 다. */
export interface PickedWord {
  word: Word
  source: WordSource
}

/** 재노출 상한 비율 (FR-014, OI-08). 상한은 `floor(dailyCount × 0.3)`. */
const REVIEW_RATIO_PERCENT = 30

/**
 * 일일 목록을 구성한다 (프로세스 설계서 P2 단계 2~4). 같은 입력이면 같은 결과를 낸다 (FR-013).
 * 후보는 이미 난이도와 기능어 조건으로 걸러져 있어야 한다. 이 함수는 개수와 순서만 정한다.
 * - `reviewCandidates`: 우선순위 순서 (다음 재노출 날짜, 빈도 순)
 * - `newCandidates`: 빈도 순서
 *
 * 순서: 재노출 (상한까지) → 재노출 (상한 초과분, 신규가 부족할 때만) → 신규.
 */
export function composeDailyList(
  dailyCount: number,
  reviewCandidates: readonly Word[],
  newCandidates: readonly Word[],
): PickedWord[] {
  if (dailyCount <= 0) throw new RangeError(`dailyCount 는 1 이상이어야 한다: ${dailyCount}`)

  const reviewCap = Math.floor((dailyCount * REVIEW_RATIO_PERCENT) / 100)
  const cappedReviews = reviewCandidates.slice(0, reviewCap)
  const pickedNew = newCandidates.slice(0, dailyCount - cappedReviews.length)
  const overflowReviews = reviewCandidates
    .slice(cappedReviews.length)
    .slice(0, dailyCount - cappedReviews.length - pickedNew.length)

  return [
    ...cappedReviews.map((word): PickedWord => ({ word, source: 'review' })),
    ...overflowReviews.map((word): PickedWord => ({ word, source: 'review' })),
    ...pickedNew.map((word): PickedWord => ({ word, source: 'new' })),
  ]
}
