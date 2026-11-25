// 2026-11-25 - Smart Voca - 오늘의 단어 카드 학습: 한 장씩 노출, 뜻과 가이드를 보고 영단어를 두 번 따라 쓰면 완료 (FR-010, FR-013, FR-020)
// 파일 위치: web/src/ui/TodayWordScreen.tsx
// 2026-11-25 변경: 목록 + "학습 완료" 버튼 방식 → 카드 한 장씩 + 따라 쓰기 두 번 방식 (사용자 결정)

import { useEffect, useRef, useState } from 'react'
import { STUDY_TYPING_REPEAT } from '../appConfig'
import type { AppServices } from '../data/appServices'
import type { PickedWord } from '../domain/dailyWordSelector'
import type { FunctionWordTip } from '../domain/functionWords'
import type { LocalDate } from '../domain/localDate'
import { type BlankState, firstUnstudiedIndex, isCardComplete, judgeBlank } from '../domain/studyFlow'
import './study.css'

interface Props {
  services: AppServices
  today: LocalDate
  /** 바뀌면 목록을 다시 읽는다 (가져오기 성공 등). */
  refreshKey: number
  onStartExam: () => void
}

/** 완료 표시를 잠깐 보여 준 뒤 다음 카드로 넘어간다 (ms). */
const NEXT_CARD_DELAY_MS = 700

/**
 * 오늘 목록(설정한 일일 개수)을 카드로 한 장씩 보여 준다. 완료한 카드는 건너뛴다.
 * 목록은 같은 날 다시 열어도 그대로다 (FR-013). 완료 기록은 재노출 일정을 올린다 (FR-020).
 */
export function TodayWordScreen({ services, today, refreshKey, onStartExam }: Props) {
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

  /** 완료 기록은 바로 저장하고, 다음 카드로 넘기는 것만 늦춘다. 완료 표시를 잠깐 보여 주기 위해서다. */
  const completeCard = async (wordId: number) => {
    await services.study.markStudied(wordId, today)
    setTimeout(() => setStudiedIds((previous) => new Set(previous).add(wordId)), NEXT_CARD_DELAY_MS)
  }

  const currentIndex = items ? firstUnstudiedIndex(items.map((item) => item.word.id), studiedIds) : -1
  const doneCount = items ? items.filter((item) => studiedIds.has(item.word.id)).length : 0

  return (
    <section className="stack" aria-labelledby="today-title">
      <h2 id="today-title" className="screen-title">
        오늘의 단어 <span className="muted">({today})</span>
      </h2>
      {items && items.length > 0 && <ProgressBar done={doneCount} total={items.length} />}
      <div className="today-layout">
        <div className="stack">
          {errorText ? (
            <p role="alert">{errorText}</p>
          ) : items === null ? (
            <p className="muted">불러오는 중</p>
          ) : items.length === 0 ? (
            <p className="card">오늘 학습할 단어가 없습니다.</p>
          ) : currentIndex === -1 ? (
            <AllDone items={items} onStartExam={onStartExam} />
          ) : (
            <StudyCard
              key={items[currentIndex].word.id}
              item={items[currentIndex]}
              number={currentIndex + 1}
              total={items.length}
              onComplete={completeCard}
            />
          )}
        </div>
        <DailyTipPanel tip={services.functionWords.dailyTip(today)} />
      </div>
    </section>
  )
}

function ProgressBar({ done, total }: { done: number; total: number }) {
  return (
    <div className="study-progress">
      <div className="study-progress-track" role="progressbar" aria-valuemin={0} aria-valuemax={total} aria-valuenow={done} aria-label="오늘 학습 진행">
        <div className="study-progress-fill" style={{ width: `${(done / total) * 100}%` }} />
      </div>
      <span className="muted">
        {done} / {total} 완료
      </span>
    </div>
  )
}

interface StudyCardProps {
  item: PickedWord
  number: number
  total: number
  onComplete: (wordId: number) => Promise<void>
}

/**
 * 카드 한 장. 뜻과 영단어(가이드)를 보여 주고, 빈칸 두 개에 차례로 따라 쓰게 한다.
 * 첫 칸이 맞아야 둘째 칸이 열린다. 두 칸이 모두 맞으면 완료를 기록하고 잠시 뒤 다음 카드로 넘어간다.
 * 카드가 바뀌면 `key` 가 바뀌어 입력 상태가 새로 시작된다.
 */
function StudyCard({ item, number, total, onComplete }: StudyCardProps) {
  const { word } = item
  const [inputs, setInputs] = useState<string[]>(() => Array(STUDY_TYPING_REPEAT).fill(''))
  const [completed, setCompleted] = useState(false)
  const inputRefs = useRef<(HTMLInputElement | null)[]>([])

  const states: BlankState[] = inputs.map((input) => judgeBlank(input, word.word))
  // 앞 칸이 모두 맞아야 다음 칸을 쓸 수 있다
  const isUnlocked = (index: number) => states.slice(0, index).every((state) => state === 'correct')
  // 지금 써야 할 칸 = 아직 맞지 않은 첫 칸
  const activeIndex = states.findIndex((state) => state !== 'correct')

  // 칸이 맞으면 다음 칸으로 커서를 옮긴다. 화면에 잠금이 풀린 "뒤"(effect)에 옮겨야 한다.
  // 입력 처리 중에 바로 옮기면 칸이 아직 disabled 라서 커서가 들어가지 않는 경우가 있었다
  useEffect(() => {
    if (activeIndex > 0) inputRefs.current[activeIndex]?.focus()
  }, [activeIndex])

  const onChange = (index: number, value: string) => {
    if (completed) return
    const next = inputs.map((input, i) => (i === index ? value : input))
    setInputs(next)
    if (isCardComplete(next, word.word, STUDY_TYPING_REPEAT)) {
      setCompleted(true)
      void onComplete(word.id)
    }
  }

  return (
    <article className={`card study-card ${completed ? 'is-done' : ''}`} aria-label={`단어 카드 ${number} / ${total}`}>
      {/* intro.html 의 단어 카드 형식: 배지 → 영단어 + 품사 → 뜻 → 예문 */}
      <div className="word-head">
        <span className={`badge ${item.source === 'review' ? 'review' : ''}`}>{item.source === 'review' ? '복습' : '새 단어'}</span>
        <span className="word-number">
          {number} / {total}
        </span>
      </div>

      <p>
        <span className="study-word" lang="en">
          {word.word}
        </span>
        {word.pos === 'verb' && <span className="pos">동사</span>}
      </p>
      <p className="study-meaning">{word.meaningKo}</p>
      {word.exampleEn && (
        <p className="word-example" lang="en">
          {word.exampleEn}
        </p>
      )}
      {word.exampleKo && <p className="word-example-ko">{word.exampleKo}</p>}

      <p className="study-guide">위 영단어를 보고 아래 빈칸에 {STUDY_TYPING_REPEAT}번 직접 써 보세요.</p>
      <div className="study-blanks">
        {inputs.map((input, index) => (
          <label key={index} className="study-blank">
            <span className="study-blank-label">{index + 1}번째</span>
            {/* 휴대폰 키보드가 첫 글자를 대문자로 바꾸거나 자동 수정하지 않게 끈다 */}
            <input
              ref={(element) => {
                inputRefs.current[index] = element
              }}
              className={`text-input study-input state-${states[index]}`}
              lang="en"
              value={input}
              placeholder={word.word}
              onChange={(event) => onChange(index, event.target.value)}
              disabled={completed || !isUnlocked(index)}
              autoFocus={index === 0}
              autoComplete="off"
              autoCapitalize="none"
              autoCorrect="off"
              spellCheck={false}
              aria-invalid={states[index] === 'wrong'}
            />
            <span className="study-mark" aria-hidden="true">
              {states[index] === 'correct' ? '✓' : states[index] === 'wrong' ? '✗' : ''}
            </span>
          </label>
        ))}
      </div>
      <p className="study-status" role="status">
        {completed ? '학습 완료! 다음 카드로 넘어갑니다' : states.includes('wrong') ? '철자를 다시 확인해 보세요' : ''}
      </p>
    </article>
  )
}

/** 오늘 카드를 모두 끝냈을 때. 오늘 본 단어를 다시 보여 주고 시험으로 이어 준다. */
function AllDone({ items, onStartExam }: { items: PickedWord[]; onStartExam: () => void }) {
  return (
    <div className="card stack study-all-done">
      <p className="study-done-title">오늘 단어 {items.length}개를 모두 학습했습니다 🎉</p>
      <ul className="study-done-list">
        {items.map((item) => (
          <li key={item.word.id}>
            <span lang="en">{item.word.word}</span> <span className="muted">{item.word.meaningKo}</span>
          </li>
        ))}
      </ul>
      <button type="button" className="button" onClick={onStartExam}>
        시험 보러 가기
      </button>
    </div>
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
