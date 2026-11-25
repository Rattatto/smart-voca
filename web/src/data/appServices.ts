// 2026-11-25 - Smart Voca - 앱 전체가 함께 쓰는 DB 연결과 저장소 묶음. Kotlin MainActivity.onCreate 의 생성 부분 이관
// 파일 위치: web/src/data/appServices.ts

import { FUNCTION_WORDS_FILE_NAME, FunctionWords } from '../domain/functionWords'
import { DailyListRepository } from './dailyListRepository'
import { openDatabase } from './db'
import { ExamRepository } from './examRepository'
import { SettingsStore } from './settingsStore'
import { StudyRepository } from './studyRepository'
import { WordRepository } from './wordRepository'

export interface AppServices {
  words: WordRepository
  daily: DailyListRepository
  study: StudyRepository
  exam: ExamRepository
  settings: SettingsStore
  functionWords: FunctionWords
}

let servicesPromise: Promise<AppServices> | null = null

/**
 * DB 연결은 앱 전체에서 하나만 쓴다. 여러 번 불려도 처음 만든 것을 돌려준다.
 * React 개발 모드(StrictMode)는 effect 를 두 번 실행하므로, 이렇게 막지 않으면 연결이 두 개 생긴다.
 */
export function getAppServices(): Promise<AppServices> {
  servicesPromise ??= createAppServices().catch((error: unknown) => {
    servicesPromise = null // 실패하면 다음에 다시 시도할 수 있게 비운다
    throw error
  })
  return servicesPromise
}

async function createAppServices(): Promise<AppServices> {
  const db = openDatabase()
  await db.open() // 시크릿 모드 등에서 IndexedDB 를 못 쓰면 여기서 실패한다
  const functionWords = await loadFunctionWords()
  // 브라우저가 저장 공간이 부족할 때 데이터를 지우지 않도록 요청한다. 거절돼도 앱은 동작한다 (ADR-015)
  void navigator.storage?.persist?.().catch(() => false)
  return {
    words: new WordRepository(db),
    daily: new DailyListRepository(db, functionWords.words),
    study: new StudyRepository(db),
    exam: new ExamRepository(db),
    settings: new SettingsStore(db),
    functionWords,
  }
}

/** 기능어 파일은 앱과 함께 배포되는 정적 파일이라 시작할 때 한 번 읽는다. */
async function loadFunctionWords(): Promise<FunctionWords> {
  const response = await fetch(`${import.meta.env.BASE_URL}${FUNCTION_WORDS_FILE_NAME}`)
  if (!response.ok) throw new Error(`기능어 파일을 읽지 못했습니다 (${response.status})`)
  return FunctionWords.parse(await response.text())
}
