// 2026-11-25 - Smart Voca - 오늘의 일일 단어 카드 목록, 학습 완료 버튼, 기능어 TIP 카드 (FR-010, FR-013, FR-020). Kotlin TodayWordScreen.kt 이관
// 파일 위치: web/src/ui/TodayWordScreen.tsx

import { useEffect, useState } from 'react'
import type { AppServices } from '../data/appServices'
import type { PickedWord } from '../domain/dailyWordSelector'
import type { FunctionWordTip } from '../domain/functionWords'
import type { LocalDate } from '../domain/localDate'

interface Props {
  services: AppServices
  today: LocalDate
  /** 바뀌면 목록을 다시 읽는다 (가져오기 성공 등). */
  refreshKey: number
  onOpenSettings: () => void
}

/**
 * 오늘의 단어 목록과 기능어 TIP. 목록이 없으면 현재 설정으로 만들고, 있으면 저장된 목록을 쓴다.
 * 설정을 바꿔도 오늘 목록은 바뀌지 않는다 (FR-045).
 */
export function TodayWordScreen({ services, today, refreshKey, onOpenSettings }: Props) {
  const [items, setItems] = useState<PickedWord[] | null>(null)
  const [studiedIds, setStudiedIds] = useState<ReadonlySet<number>>(new Set())
  const [errorText, setErrorText] = useState<string | null>(null)

  useEffect(() => {
    // 응답이 늦게 와서 이미 다른 날짜로 넘어갔다면 그 결과는 버린다
    let cancelled = false
    const load = async () => {
      try {
        const settings = await services.settings.load()
        const list = await services.daily.getOrCreate(today, settings.dailyCount, settings.levelFilter)
        const studied = await services.study.studiedIdsOn(today)
        if (cancelled) return
        setItems(list)
        setStudiedIds(studied)
        setErrorText(null)
      } catch (error) {
        if (!cancelled) setErrorText(`목록을 만들지 못했습니다: ${error instanceof Error ? error.message : error}`)
      }
    }
    void load()
    return () => {
      cancelled = true
    }
  }, [services, today, refreshKey])

  const markStudied = async (wordId: number) => {
    await services.study.markStudied(wordId, today)
    setStudiedIds((previous) => new Set(previous).add(wordId))
  }

  const doneCount = items?.filter((item) => studiedIds.has(item.word.id)).length ?? 0

  return (
    <section className="stack" aria-labelledby="today-title">
      <h2 id="today-title" className="screen-title">
        오늘의 단어 ({today})
        {items && items.length > 0 && <span className="muted"> · {doneCount}/{items.length} 완료</span>}
      </h2>
      <div className="today-layout">
        <WordList errorText={errorText} items={items} studiedIds={studiedIds} onStudied={markStudied} onOpenSettings={onOpenSettings} />
        <DailyTipPanel tip={services.functionWords.dailyTip(today)} />
      </div>
    </section>
  )
}

interface WordListProps {
  errorText: string | null
  items: PickedWord[] | null
  studiedIds: ReadonlySet<number>
  onStudied: (wordId: number) => void
  onOpenSettings: () => void
}

function WordList({ errorText, items, studiedIds, onStudied, onOpenSettings }: WordListProps) {
  if (errorText) return <p role="alert">{errorText}</p>
  if (items === null) return <p className="muted">불러오는 중</p>
  if (items.length === 0) {
    // 빈 상태에서 바로 가져오기로 갈 수 있게 한다 (11/11 요청 2.2 F)
    return (
      <div className="card stack">
        <p>가져온 단어가 없습니다. 설정에서 단어 CSV 를 먼저 가져오세요.</p>
        <button type="button" className="button" onClick={onOpenSettings}>
          단어 가져오러 가기
        </button>
      </div>
    )
  }
  return (
    <ol className="word-list">
      {items.map((item, index) => (
        <WordCard
          key={item.word.id}
          number={index + 1}
          item={item}
          studied={studiedIds.has(item.word.id)}
          onStudied={() => onStudied(item.word.id)}
        />
      ))}
    </ol>
  )
}

/** 단어 한 개를 카드로 보여 준다. 단어와 뜻이 위, 예문이 가운데, 완료 버튼이 아래에 온다. */
function WordCard({ number, item, studied, onStudied }: { number: number; item: PickedWord; studied: boolean; onStudied: () => void }) {
  const { word } = item
  return (
    <li className="card word-card">
      <div className="word-head">
        <span className="word-number">{number}</span>
        <span className="word-text" lang="en">
          {word.word}
        </span>
        {item.source === 'review' && <span className="badge">재노출</span>}
      </div>
      <p className="word-meaning">{word.meaningKo}</p>
      {word.exampleEn && (
        <p className="word-example" lang="en">
          {word.exampleEn}
        </p>
      )}
      {word.exampleKo && <p className="word-example-ko">{word.exampleKo}</p>}
      {studied ? (
        <p className="word-done">오늘 완료됨</p>
      ) : (
        <button type="button" className="button full" onClick={onStudied}>
          학습 완료
        </button>
      )}
    </li>
  )
}

/** 기능어 TIP 을 연한 주황 카드에 보여 준다. */
function DailyTipPanel({ tip }: { tip: FunctionWordTip | null }) {
  return (
    <aside className="tip-card" aria-label="오늘의 TIP">
      <p className="tip-label">오늘의 TIP</p>
      {tip === null ? (
        <p className="tip-text muted">TIP 이 없습니다</p>
      ) : (
        <>
          <p className="tip-word" lang="en">
            {tip.word}
          </p>
          <p className="tip-category">{tip.category}</p>
          <p className="tip-text">{tip.tipKo}</p>
        </>
      )}
    </aside>
  )
}
