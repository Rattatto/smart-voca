// 2026-11-25 - Smart Voca - 저장소 테스트 (가져오기, 일일 목록, 학습 완료·재노출, 시험 이력·단계, 설정). Kotlin *RepositoryTest, SettingsStoreTest 이관
// 파일 위치: web/src/data/repositories.test.ts

import 'fake-indexeddb/auto'
import { afterEach, beforeEach, describe, expect, it } from 'vitest'
import { addDays } from '../domain/localDate'
import { DEFAULT_STAGE } from '../domain/stageRules'
import type { Level } from '../domain/types'
import { MissingColumnsError } from './csv/wordCsvParser'
import { DailyListRepository } from './dailyListRepository'
import { openDatabase, type SmartVocaDb } from './db'
import { ExamRepository } from './examRepository'
import { DEFAULT_SETTINGS, SettingsStore } from './settingsStore'
import { StudyRepository } from './studyRepository'
import { WordRepository } from './wordRepository'

const HEADER = 'word,meaning_ko,freq_rank,level'
const fixedNow = new Date('2026-11-11T00:00:00Z')
const day1 = '2026-11-11'
const day2 = '2026-11-12'
const day3 = '2026-11-13'

let db: SmartVocaDb
let words: WordRepository
let daily: DailyListRepository
let study: StudyRepository
let exam: ExamRepository
let testCount = 0

beforeEach(() => {
  // 테스트마다 새 이름의 DB 를 써서 깨끗한 상태로 시작한다 (Kotlin 의 메모리 DB 와 같은 역할)
  db = openDatabase(`test-${++testCount}`)
  words = new WordRepository(db)
  daily = new DailyListRepository(db)
  study = new StudyRepository(db)
  exam = new ExamRepository(db)
})

afterEach(async () => {
  await db.delete()
})

const importRows = (...rows: string[]) => words.importCsv([HEADER, ...rows].join('\n'))
const list = (date: string, count = 10, level: Level | null = null) => daily.getOrCreate(date, count, level, fixedNow)
const greek = 'alpha beta gamma delta epsilon zeta eta theta iota kappa lambda mu nu xi omicron pi rho sigma tau upsilon'.split(' ')

describe('WordRepository', () => {
  it('새 단어는 모두 저장되고 개수가 맞다', async () => {
    const summary = await importRows('be,있다,1,basic', 'go,가다,2,basic')

    expect(summary).toMatchObject({ inserted: 2, updated: 0 })
    expect(await words.countWords()).toBe(2)
  })

  it('같은 빈도 순위를 다시 가져오면 내용만 갱신하고 id 는 유지한다', async () => {
    await importRows('be,있다,1,basic')
    const idBefore = (await words.findAllOrdered())[0].id

    const summary = await importRows('be,존재하다,1,basic')

    expect(summary).toMatchObject({ inserted: 0, updated: 1 })
    const all = await words.findAllOrdered()
    expect(all).toHaveLength(1)
    expect(all[0].id).toBe(idBefore)
    expect(all[0].meaningKo).toBe('존재하다')
  })

  it('잘못된 행은 저장하지 않고 건너뛴 행을 돌려준다', async () => {
    const summary = await importRows('be,있다,1,basic', 'have,가지다,,basic')

    expect(summary.inserted).toBe(1)
    expect(summary.skipped).toEqual([{ lineNumber: 3, reason: 'freq_rank 누락' }])
    expect(await words.countWords()).toBe(1)
  })

  it('필수 열이 없으면 예외를 던지고 아무것도 저장하지 않는다', async () => {
    await expect(words.importCsv('word,meaning_ko,level\nbe,있다,basic\n')).rejects.toBeInstanceOf(MissingColumnsError)
    expect(await words.countWords()).toBe(0)
  })
})

describe('DailyListRepository', () => {
  it('같은 날짜에는 새 단어가 들어와도 목록이 바뀌지 않는다', async () => {
    await importRows('alpha,뜻1,1,basic', 'beta,뜻2,2,basic', 'gamma,뜻3,3,basic')
    const first = await list(day1)
    await importRows('delta,뜻4,4,basic', 'epsilon,뜻5,5,basic')
    const second = await list(day1)

    expect(first.map((p) => p.word.word)).toEqual(['alpha', 'beta', 'gamma'])
    expect(second.map((p) => p.word.id)).toEqual(first.map((p) => p.word.id))
  })

  it('날짜가 바뀌면 새 목록을 만든다', async () => {
    await importRows('alpha,뜻1,1,basic', 'beta,뜻2,2,basic')
    await list(day1)
    await importRows('gamma,뜻3,3,basic')

    expect((await list(day2)).map((p) => p.word.word)).toEqual(['alpha', 'beta', 'gamma'])
  })

  it('설정 개수와 난이도 필터를 적용한다', async () => {
    await importRows('alpha,뜻1,1,basic', 'beta,뜻2,2,intermediate', 'gamma,뜻3,3,intermediate', 'delta,뜻4,4,advanced')

    expect((await list(day1, 1, 'intermediate')).map((p) => p.word.word)).toEqual(['beta'])
  })

  it('노출 시각과 설정 스냅샷을 함께 저장하고 순서를 유지한다', async () => {
    await importRows('alpha,뜻1,1,basic', 'beta,뜻2,2,basic', 'gamma,뜻3,3,basic')
    await list(day1, 2)

    const rows = await daily.findRows(day1)

    expect(rows.map((r) => r.position)).toEqual([1, 2])
    expect(rows.every((r) => r.shownAt === fixedNow.toISOString())).toBe(true)
    expect(rows.every((r) => r.source === 'new' && r.dailyCount === 2 && r.levelFilter === 'all')).toBe(true)
  })

  it('학습 완료한 단어는 다음 날 재노출되고 신규 목록에서는 빠진다', async () => {
    await importRows('alpha,뜻1,1,basic', 'beta,뜻2,2,basic')
    const first = await list(day1)
    await study.markStudied(first[0].word.id, day1, fixedNow)

    const nextDay = await list(day2)

    expect(nextDay.map((p) => p.word.word)).toEqual(['alpha', 'beta'])
    expect(nextDay.map((p) => p.source)).toEqual(['review', 'new'])
  })

  it('같은 날 두 번 완료해도 재노출 일정은 한 번만 올라간다', async () => {
    await importRows('alpha,뜻1,1,basic')
    const first = await list(day1)
    await study.markStudied(first[0].word.id, day1, fixedNow)
    await study.markStudied(first[0].word.id, day1, fixedNow)

    // 단계 0 이므로 다음 날 재노출된다. 두 번 올랐다면 3일 뒤라서 다음 날에는 안 나온다
    const nextDay = await list(day2, 5)

    expect(nextDay.map((p) => p.word.word)).toEqual(['alpha'])
    expect(nextDay[0].source).toBe('review')
    expect(await study.studiedIdsOn(day1)).toEqual(new Set([first[0].word.id]))
  })

  it('재노출은 설정 개수의 30 퍼센트까지만 들어간다', async () => {
    await importRows(...greek.map((word, index) => `${word},뜻${word},${index + 1},basic`))
    const first = await list(day1, 10)
    for (const item of first.slice(0, 5)) await study.markStudied(item.word.id, day1, fixedNow)

    // 재노출 대상 5개, 상한은 floor(10 × 0.3) = 3. 나머지 7칸은 신규 단어
    const nextDay = await list(day2, 10)

    expect(nextDay.filter((p) => p.source === 'review')).toHaveLength(3)
    expect(nextDay.filter((p) => p.source === 'new')).toHaveLength(7)
  })

  it('단어가 없으면 빈 목록을 돌려주고 목록을 저장하지 않는다', async () => {
    expect(await list(day1)).toEqual([])
    expect(await daily.findRows(day1)).toEqual([])
  })

  it('난이도 필터는 재노출 단어에도 적용된다', async () => {
    await importRows('alpha,뜻1,1,basic', 'beta,뜻2,2,advanced')
    const first = await list(day1, 1)
    await study.markStudied(first[0].word.id, day1, fixedNow)

    const advancedOnly = await list(day2, 5, 'advanced')

    expect(advancedOnly.map((p) => p.word.word)).toEqual(['beta'])
    expect(advancedOnly.map((p) => p.source)).toEqual(['new'])
  })

  it('기능어는 일일 목록에 들어가지 않는다', async () => {
    await importRows('the,그,1,basic', 'go,가다,2,basic')
    const withFunctionWords = new DailyListRepository(db, new Set(['the']))

    const picked = await withFunctionWords.getOrCreate(day1, 10, null, fixedNow)

    expect(picked.map((p) => p.word.word)).toEqual(['go'])
  })

  it('단어 2000개 기준 가져오기와 목록 생성이 각각 1초 안에 끝난다 (NFR-003)', async () => {
    // 파서는 영문자 단어만 받으므로 숫자를 a~j 문자로 바꿔 단어를 만든다
    const lettersFor = (n: number) => 'w' + [...String(n)].map((d) => String.fromCharCode(97 + Number(d))).join('')
    const rows = Array.from({ length: 2000 }, (_, i) => `${lettersFor(i + 1)},뜻${i + 1},${i + 1},basic`)

    let startedAt = performance.now()
    await importRows(...rows)
    const importMillis = performance.now() - startedAt
    startedAt = performance.now()
    const selected = await list(day1)
    const listMillis = performance.now() - startedAt

    expect(await words.countWords()).toBe(2000)
    expect(selected).toHaveLength(10)
    expect(importMillis, `가져오기 ${importMillis.toFixed(0)}ms`).toBeLessThan(1000)
    expect(listMillis, `목록 생성 ${listMillis.toFixed(0)}ms`).toBeLessThan(1000)
  })
})

describe('ExamRepository', () => {
  const importGo = async () => {
    await words.importCsv(`${HEADER}\ngo,가다,1,basic`)
    return (await daily.getOrCreate(day1, 10, null, fixedNow))[0].word.id
  }
  const answer = (wordId: number, date: string, userInput: string, isCorrect: boolean, type: 'mc' | 'typed') =>
    exam.recordAttempt({ wordId, date, shownMeaning: '가다', userInput, isCorrect, type, now: fixedNow })

  it('시험 답은 날짜, 문제 형식과 함께 저장된다', async () => {
    const wordId = await importGo()
    await answer(wordId, day1, 'went', false, 'typed')
    await answer(wordId, day1, '가다', true, 'mc')

    const attempts = await exam.attemptsOn(day1)

    expect(attempts.map((a) => a.userInput)).toEqual(['went', '가다'])
    expect(attempts.map((a) => a.isCorrect)).toEqual([false, true])
    expect(attempts.map((a) => a.questionType)).toEqual(['typed', 'mc'])
  })

  it('객관식 연속 3일 정답이면 단계가 주관식으로 바뀐다', async () => {
    const wordId = await importGo()
    await answer(wordId, day1, '가다', true, 'mc')
    await answer(wordId, day2, '가다', true, 'mc')
    const after = await answer(wordId, day3, '가다', true, 'mc')

    expect(after.type).toBe('typed')
    expect((await exam.stageOf(wordId)).type).toBe('typed')
  })

  it('주관식에서 틀리면 객관식으로 강등된다', async () => {
    const wordId = await importGo()
    for (const offset of [0, 1, 2]) await answer(wordId, addDays(day1, offset), '가다', true, 'mc')

    const after = await answer(wordId, addDays(day3, 1), 'went', false, 'typed')

    expect(after.type).toBe('mc')
  })

  it('기록이 없는 단어의 단계는 객관식이다', async () => {
    expect(await exam.stageOf(999)).toEqual(DEFAULT_STAGE)
  })

  it('시험 결과는 재노출 일정을 바꾸지 않는다', async () => {
    const wordId = await importGo()
    await study.markStudied(wordId, day1, fixedNow)
    await answer(wordId, day1, 'xx', false, 'mc')

    const due = (await list(day2)).filter((p) => p.source === 'review')

    expect(due.map((p) => p.word.word)).toEqual(['go'])
  })

  it('시험 문제는 오늘 목록 단어로 만들고 객관식 보기는 정답을 포함한다', async () => {
    await importRows('go,가다,1,basic', 'come,오다,2,basic', 'make,만들다,3,basic', 'take,가지다,4,basic', 'see,보다,5,basic')
    const today = (await list(day1)).map((p) => p.word)

    const questions = await exam.buildQuestions(day1, 5, today)

    expect(questions).toHaveLength(5)
    for (const question of questions.filter((q) => q.type === 'mc')) {
      expect(question.choices).toContain(question.word.meaningKo)
    }
  })
})

describe('SettingsStore', () => {
  let store: SettingsStore
  beforeEach(() => {
    store = new SettingsStore(db)
  })

  it('저장한 값을 다시 읽는다', async () => {
    await store.saveDailyCount(20)
    await store.saveLevelFilter('advanced')
    await store.saveVerbRatio(80)

    expect(await store.load()).toEqual({ dailyCount: 20, levelFilter: 'advanced', verbRatio: 80, examSize: 10 })
  })

  it('저장한 적이 없으면 기본값을 돌려준다', async () => {
    expect(await store.load()).toEqual(DEFAULT_SETTINGS)
  })

  it('난이도를 전체로 되돌리면 null 로 읽힌다', async () => {
    await store.saveLevelFilter('basic')
    await store.saveLevelFilter(null)

    expect((await store.load()).levelFilter).toBeNull()
  })

  it('일일 개수는 5에서 30 사이만 받는다', async () => {
    await expect(store.saveDailyCount(4)).rejects.toThrow(RangeError)
    await expect(store.saveDailyCount(31)).rejects.toThrow(RangeError)
    await expect(store.saveDailyCount(5.5)).rejects.toThrow(RangeError)
    await store.saveDailyCount(5)
    await store.saveDailyCount(30)

    expect((await store.load()).dailyCount).toBe(30)
  })

  it('동사 비중은 30에서 90 사이의 10 단위만 받는다', async () => {
    await expect(store.saveVerbRatio(35)).rejects.toThrow(RangeError)
    await expect(store.saveVerbRatio(100)).rejects.toThrow(RangeError)
    await store.saveVerbRatio(90)

    expect((await store.load()).verbRatio).toBe(90)
  })

  it('시험 문제 수는 5에서 20 사이만 받고 기본값은 10이다', async () => {
    expect((await store.load()).examSize).toBe(10)
    await expect(store.saveExamSize(4)).rejects.toThrow(RangeError)
    await expect(store.saveExamSize(21)).rejects.toThrow(RangeError)
    await store.saveExamSize(20)

    expect((await store.load()).examSize).toBe(20)
  })

  it('저장된 값이 범위를 벗어나거나 깨졌으면 기본값으로 읽는다', async () => {
    // 범위 검사를 거치지 않은 값이 들어 있는 경우를 가정한다
    await db.settings.bulkPut([
      { key: 'daily_count', value: 99 },
      { key: 'level_filter', value: 'expert' },
      { key: 'last_seen_date', value: 'yesterday' },
    ])

    const loaded = await store.load()

    expect(loaded.dailyCount).toBe(DEFAULT_SETTINGS.dailyCount)
    expect(loaded.levelFilter).toBeNull()
    expect(await store.lastSeenDate()).toBeNull()
  })

  it('마지막으로 본 날짜를 저장하고 다시 읽는다 (처음에는 null)', async () => {
    expect(await store.lastSeenDate()).toBeNull()
    await store.saveLastSeenDate('2026-11-18')

    expect(await store.lastSeenDate()).toBe('2026-11-18')
  })
})
