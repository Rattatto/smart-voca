// 2026-11-25 - Smart Voca - 테스트용 단어 생성 도우미 (테스트 파일에서만 사용)
// 파일 위치: web/src/domain/testWords.ts

import type { Level, Word } from './types'

export function testWord(rank: number, level: Level = 'basic'): Word {
  return {
    id: rank,
    word: `word${rank}`,
    pos: null,
    meaningKo: `뜻${rank}`,
    exampleEn: null,
    exampleKo: null,
    freqRank: rank,
    level,
    rating: null,
  }
}

export function range(from: number, to: number): number[] {
  return Array.from({ length: to - from + 1 }, (_, index) => from + index)
}
