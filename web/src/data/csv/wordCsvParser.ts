// 2026-11-25 - Smart Voca - 단어 CSV 헤더·행을 검증해 저장할 항목과 건너뛴 행을 나눔 (FR-001~005). Kotlin WordCsvParser.kt 이관
// 파일 위치: web/src/data/csv/wordCsvParser.ts

import { parseLevel, type Word } from '../../domain/types'
import { readCsvRecords } from './csvRecordReader'

/** CSV 에서 읽은 단어 한 항목. `id` 는 저장소가 정한다. */
export type WordItem = Omit<Word, 'id'>

/** 건너뛴 행의 원본 줄 번호와 사유 (FR-003). */
export interface SkippedRow {
  lineNumber: number
  reason: string
}

export interface WordImportResult {
  words: WordItem[]
  skipped: SkippedRow[]
}

/** 필수 열이 없으면 적재를 중단한다 (FR-002). 누락된 열 이름을 담는다. */
export class MissingColumnsError extends Error {
  readonly missingColumns: string[]

  constructor(missingColumns: string[]) {
    super(`필수 열이 없습니다: ${missingColumns.join(', ')}`)
    this.name = 'MissingColumnsError'
    this.missingColumns = missingColumns
  }
}

/** 행 하나의 검증 실패. 메시지가 그대로 건너뛴 사유로 쓰인다. */
class RowFormatError extends Error {}

const BYTE_ORDER_MARK = '﻿'
export const REQUIRED_COLUMNS = ['word', 'meaning_ko', 'freq_rank', 'level'] as const
/** 영문자로 시작하고 영문자, 아포스트로피, 하이픈, 공백만 허용 (데이터 설계 §4) */
const WORD_PATTERN = /^[A-Za-z][A-Za-z'\- ]*$/

/**
 * 단어 CSV 를 읽는다. 행 단위 오류는 건너뛰고 기록만 한다.
 * 헤더 오류는 파일 전체가 잘못된 것이므로 예외로 중단한다.
 */
export function parseWordCsv(text: string): WordImportResult {
  // Windows 메모장이 저장한 CSV 는 맨 앞에 BOM 이 붙어 있어 먼저 제거한다
  const withoutBom = text.startsWith(BYTE_ORDER_MARK) ? text.slice(1) : text
  const records = readCsvRecords(withoutBom).filter((record) => !record.fields.every((value) => value.trim() === ''))
  if (records.length === 0) throw new Error('CSV 에 헤더 행이 없습니다.')

  const header = records[0].fields.map((name) => name.trim())
  const missingColumns = REQUIRED_COLUMNS.filter((column) => !header.includes(column))
  if (missingColumns.length > 0) throw new MissingColumnsError([...missingColumns])

  const columnIndexByName = new Map(header.map((name, index) => [name, index]))
  const words: WordItem[] = []
  const skipped: SkippedRow[] = []
  // 같은 빈도 순위가 두 번 나오면 먼저 나온 행을 남긴다 (데이터 설계 §4 의 중복 키)
  const lineByRank = new Map<number, number>()

  for (const record of records.slice(1)) {
    try {
      const item = toWordItem(record.fields, header.length, columnIndexByName)
      const firstLine = lineByRank.get(item.freqRank)
      if (firstLine !== undefined) {
        skipped.push({ lineNumber: record.lineNumber, reason: `freq_rank ${item.freqRank} 중복 (${firstLine}행과 같음)` })
        continue
      }
      lineByRank.set(item.freqRank, record.lineNumber)
      words.push(item)
    } catch (error) {
      if (!(error instanceof RowFormatError)) throw error
      skipped.push({ lineNumber: record.lineNumber, reason: error.message })
    }
  }
  return { words, skipped }
}

function toWordItem(fields: string[], expectedSize: number, columnIndexByName: Map<string, number>): WordItem {
  if (fields.length !== expectedSize) {
    throw new RowFormatError(`열 개수 불일치 (헤더 ${expectedSize}개, 행 ${fields.length}개)`)
  }
  const valueOf = (column: string): string => {
    const index = columnIndexByName.get(column)
    return index === undefined ? '' : fields[index].trim()
  }
  const optionalValueOf = (column: string): string | null => valueOf(column) || null

  const word = valueOf('word')
  if (word === '') throw new RowFormatError('word 누락')
  if (!WORD_PATTERN.test(word)) throw new RowFormatError(`word 형식 오류 (${word})`)

  const meaningKo = valueOf('meaning_ko')
  if (meaningKo === '') throw new RowFormatError('meaning_ko 누락')

  const rankText = valueOf('freq_rank')
  if (rankText === '') throw new RowFormatError('freq_rank 누락')
  // Kotlin toIntOrNull 과 같게 정수 표기만 받는다 ("1.5", "1e3" 은 오류)
  const freqRank = /^[+-]?\d+$/.test(rankText) ? Number(rankText) : NaN
  if (!(freqRank > 0)) throw new RowFormatError(`freq_rank 는 1 이상의 정수여야 함 (${rankText})`)

  const levelText = valueOf('level')
  if (levelText === '') throw new RowFormatError('level 누락')
  const level = parseLevel(levelText)
  if (level === null) throw new RowFormatError(`level 값 오류 (${levelText}, basic·intermediate·advanced 중 하나)`)

  return {
    word,
    pos: optionalValueOf('pos'),
    meaningKo,
    exampleEn: optionalValueOf('example_en'),
    exampleKo: optionalValueOf('example_ko'),
    freqRank,
    level,
    rating: optionalValueOf('rating'),
  }
}
