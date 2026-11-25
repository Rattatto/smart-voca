// 2026-11-25 - Smart Voca - 날짜 변경 감지 (앱을 열 때 마지막으로 본 날짜와 비교). Kotlin DayChangeRules.kt 이관
// 파일 위치: web/src/domain/dayChangeRules.ts

import type { LocalDate } from './localDate'

/** 마지막으로 본 날짜가 없으면 (처음 실행) 알리지 않는다. 같은 날이면 알리지 않는다. */
export function shouldNotifyDayChange(lastSeen: LocalDate | null, today: LocalDate): boolean {
  return lastSeen !== null && lastSeen !== today
}
