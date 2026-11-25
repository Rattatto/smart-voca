// 2026-11-25 - Smart Voca - 재노출 단계와 다음 날짜 계산 (FR-020~023, P3, PR-03). Kotlin ReviewScheduler.kt 이관
// 파일 위치: web/src/domain/reviewScheduler.ts

import { addDays, type LocalDate } from './localDate'

/** 단계 0 ~ 4 의 간격 (일). 학습 후 1, 3, 7, 14, 30일 (FR-021). */
const INTERVAL_DAYS = [1, 3, 7, 14, 30] as const

/** 이 단계를 넘으면 재노출이 끝난다 (FR-023). */
const LAST_STEP = INTERVAL_DAYS.length - 1

/** 처음 학습하면 0, 이미 일정이 있으면 한 단계 올린다. */
export function nextStep(currentStep: number | null): number {
  return currentStep === null ? 0 : currentStep + 1
}

/** 단계에 해당하는 다음 재노출 날짜. 끝난 단계면 null. */
export function nextReviewDate(today: LocalDate, step: number): LocalDate | null {
  return step > LAST_STEP ? null : addDays(today, INTERVAL_DAYS[step])
}
