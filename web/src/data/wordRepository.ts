// 2026-11-25 - Smart Voca - CSV 가져오기 결과를 DB 에 한 트랜잭션으로 저장 (FR-001, FR-005, NFR-003). Kotlin WordRepository.kt 이관
// 파일 위치: web/src/data/wordRepository.ts

import type { Word } from '../domain/types'
import { parseWordCsv, type SkippedRow, type WordItem } from './csv/wordCsvParser'
import type { SmartVocaDb } from './db'

/** 가져오기 한 번의 결과. 저장 개수와 건너뛴 행을 함께 보여 준다 (FR-003). */
export interface ImportSummary {
  inserted: number
  updated: number
  skipped: SkippedRow[]
}

export class WordRepository {
  private readonly db: SmartVocaDb

  constructor(db: SmartVocaDb) {
    this.db = db
  }

  /**
   * CSV 텍스트를 읽어 저장한다. 헤더가 잘못되면 파서가 예외를 던지고 아무것도 저장하지 않는다.
   * 같은 `freqRank` 가 이미 있으면 내용만 갱신하고 `id` 는 그대로 둔다 (데이터 설계 §4).
   * `id` 를 유지해야 학습 기록이 끊기지 않는다.
   *
   * 행마다 조회하지 않고 기존 순위→id 표를 한 번 만든 뒤 한꺼번에 쓴다.
   * IndexedDB 는 요청 한 번의 비용이 커서, 2,000행을 한 줄씩 조회하면 눈에 띄게 느려진다.
   */
  async importCsv(csvText: string): Promise<ImportSummary> {
    const parsed = parseWordCsv(csvText)
    // 중간에 실패하면 전부 되돌린다 (프로세스 설계 P1, 저장 실패 시 전체 롤백)
    return this.db.transaction('rw', this.db.words, async () => {
      const idByRank = new Map((await this.db.words.toArray()).map((word) => [word.freqRank, word.id]))
      const updates: Word[] = []
      const inserts: WordItem[] = []
      for (const item of parsed.words) {
        const existingId = idByRank.get(item.freqRank)
        if (existingId === undefined) inserts.push(item)
        else updates.push({ ...item, id: existingId })
      }
      await this.db.words.bulkPut(updates)
      await this.db.words.bulkAdd(inserts)
      return { inserted: inserts.length, updated: updates.length, skipped: parsed.skipped }
    })
  }

  countWords(): Promise<number> {
    return this.db.words.count()
  }

  /** 전체 단어 (빈도 순). 객관식 보기 후보 등에 쓴다. */
  findAllOrdered(): Promise<Word[]> {
    return this.db.words.orderBy('freqRank').toArray()
  }
}
