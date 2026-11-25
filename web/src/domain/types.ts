// 2026-11-25 - Smart Voca - 도메인 공용 타입 (단어, 난이도, 문제 형식). Kotlin WordEntity, Level, QuestionType 이관
// 파일 위치: web/src/domain/types.ts

/** 난이도. CSV 와 DB 에 이 문자열 그대로 저장한다 (ADR-007). */
export const LEVELS = ['basic', 'intermediate', 'advanced'] as const
export type Level = (typeof LEVELS)[number]

export function parseLevel(value: string): Level | null {
  return (LEVELS as readonly string[]).includes(value) ? (value as Level) : null
}

/**
 * 저장된 단어 한 개. `id` 는 내부 식별자이며 학습 기록이 이 값으로 단어를 가리킨다.
 * 재가져오기 때도 `freqRank` 가 같으면 같은 `id` 를 유지한다 (1차 중복 키).
 */
export interface Word {
  id: number
  word: string
  pos: string | null
  meaningKo: string
  exampleEn: string | null
  exampleKo: string | null
  freqRank: number
  level: Level
  rating: string | null
}

/** 문제 형식. 객관식은 영단어를 보고 뜻을 고르고, 주관식은 뜻을 보고 영단어를 입력한다. */
export type QuestionType = 'mc' | 'typed'
