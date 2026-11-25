// 2026-11-25 - Smart Voca - 카드 학습 흐름 규칙 (한 장씩 노출, 따라 쓰기 두 번으로 완료 판정)
// 파일 위치: web/src/domain/studyFlow.ts

import { isTypedAnswerCorrect } from './examRules'

/** 오늘 목록에서 아직 완료하지 않은 첫 카드의 위치. 모두 끝났으면 -1. */
export function firstUnstudiedIndex(wordIds: readonly number[], studiedIds: ReadonlySet<number>): number {
  return wordIds.findIndex((id) => !studiedIds.has(id))
}

/** 칸 하나의 상태. 비어 있음 → 입력 중 → 맞음/틀림. */
export type BlankState = 'empty' | 'typing' | 'correct' | 'wrong'

/**
 * 칸에 입력한 값을 판정한다. 대소문자와 앞뒤 공백은 무시한다 (시험 채점과 같은 규칙, FR-033).
 * 정답 길이에 닿기 전에는 틀렸다고 하지 않는다. 쓰는 도중에 빨간색이 뜨면 방해가 되기 때문이다.
 */
export function judgeBlank(input: string, answer: string): BlankState {
  const typed = input.trim()
  if (typed === '') return 'empty'
  if (isTypedAnswerCorrect(typed, answer)) return 'correct'
  return typed.length < answer.trim().length ? 'typing' : 'wrong'
}

/** 칸이 모두 맞아야 학습 완료다. */
export function isCardComplete(inputs: readonly string[], answer: string, requiredCount: number): boolean {
  return inputs.length >= requiredCount && inputs.slice(0, requiredCount).every((input) => judgeBlank(input, answer) === 'correct')
}
