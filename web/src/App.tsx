// 2026-11-25 - Smart Voca - 앱 진입 화면: 화면 전환, 날짜 변경 알림, 저장소 준비. Kotlin MainActivity.StartScreen 이관
// 파일 위치: web/src/App.tsx

import { useEffect, useState } from 'react'
import { SHOW_SETTINGS } from './appConfig'
import { type AppServices, getAppServices } from './data/appServices'
import { shouldNotifyDayChange } from './domain/dayChangeRules'
import { type LocalDate, todayLocal } from './domain/localDate'
import { ExamScreen } from './ui/ExamScreen'
import { SettingsScreen } from './ui/SettingsScreen'
import { SharePanel } from './ui/SharePanel'
import './ui/layout.css'
import { TodayWordScreen } from './ui/TodayWordScreen'

type Screen = 'today' | 'exam' | 'settings'

export default function App() {
  const [services, setServices] = useState<AppServices | null>(null)
  const [startError, setStartError] = useState<string | null>(null)
  const [screen, setScreen] = useState<Screen>('today')
  // 가져오기가 성공할 때마다 올라가서 오늘 목록을 다시 읽게 한다
  const [importCount, setImportCount] = useState(0)
  // 화면이 기준으로 삼는 날짜. 앱을 열거나 탭으로 돌아올 때 다시 본다
  const [today, setToday] = useState<LocalDate>(() => todayLocal())
  // 날짜가 바뀌었을 때 알림에 보여 줄 이전 날짜 (null 이면 알림 없음)
  const [changedFrom, setChangedFrom] = useState<LocalDate | null>(null)

  useEffect(() => {
    getAppServices().then(setServices, (error: unknown) => {
      setStartError(error instanceof Error ? error.message : String(error))
    })
  }, [])

  // 안드로이드의 ON_RESUME 에 해당한다. 탭이 다시 보이면 날짜가 바뀌었는지 확인한다
  useEffect(() => {
    if (!services) return
    const checkDay = async () => {
      const now = todayLocal()
      const lastSeen = await services.settings.lastSeenDate()
      await services.settings.saveLastSeenDate(now)
      setToday(now)
      if (shouldNotifyDayChange(lastSeen, now)) setChangedFrom(lastSeen)
    }
    const onVisible = () => {
      if (document.visibilityState === 'visible') void checkDay()
    }
    void checkDay()
    document.addEventListener('visibilitychange', onVisible)
    return () => document.removeEventListener('visibilitychange', onVisible)
  }, [services])

  if (startError) {
    return (
      <main className="app">
        <h1 className="app-title">스마트보카</h1>
        <p role="alert">
          앱을 시작하지 못했습니다: {startError}
          <br />
          시크릿(비공개) 창에서는 저장소를 쓸 수 없을 수 있습니다. 일반 창에서 열어 주세요.
        </p>
      </main>
    )
  }
  if (!services) {
    return (
      <main className="app">
        <p className="muted">불러오는 중</p>
      </main>
    )
  }

  // 화면 형식은 intro.html 을 따른다: 어두운 돌판 hero (화면 전체 폭) → 크림 본문 → 돌판 바닥글
  return (
    <>
      <header className="hero">
        <div className="wrap hero-inner">
          <div>
            <p className="eyebrow">SMART VOCA · 스마트보카</p>
            <h1 className="hero-title">
              <em>동사</em>만 알아도 대박이다
            </h1>
          </div>
          <nav className="hero-actions" aria-label="주 메뉴">
            {screen !== 'today' && (
              <button type="button" className="button hero-button ghost" onClick={() => setScreen('today')}>
                오늘의 단어
              </button>
            )}
            {SHOW_SETTINGS && screen !== 'settings' && (
              <button type="button" className="button hero-button ghost" onClick={() => setScreen('settings')}>
                설정
              </button>
            )}
            {screen !== 'exam' && (
              <button type="button" className="button hero-button" onClick={() => setScreen('exam')}>
                시험 시작
              </button>
            )}
          </nav>
        </div>
      </header>

      <main className="app wrap">
        {SHOW_SETTINGS && screen === 'settings' && (
          <SettingsScreen services={services} onImported={() => setImportCount((n) => n + 1)} onClose={() => setScreen('today')} />
        )}
        {screen === 'exam' && <ExamScreen services={services} today={today} onClose={() => setScreen('today')} />}
        {screen === 'today' && (
          <TodayWordScreen services={services} today={today} refreshKey={importCount} onStartExam={() => setScreen('exam')} />
        )}
      </main>

      <footer className="site-footer">
        <div className="wrap">
          <SharePanel />
          <p className="site-footer-note">Smart Voca · 학교 수업 과제 프로젝트 · 2026. 기록은 이 브라우저에만 저장됩니다.</p>
        </div>
      </footer>

      {changedFrom && (
        <div className="dialog-backdrop" onClick={() => setChangedFrom(null)}>
          <div className="dialog" role="alertdialog" aria-labelledby="day-changed-title" onClick={(e) => e.stopPropagation()}>
            <h2 id="day-changed-title">날짜가 바뀌었습니다</h2>
            <p>
              {changedFrom} → {today}
              <br />
              오늘의 단어 목록이 새로 준비되었습니다.
            </p>
            <button type="button" className="button" autoFocus onClick={() => setChangedFrom(null)}>
              확인
            </button>
          </div>
        </div>
      )}
    </>
  )
}
