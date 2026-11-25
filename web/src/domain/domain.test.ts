// 2026-11-25 - Smart Voca - 도메인 규칙 테스트. Kotlin src/test/.../domain/*Test.kt 의 케이스를 그대로 이관
// 파일 위치: web/src/domain/domain.test.ts

import { readFileSync } from 'node:fs'
import { describe, expect, it } from 'vitest'
import { composeDailyList } from './dailyWordSelector'
import { shouldNotifyDayChange } from './dayChangeRules'
import { buildChoices, isMultipleChoiceCorrect, isTypedAnswerCorrect, planExam } from './examRules'
import { FUNCTION_WORDS_FILE_NAME, FunctionWords } from './functionWords'
import { addDays, daysBetween, todayLocal, toEpochDay } from './localDate'
import { seededRng } from './random'
import { nextReviewDate, nextStep } from './reviewScheduler'
import { afterAnswer, DEFAULT_STAGE, type StageState } from './stageRules'
import { range, testWord } from './testWords'

describe('localDate', () => {
  it('epochDay 는 Kotlin toEpochDay 와 같다', () => {
    expect(toEpochDay('1970-01-01')).toBe(0)
    expect(toEpochDay('2026-11-25')).toBe(20_782)
  })

  it('월말과 윤년을 넘겨 날짜를 더한다', () => {
    expect(addDays('2026-11-30', 1)).toBe('2026-12-01')
    expect(addDays('2028-02-28', 1)).toBe('2028-02-29')
    expect(addDays('2026-01-01', -1)).toBe('2025-12-31')
  })

  it('현지 날짜는 시각과 관계없이 그날 날짜다', () => {
    expect(todayLocal(new Date(2026, 10, 25, 23, 59))).toBe('2026-11-25')
    expect(todayLocal(new Date(2026, 10, 25, 0, 0))).toBe('2026-11-25')
  })
})

describe('DailyWordSelector', () => {
  it('재노출은 설정 개수의 30 퍼센트까지만 먼저 넣고 나머지는 신규로 채운다', () => {
    const picked = composeDailyList(10, range(1, 5).map((n) => testWord(n)), range(101, 120).map((n) => testWord(n)))

    expect(picked.filter((p) => p.source === 'review')).toHaveLength(3)
    expect(picked.filter((p) => p.source === 'new')).toHaveLength(7)
    expect(picked.slice(0, 3).map((p) => p.word.freqRank)).toEqual([1, 2, 3])
  })

  it('신규 단어가 부족하면 재노출 후보를 상한을 넘겨 채운다', () => {
    const picked = composeDailyList(10, range(1, 6).map((n) => testWord(n)), [testWord(101), testWord(102)])

    expect(picked.filter((p) => p.source === 'review')).toHaveLength(6)
    expect(picked.filter((p) => p.source === 'new')).toHaveLength(2)
    expect(picked).toHaveLength(8)
  })

  it('재노출이 먼저 나오고 신규 단어가 뒤에 나온다', () => {
    const picked = composeDailyList(10, [testWord(1)], [testWord(101), testWord(102)])

    expect(picked.map((p) => p.source)).toEqual(['review', 'new', 'new'])
  })

  it('후보가 모두 부족하면 있는 만큼만 돌려준다', () => {
    expect(composeDailyList(10, [], [testWord(1)])).toHaveLength(1)
  })

  it('개수가 1 미만이면 예외를 던진다', () => {
    expect(() => composeDailyList(0, [], [])).toThrow(RangeError)
  })
})

describe('DayChangeRules', () => {
  const day1 = '2026-11-18'

  it('처음 실행이면 알리지 않는다', () => expect(shouldNotifyDayChange(null, day1)).toBe(false))
  it('같은 날이면 알리지 않는다', () => expect(shouldNotifyDayChange(day1, day1)).toBe(false))
  it('마지막으로 본 날짜와 다르면 알린다', () => expect(shouldNotifyDayChange(day1, addDays(day1, 1))).toBe(true))
})

describe('ExamRules', () => {
  const pool = range(1, 30).map((n) => testWord(n))

  it('객관식 보기는 정답 뜻을 포함한 4개이고 같은 난이도에서 뽑는다', () => {
    const choices = buildChoices(testWord(1), pool, seededRng(3))!

    expect(choices).toHaveLength(4)
    expect(new Set(choices).size).toBe(4)
    expect(choices).toContain('뜻1')
  })

  it('같은 난이도 뜻이 부족하면 보기를 만들지 않는다', () => {
    expect(buildChoices(testWord(1, 'advanced'), pool, seededRng(3))).toBeNull()
  })

  it('객관식 정답은 고른 뜻이 정답 뜻과 같을 때만 맞다', () => {
    expect(isMultipleChoiceCorrect('뜻1', testWord(1))).toBe(true)
    expect(isMultipleChoiceCorrect('뜻2', testWord(1))).toBe(false)
  })

  it('객관식 단계 단어는 객관식으로만 나오고 설정 문제 수를 넘지 않는다', () => {
    const planned = planExam(range(1, 5).map((n) => testWord(n)), 10, () => false, pool, seededRng(5))

    expect(planned.every((q) => q.type === 'mc')).toBe(true)
    expect(planned).toHaveLength(5)
  })

  it('주관식 단계 단어를 섞은 객관식은 전체 객관식 비율 70퍼센트를 넘지 않는다', () => {
    // 여러 시드로 확인한다. 어떤 난수가 나와도 상한 7개를 넘으면 안 된다
    for (const seed of range(1, 50)) {
      const planned = planExam(range(1, 20).map((n) => testWord(n)), 10, () => true, pool, seededRng(seed))

      expect(planned.filter((q) => q.type === 'mc').length).toBeLessThanOrEqual(7)
      expect(planned).toHaveLength(10)
    }
  })

  it('주관식 단계 단어는 주로 주관식으로 나온다', () => {
    const planned = planExam(range(1, 10).map((n) => testWord(n)), 10, () => true, pool, seededRng(11))

    expect(planned.filter((q) => q.type === 'typed').length).toBeGreaterThanOrEqual(5)
  })

  it('후보가 없으면 빈 목록이다', () => {
    expect(planExam([], 10, () => false, pool)).toEqual([])
  })

  it('문제 수가 1 미만이면 예외를 던진다', () => {
    expect(() => planExam([testWord(1)], 0, () => false, pool)).toThrow(RangeError)
  })

  it('주관식은 앞뒤 공백과 대소문자만 무시하고 원형만 맞다', () => {
    expect(isTypedAnswerCorrect('  Go ', 'go')).toBe(true)
    expect(isTypedAnswerCorrect('GO', 'go')).toBe(true)
    expect(isTypedAnswerCorrect('went', 'go')).toBe(false)
    expect(isTypedAnswerCorrect('', 'go')).toBe(false)
    expect(isTypedAnswerCorrect('g o', 'go')).toBe(false)
  })
})

describe('FunctionWords', () => {
  const csv = ['word,category,tip_ko', 'The,관사,이미 알고 있는 대상 앞에 쓴다 (the sun)', 'to,전치사,방향과 도착점을 나타낸다 (to go)'].join('\n')

  it('헤더를 건너뛰고 단어를 소문자로 모은다', () => {
    const functionWords = FunctionWords.parse(csv)

    expect(functionWords.tips.map((tip) => tip.word)).toEqual(['The', 'to'])
    expect([...functionWords.words]).toEqual(['the', 'to'])
  })

  it('같은 날짜에는 같은 TIP 을 고르고 날짜가 바뀌면 다음 TIP 을 고른다', () => {
    const functionWords = FunctionWords.parse(csv)
    const day = '2026-11-11'

    expect(functionWords.dailyTip(day)).toEqual(functionWords.dailyTip(day))
    expect(functionWords.dailyTip(day)).not.toEqual(functionWords.dailyTip(addDays(day, 1)))
  })

  it('기능어가 없으면 TIP 은 null 이다', () => {
    expect(new FunctionWords([]).dailyTip('2026-11-11')).toBeNull()
  })

  it('형식이 틀린 행이 있으면 예외를 던진다', () => {
    expect(() => FunctionWords.parse('word,category,tip_ko\nthe,관사')).toThrow()
  })

  it('기능어 파일이 public 에 있고 읽을 수 있다', () => {
    // 테스트는 web/ 에서 실행한다. 파일이 없으면 앱이 TIP 을 띄우지 못한다
    const text = readFileSync(`public/${FUNCTION_WORDS_FILE_NAME}`, 'utf-8')

    expect(FunctionWords.parse(text).tips.length).toBeGreaterThan(100)
  })
})

describe('ReviewScheduler', () => {
  const today = '2026-11-18'

  it('처음 학습하면 단계 0 이고 다음 날 재노출한다', () => {
    expect(nextStep(null)).toBe(0)
    expect(nextReviewDate(today, 0)).toBe(addDays(today, 1))
  })

  it('간격은 1, 3, 7, 14, 30일 순서로 늘어난다', () => {
    let step: number | null = null
    const gaps: number[] = []
    for (let index = 0; index < 5; index++) {
      step = nextStep(step)
      gaps.push(daysBetween(today, nextReviewDate(today, step)!))
    }

    expect(gaps).toEqual([1, 3, 7, 14, 30])
  })

  it('30일 간격 뒤에는 재노출 날짜가 없다', () => {
    expect(nextReviewDate(today, nextStep(4))).toBeNull()
  })
})

describe('StageRules', () => {
  const day1 = '2026-11-18'
  const day2 = addDays(day1, 1)
  const day3 = addDays(day1, 2)
  const now = new Date('2026-11-18T01:00:00Z')
  const mc = (state: StageState, correct: boolean, day: string) => afterAnswer(state, 'mc', correct, day, now)

  it('객관식을 연속 3일 맞히면 주관식으로 승격하고 승격 시각을 남긴다', () => {
    let state = mc(mc(DEFAULT_STAGE, true, day1), true, day2)
    expect(state.type).toBe('mc')

    state = mc(state, true, day3)

    expect(state.type).toBe('typed')
    expect(state.promotedAt).not.toBeNull()
  })

  it('같은 날 여러 번 맞혀도 연속 일수는 한 번만 센다', () => {
    const state = mc(mc(mc(DEFAULT_STAGE, true, day1), true, day1), true, day1)

    expect(state.type).toBe('mc')
    expect(state.streakDays).toBe(1)
  })

  it('객관식에서 틀리면 연속 일수가 0으로 돌아간다', () => {
    const state = mc(mc(mc(mc(DEFAULT_STAGE, true, day1), true, day2), false, day2), true, day3)

    expect(state.type).toBe('mc')
    expect(state.streakDays).toBe(1)
  })

  it('중간에 날짜가 끊기면 연속 일수는 다시 1부터 센다', () => {
    const state = mc(mc(mc(DEFAULT_STAGE, true, day1), true, day2), true, addDays(day2, 2))

    expect(state.type).toBe('mc')
    expect(state.streakDays).toBe(1)
  })

  it('주관식에서 틀리면 객관식으로 강등하고 연속 일수를 0으로 돌린다', () => {
    const typed: StageState = { type: 'typed', streakDays: 0, lastCountedDate: day3, promotedAt: now.toISOString() }

    const after = afterAnswer(typed, 'typed', false, addDays(day3, 1), now)

    expect(after.type).toBe('mc')
    expect(after.streakDays).toBe(0)
    expect(after.lastCountedDate).toBeNull()
  })

  it('주관식을 맞히면 단계는 그대로다', () => {
    const typed: StageState = { ...DEFAULT_STAGE, type: 'typed', promotedAt: now.toISOString() }

    expect(afterAnswer(typed, 'typed', true, day2, now)).toEqual(typed)
  })
})
