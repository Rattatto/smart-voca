// 2026-11-25 - Smart Voca - 설정 값 저장과 읽기 (FR-040~044). Kotlin SettingsStore.kt (DataStore) 이관, 저장은 IndexedDB settings 테이블
// 파일 위치: web/src/data/settingsStore.ts

import type { LocalDate } from '../domain/localDate'
import { type Level, parseLevel } from '../domain/types'
import type { SmartVocaDb } from './db'

/** 화면과 선정 규칙이 쓰는 설정 값 묶음. `levelFilter` 가 null 이면 전체 난이도다. */
export interface AppSettings {
  dailyCount: number
  levelFilter: Level | null
  verbRatio: number
  examSize: number
}

export const DAILY_COUNT_RANGE = { min: 5, max: 30 } as const
export const EXAM_SIZE_RANGE = { min: 5, max: 20 } as const
export const VERB_RATIO_VALUES = [30, 40, 50, 60, 70, 80, 90] as const

export const DEFAULT_SETTINGS: Readonly<AppSettings> = Object.freeze({
  dailyCount: 10,
  levelFilter: null,
  verbRatio: 60,
  examSize: 10,
})

const KEY = {
  dailyCount: 'daily_count',
  levelFilter: 'level_filter',
  verbRatio: 'verb_ratio',
  examSize: 'exam_size',
  lastSeenDate: 'last_seen_date',
} as const

const LEVEL_FILTER_ALL = 'all'
const DATE_PATTERN = /^\d{4}-\d{2}-\d{2}$/

const isIntegerIn = (value: unknown, range: { min: number; max: number }): value is number =>
  Number.isInteger(value) && (value as number) >= range.min && (value as number) <= range.max

/**
 * 설정 값을 브라우저에 저장한다. 다시 열어도 유지된다 (FR-044).
 * 값 범위는 여기서 검사한다. 저장된 값이 범위를 벗어나면 기본값으로 되돌려 읽는다.
 */
export class SettingsStore {
  private readonly db: SmartVocaDb

  constructor(db: SmartVocaDb) {
    this.db = db
  }

  async load(): Promise<AppSettings> {
    const values = new Map((await this.db.settings.toArray()).map((row) => [row.key, row.value]))
    const dailyCount = values.get(KEY.dailyCount)
    const verbRatio = values.get(KEY.verbRatio)
    const examSize = values.get(KEY.examSize)
    const storedLevel = values.get(KEY.levelFilter)
    return {
      dailyCount: isIntegerIn(dailyCount, DAILY_COUNT_RANGE) ? dailyCount : DEFAULT_SETTINGS.dailyCount,
      levelFilter: typeof storedLevel === 'string' && storedLevel !== LEVEL_FILTER_ALL ? parseLevel(storedLevel) : null,
      verbRatio: (VERB_RATIO_VALUES as readonly unknown[]).includes(verbRatio) ? (verbRatio as number) : DEFAULT_SETTINGS.verbRatio,
      examSize: isIntegerIn(examSize, EXAM_SIZE_RANGE) ? examSize : DEFAULT_SETTINGS.examSize,
    }
  }

  /** 일일 노출 개수. 5~30, 1 단위 (FR-040). */
  async saveDailyCount(value: number): Promise<void> {
    if (!isIntegerIn(value, DAILY_COUNT_RANGE)) throw new RangeError(`일일 개수는 5~30 이어야 한다: ${value}`)
    await this.db.settings.put({ key: KEY.dailyCount, value })
  }

  /** 난이도. null 은 전체 (FR-041). */
  async saveLevelFilter(level: Level | null): Promise<void> {
    await this.db.settings.put({ key: KEY.levelFilter, value: level ?? LEVEL_FILTER_ALL })
  }

  /** 동사 비중. 30~90, 10 단위 (FR-042). 품사 확보 전까지 저장만 하고 적용하지 않는다. */
  async saveVerbRatio(value: number): Promise<void> {
    if (!(VERB_RATIO_VALUES as readonly number[]).includes(value)) {
      throw new RangeError(`동사 비중은 30~90 의 10 단위여야 한다: ${value}`)
    }
    await this.db.settings.put({ key: KEY.verbRatio, value })
  }

  /** 시험 한 회차의 문제 수. 5~20, 1 단위 (FR-043). */
  async saveExamSize(value: number): Promise<void> {
    if (!isIntegerIn(value, EXAM_SIZE_RANGE)) throw new RangeError(`시험 문제 수는 5~20 이어야 한다: ${value}`)
    await this.db.settings.put({ key: KEY.examSize, value })
  }

  /** 앱이 마지막으로 본 날짜. 처음 실행이거나 값이 깨졌으면 null. 날짜 변경 알림에 쓴다. */
  async lastSeenDate(): Promise<LocalDate | null> {
    const value = (await this.db.settings.get(KEY.lastSeenDate))?.value
    return typeof value === 'string' && DATE_PATTERN.test(value) ? value : null
  }

  async saveLastSeenDate(date: LocalDate): Promise<void> {
    await this.db.settings.put({ key: KEY.lastSeenDate, value: date })
  }
}
