// 2026-11-25 - Smart Voca - 설정 화면 (일일 개수, 난이도, 동사 비중, 시험 문제 수, CSV 가져오기) (FR-040~045). Kotlin SettingsScreen.kt 이관
// 파일 위치: web/src/ui/SettingsScreen.tsx

import { useEffect, useState } from 'react'
import type { AppServices } from '../data/appServices'
import { type AppSettings, DAILY_COUNT_RANGE, DEFAULT_SETTINGS, EXAM_SIZE_RANGE } from '../data/settingsStore'
import type { Level } from '../domain/types'
import { ImportPanel } from './ImportPanel'

interface Props {
  services: AppServices
  onImported: () => void
  onClose: () => void
}

const LEVEL_OPTIONS: { label: string; value: Level | null }[] = [
  { label: '전체', value: null },
  { label: '기초', value: 'basic' },
  { label: '중급', value: 'intermediate' },
  { label: '심화', value: 'advanced' },
]

/**
 * 설정을 바꾸면 바로 저장된다. 이미 만들어진 오늘의 목록은 바뀌지 않고, 다음 날 목록부터 반영된다 (FR-045).
 * 화면 값은 먼저 바꾸고 저장은 뒤에서 한다. 슬라이더를 끄는 동안 화면이 끊기지 않게 하려는 것이다.
 */
export function SettingsScreen({ services, onImported, onClose }: Props) {
  const [settings, setSettings] = useState<AppSettings>(DEFAULT_SETTINGS)
  const [loaded, setLoaded] = useState(false)
  const store = services.settings

  useEffect(() => {
    void store.load().then((value) => {
      setSettings(value)
      setLoaded(true)
    })
  }, [store])

  const update = <K extends keyof AppSettings>(key: K, value: AppSettings[K], save: () => Promise<void>) => {
    setSettings((current) => ({ ...current, [key]: value }))
    void save()
  }

  return (
    <section className="stack" aria-labelledby="settings-title">
      <h2 id="settings-title" className="screen-title">
        설정
      </h2>
      <p className="muted">바꾼 값은 바로 저장됩니다. 오늘 목록은 그대로이고 내일 목록부터 반영됩니다.</p>

      <fieldset className="stack" disabled={!loaded} style={{ border: 0, padding: 0, margin: 0 }}>
        <label className="field">
          <span className="field-label">일일 노출 개수: {settings.dailyCount}개</span>
          <input
            type="range"
            min={DAILY_COUNT_RANGE.min}
            max={DAILY_COUNT_RANGE.max}
            step={1}
            value={settings.dailyCount}
            onChange={(e) => {
              const value = Number(e.target.value)
              update('dailyCount', value, () => store.saveDailyCount(value))
            }}
          />
        </label>

        <div className="field" role="group" aria-labelledby="level-label">
          <span id="level-label" className="field-label">
            난이도
          </span>
          <div className="chip-row">
            {LEVEL_OPTIONS.map((option) => (
              <button
                key={option.label}
                type="button"
                className="chip"
                aria-pressed={settings.levelFilter === option.value}
                onClick={() => update('levelFilter', option.value, () => store.saveLevelFilter(option.value))}
              >
                {option.label}
              </button>
            ))}
          </div>
        </div>

        <label className="field">
          <span className="field-label">동사 비중: {settings.verbRatio}%</span>
          <input
            type="range"
            min={30}
            max={90}
            step={10}
            value={settings.verbRatio}
            onChange={(e) => {
              const value = Number(e.target.value)
              update('verbRatio', value, () => store.saveVerbRatio(value))
            }}
          />
          <span className="muted">동사 비중은 저장만 되며, 품사 데이터가 준비되면 목록에 적용됩니다.</span>
        </label>

        <label className="field">
          <span className="field-label">시험 문제 수: {settings.examSize}문제</span>
          <input
            type="range"
            min={EXAM_SIZE_RANGE.min}
            max={EXAM_SIZE_RANGE.max}
            step={1}
            value={settings.examSize}
            onChange={(e) => {
              const value = Number(e.target.value)
              update('examSize', value, () => store.saveExamSize(value))
            }}
          />
        </label>
      </fieldset>

      <div className="section">
        <ImportPanel services={services} onImported={onImported} />
      </div>

      <button type="button" className="button" onClick={onClose}>
        닫기
      </button>
    </section>
  )
}
