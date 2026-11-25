// 2026-11-25 - Smart Voca - 기능어 목록과 일일 TIP 선택. Kotlin FunctionWords.kt 이관
// 파일 위치: web/src/domain/functionWords.ts

import { type LocalDate, toEpochDay } from './localDate'

/** 기능어 한 개와 그 쓰임 TIP. 일일 목록에서는 빼고, 대신 TIP 칸에 보여 준다. */
export interface FunctionWordTip {
  word: string
  category: string
  tipKo: string
}

/** `public/` 에 두는 기능어 파일 이름. 형식은 `word,category,tip_ko` 이며 TIP 에는 쉼표를 쓰지 않는다. */
export const FUNCTION_WORDS_FILE_NAME = 'function_words.csv'

export class FunctionWords {
  /** 소문자 기준 기능어 집합. 일일 목록 선정에서 이 단어를 뺀다. */
  readonly words: ReadonlySet<string>
  readonly tips: readonly FunctionWordTip[]

  constructor(tips: readonly FunctionWordTip[]) {
    this.tips = tips
    this.words = new Set(tips.map((tip) => tip.word.toLowerCase()))
  }

  /** 날짜마다 다른 TIP 을 고른다. 같은 날짜에는 항상 같은 TIP 이다. */
  dailyTip(date: LocalDate): FunctionWordTip | null {
    if (this.tips.length === 0) return null
    return this.tips[toEpochDay(date) % this.tips.length]
  }

  static parse(csvText: string): FunctionWords {
    const tips = csvText
      .split(/\r?\n/)
      .slice(1) // 헤더
      .filter((line) => line.trim() !== '')
      .map((line) => {
        // Kotlin split(limit = 3) 과 같게: 앞의 쉼표 두 개로만 나눈다
        const first = line.indexOf(',')
        const second = first < 0 ? -1 : line.indexOf(',', first + 1)
        if (second < 0) throw new Error(`기능어 CSV 형식 오류: ${line}`)
        return {
          word: line.slice(0, first).trim(),
          category: line.slice(first + 1, second).trim(),
          tipKo: line.slice(second + 1).trim(),
        }
      })
    return new FunctionWords(tips)
  }
}
