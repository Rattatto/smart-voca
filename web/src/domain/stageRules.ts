// 2026-11-25 - Smart Voca - 단어별 시험 단계 (객관식 → 주관식 승격, 주관식 오답 시 강등) (FR-038, ADR-012). Kotlin StageRules.kt 이관
// 파일 위치: web/src/domain/stageRules.ts

import { addDays, type LocalDate } from './localDate'
import type { QuestionType } from './types'

/** 단어 하나의 시험 단계 상태. `promotedAt` 은 승격 시각 (ISO 문자열). */
export interface StageState {
  type: QuestionType
  streakDays: number
  lastCountedDate: LocalDate | null
  promotedAt: string | null
}

/** 기록이 없는 단어의 상태: 객관식, 연속 0일. */
export const DEFAULT_STAGE: StageState = Object.freeze({
  type: 'mc',
  streakDays: 0,
  lastCountedDate: null,
  promotedAt: null,
})

/** 객관식을 이 일수만큼 연속으로 맞히면 주관식으로 승격한다 (D2). */
export const PROMOTE_STREAK_DAYS = 3

/**
 * 답 한 번이 끝난 뒤의 단계를 돌려준다. 입력 상태는 바꾸지 않고 새 객체를 돌려준다.
 * - 객관식 정답: 하루에 한 번만 센다 (D3). 어제 이어서 맞혔으면 +1, 끊겼으면 1부터. 기준에 닿으면 승격.
 * - 객관식 오답: 연속 일수를 0으로.
 * - 주관식 정답: 변화 없음.
 * - 주관식 오답: 객관식으로 강등, 연속 일수 0 (D4 변경).
 * 시험 결과는 재노출 일정과 무관하다 (FR-037).
 */
export function afterAnswer(
  state: StageState,
  answeredType: QuestionType,
  isCorrect: boolean,
  today: LocalDate,
  now: Date,
): StageState {
  if (answeredType === 'typed') {
    return isCorrect ? state : { ...state, type: 'mc', streakDays: 0, lastCountedDate: null }
  }
  if (!isCorrect) return { ...state, streakDays: 0, lastCountedDate: null }
  if (state.lastCountedDate === today) return state

  const continued = state.lastCountedDate === addDays(today, -1)
  const streak = continued ? state.streakDays + 1 : 1
  if (state.type === 'mc' && streak >= PROMOTE_STREAK_DAYS) {
    return { ...state, type: 'typed', streakDays: 0, lastCountedDate: today, promotedAt: now.toISOString() }
  }
  return { ...state, streakDays: streak, lastCountedDate: today }
}
