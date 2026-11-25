// 2026-11-25 - Smart Voca - 카드 학습 규칙과 임시 동사 묶음 파일 테스트
// 파일 위치: web/src/domain/studyFlow.test.ts

import { readFileSync } from 'node:fs'
import { describe, expect, it } from 'vitest'
import { STARTER_WORDS_FILE_NAME, STUDY_TYPING_REPEAT } from '../appConfig'
import { parseWordCsv } from '../data/csv/wordCsvParser'
import { FUNCTION_WORDS_FILE_NAME, FunctionWords } from './functionWords'
import { firstUnstudiedIndex, isCardComplete, judgeBlank } from './studyFlow'

describe('studyFlow', () => {
  it('아직 완료하지 않은 첫 카드를 고르고, 모두 끝나면 -1 이다', () => {
    expect(firstUnstudiedIndex([1, 2, 3], new Set())).toBe(0)
    expect(firstUnstudiedIndex([1, 2, 3], new Set([1, 3]))).toBe(1)
    expect(firstUnstudiedIndex([1, 2, 3], new Set([1, 2, 3]))).toBe(-1)
    expect(firstUnstudiedIndex([], new Set())).toBe(-1)
  })

  it('칸 판정: 비어 있음, 쓰는 중, 맞음, 틀림', () => {
    expect(judgeBlank('', 'go')).toBe('empty')
    expect(judgeBlank('   ', 'go')).toBe('empty')
    expect(judgeBlank('g', 'go')).toBe('typing')
    expect(judgeBlank('go', 'go')).toBe('correct')
    expect(judgeBlank(' GO ', 'go')).toBe('correct')
    expect(judgeBlank('gx', 'go')).toBe('wrong')
    expect(judgeBlank('goes', 'go')).toBe('wrong')
  })

  it('정답 길이보다 짧으면 철자가 달라도 아직 틀림으로 보지 않는다', () => {
    expect(judgeBlank('rem', 'remember')).toBe('typing')
    expect(judgeBlank('rxm', 'remember')).toBe('typing')
  })

  it('따라 쓰기 칸이 모두 맞아야 카드가 완료된다', () => {
    expect(STUDY_TYPING_REPEAT).toBe(2)
    expect(isCardComplete(['go', ''], 'go', 2)).toBe(false)
    expect(isCardComplete(['go', 'gx'], 'go', 2)).toBe(false)
    expect(isCardComplete(['go', 'Go'], 'go', 2)).toBe(true)
    expect(isCardComplete(['go'], 'go', 2)).toBe(false)
  })
})

describe('임시 동사 묶음 (public/starter_verbs.csv)', () => {
  const parsed = parseWordCsv(readFileSync(`public/${STARTER_WORDS_FILE_NAME}`, 'utf-8'))
  const functionWords = FunctionWords.parse(readFileSync(`public/${FUNCTION_WORDS_FILE_NAME}`, 'utf-8'))

  it('120개가 모두 오류 없이 읽힌다', () => {
    expect(parsed.skipped).toEqual([])
    expect(parsed.words).toHaveLength(120)
  })

  it('100% 동사다', () => {
    expect(parsed.words.every((word) => word.pos === 'verb')).toBe(true)
  })

  it('기능어가 섞여 있지 않다 (섞이면 목록에서 빠져 개수가 줄어든다)', () => {
    expect(parsed.words.filter((word) => functionWords.words.has(word.word.toLowerCase())).map((w) => w.word)).toEqual([])
  })

  it('난이도마다 40개씩이고 같은 난이도 안에서 뜻이 겹치지 않는다 (객관식 보기용)', () => {
    for (const level of ['basic', 'intermediate', 'advanced'] as const) {
      const inLevel = parsed.words.filter((word) => word.level === level)
      expect(inLevel).toHaveLength(40)
      expect(new Set(inLevel.map((word) => word.meaningKo)).size).toBe(40)
    }
  })
})
