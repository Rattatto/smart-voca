// 2026-11-25 - Smart Voca - 시험 출제, 객관식 보기, 채점 규칙 (FR-031, FR-033, FR-035, FR-038, ADR-012). Kotlin ExamRules.kt 이관
// 파일 위치: web/src/domain/examRules.ts

import { nextInt, type Rng, shuffled } from './random'
import type { QuestionType, Word } from './types'

/** 출제된 문제 한 개. 객관식이면 `choices` 에 뜻 4개가 섞여 있다. */
export interface ExamQuestion {
  word: Word
  type: QuestionType
  choices: string[] | null
}

export const CHOICE_COUNT = 4
export const MULTIPLE_CHOICE_CAP_PERCENT = 70
export const MIXED_MULTIPLE_CHOICE_PERCENT = 20

/**
 * 객관식 보기를 만든다. 정답 뜻 1개와 같은 난이도의 다른 단어 뜻 3개 (D5, D6).
 * 같은 난이도 뜻이 부족하면 null.
 */
export function buildChoices(answer: Word, pool: readonly Word[], rng: Rng = Math.random): string[] | null {
  const otherMeanings = pool
    .filter((word) => word.id !== answer.id && word.level === answer.level && word.meaningKo !== answer.meaningKo)
    .map((word) => word.meaningKo)
  const distractors = shuffled([...new Set(otherMeanings)], rng).slice(0, CHOICE_COUNT - 1)
  if (distractors.length < CHOICE_COUNT - 1) return null
  return shuffled([...distractors, answer.meaningKo], rng)
}

/**
 * 출제 구성 (P4). 후보는 오늘 목록과 오늘 재노출 대상이다 (중복 없음).
 * - 객관식 단계 단어는 항상 객관식이다.
 * - 주관식 단계 단어는 20% 를 객관식으로 섞는다 (D8). 이때 객관식이 문제 수의 70% 에 닿았으면 섞지 않는다 (D7).
 * - 보기를 만들 수 없는 문제는 주관식으로 낸다.
 */
export function planExam(
  candidates: readonly Word[],
  examSize: number,
  isTypedStage: (word: Word) => boolean,
  pool: readonly Word[],
  rng: Rng = Math.random,
): ExamQuestion[] {
  if (examSize <= 0) throw new RangeError(`examSize 는 1 이상이어야 한다: ${examSize}`)
  const total = Math.min(examSize, candidates.length)
  const multipleChoiceCap = Math.floor((examSize * MULTIPLE_CHOICE_CAP_PERCENT) / 100)

  const questions: ExamQuestion[] = []
  let multipleChoiceCount = 0
  for (const word of shuffled(candidates, rng).slice(0, total)) {
    const typedStage = isTypedStage(word)
    const wantsMultipleChoice = !typedStage || nextInt(rng, 100) < MIXED_MULTIPLE_CHOICE_PERCENT
    const multipleChoiceAllowed = !typedStage || multipleChoiceCount < multipleChoiceCap
    const choices = wantsMultipleChoice && multipleChoiceAllowed ? buildChoices(word, pool, rng) : null
    if (choices !== null) {
      multipleChoiceCount++
      questions.push({ word, type: 'mc', choices })
    } else {
      questions.push({ word, type: 'typed', choices: null })
    }
  }
  return questions
}

/** 주관식 채점 (P5). 앞뒤 공백과 대소문자만 무시하고 원형 그대로 비교한다 (FR-033, D9). */
export function isTypedAnswerCorrect(userInput: string, answer: string): boolean {
  return userInput.trim().toLowerCase() === answer.trim().toLowerCase()
}

/** 객관식 채점. 고른 뜻이 정답 뜻과 같은지 본다. */
export function isMultipleChoiceCorrect(selectedMeaning: string, answer: Word): boolean {
  return selectedMeaning === answer.meaningKo
}
