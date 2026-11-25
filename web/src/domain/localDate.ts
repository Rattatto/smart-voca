// 2026-11-25 - Smart Voca - 날짜(시각 없음) 계산 도우미. Kotlin LocalDate 대체
// 파일 위치: web/src/domain/localDate.ts

/**
 * 날짜는 `YYYY-MM-DD` 문자열로 다룬다. 문자열이라 DB 에 그대로 저장하고 `===` 로 비교할 수 있다.
 * JS Date 를 그대로 쓰면 시간대 때문에 하루가 밀릴 수 있어, 계산은 UTC 기준 일수로만 한다.
 */
export type LocalDate = string

const MS_PER_DAY = 86_400_000

/** 1970-01-01 부터 센 일수 (Kotlin `toEpochDay` 와 같다). */
export function toEpochDay(date: LocalDate): number {
  const [year, month, day] = date.split('-').map(Number)
  return Math.floor(Date.UTC(year, month - 1, day) / MS_PER_DAY)
}

export function fromEpochDay(epochDay: number): LocalDate {
  return new Date(epochDay * MS_PER_DAY).toISOString().slice(0, 10)
}

export function addDays(date: LocalDate, days: number): LocalDate {
  return fromEpochDay(toEpochDay(date) + days)
}

export function daysBetween(from: LocalDate, to: LocalDate): number {
  return toEpochDay(to) - toEpochDay(from)
}

/** 기기(브라우저)의 현지 날짜. 하루의 기준은 사용자가 있는 곳의 자정이다. */
export function todayLocal(now: Date = new Date()): LocalDate {
  const pad = (value: number) => String(value).padStart(2, '0')
  return `${now.getFullYear()}-${pad(now.getMonth() + 1)}-${pad(now.getDate())}`
}
