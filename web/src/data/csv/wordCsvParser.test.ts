// 2026-11-25 - Smart Voca - 단어 CSV 파서의 헤더 검증, 행 검증, 중복 처리 테스트 (FR-001~003, 005). Kotlin WordCsvParserTest 이관
// 파일 위치: web/src/data/csv/wordCsvParser.test.ts

import { describe, expect, it } from 'vitest'
import { UnclosedQuoteError } from './csvRecordReader'
import { MissingColumnsError, parseWordCsv } from './wordCsvParser'

describe('WordCsvParser', () => {
  it('필수 열이 없으면 누락된 열 이름과 함께 중단한다', () => {
    try {
      parseWordCsv('word,meaning_ko,level\nbe,있다,basic\n')
      expect.unreachable()
    } catch (error) {
      expect(error).toBeInstanceOf(MissingColumnsError)
      expect((error as MissingColumnsError).missingColumns).toEqual(['freq_rank'])
    }
  })

  it('선택 열이 없어도 적재되고 선택 값은 null 이다', () => {
    const result = parseWordCsv('word,meaning_ko,freq_rank,level\nbe,있다,1,basic\n')

    expect(result.words).toHaveLength(1)
    expect(result.words[0]).toMatchObject({ word: 'be', level: 'basic', pos: null, rating: null })
    expect(result.skipped).toEqual([])
  })

  it('freq_rank 가 빈 행은 건너뛰고 줄 번호와 사유를 남긴다', () => {
    const result = parseWordCsv('word,meaning_ko,freq_rank,level\nbe,있다,1,basic\nhave,가지다,,basic\n')

    expect(result.words.map((w) => w.word)).toEqual(['be'])
    expect(result.skipped).toEqual([{ lineNumber: 3, reason: 'freq_rank 누락' }])
  })

  it('따옴표 안의 쉼표는 한 필드로 읽는다', () => {
    const result = parseWordCsv('word,meaning_ko,freq_rank,level\nlevel,"정도[수준], 수준[단계]",861,basic\n')

    expect(result.words[0].meaningKo).toBe('정도[수준], 수준[단계]')
  })

  it('BOM 이 붙은 파일도 첫 열 이름을 정상으로 인식한다', () => {
    const result = parseWordCsv('﻿word,meaning_ko,freq_rank,level\nbe,있다,1,basic\n')

    expect(result.words).toHaveLength(1)
    expect(result.skipped).toEqual([])
  })

  it('같은 빈도 순위가 두 번 나오면 뒤의 행을 건너뛴다', () => {
    const result = parseWordCsv('word,meaning_ko,freq_rank,level\nbe,있다,1,basic\nam,있다,1,basic\n')

    expect(result.words.map((w) => w.word)).toEqual(['be'])
    expect(result.skipped).toEqual([{ lineNumber: 3, reason: 'freq_rank 1 중복 (2행과 같음)' }])
  })

  it('허용되지 않은 level 값과 영문자가 아닌 word 는 건너뛴다', () => {
    const csv = 'word,meaning_ko,freq_rank,level\ngo,가다,2,easy\n한글,단어,3,basic\ntake off,이륙하다,4,basic\n'

    const result = parseWordCsv(csv)

    expect(result.words.map((w) => w.word)).toEqual(['take off'])
    expect(result.skipped.map((s) => s.lineNumber)).toEqual([2, 3])
  })

  it('샘플 형식의 행을 모든 선택 열과 함께 읽는다', () => {
    const csv = 'word,pos,meaning_ko,example_en,example_ko,freq_rank,level,rating\nbe,,"있다, 있다, 참석",,,1,basic,★★★★\n'

    const item = parseWordCsv(csv).words[0]

    expect(item.pos).toBeNull()
    expect(item.meaningKo).toBe('있다, 있다, 참석')
    expect(item.rating).toBe('★★★★')
  })

  // 아래는 웹 이관 때 추가한 경계 사례
  it('Windows 줄바꿈(CRLF)과 따옴표 안의 줄바꿈, 이스케이프된 따옴표를 읽는다', () => {
    const csv = 'word,meaning_ko,example_en,freq_rank,level\r\ngo,가다,"He said ""go""\r\nnow",1,basic\r\n'

    const result = parseWordCsv(csv)

    expect(result.words[0].exampleEn).toBe('He said "go"\nnow')
    expect(result.skipped).toEqual([])
  })

  it('freq_rank 는 1 이상의 정수만 받는다', () => {
    const csv = 'word,meaning_ko,freq_rank,level\na,뜻,0,basic\nb,뜻,1.5,basic\nc,뜻,x,basic\nd,뜻,7,basic\n'

    const result = parseWordCsv(csv)

    expect(result.words.map((w) => w.freqRank)).toEqual([7])
    expect(result.skipped).toHaveLength(3)
  })

  it('따옴표가 닫히지 않으면 줄 번호와 함께 중단한다', () => {
    expect(() => parseWordCsv('word,meaning_ko,freq_rank,level\ngo,"가다,1,basic\n')).toThrow(UnclosedQuoteError)
  })
})
