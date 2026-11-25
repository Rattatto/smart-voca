// 2026-11-25 - Smart Voca - 시험 화면 (객관식 우선, 승격 후 주관식, 강등 안내) (FR-031, FR-033, FR-035, FR-036, FR-038). Kotlin ExamScreen.kt 이관
// 파일 위치: web/src/ui/ExamScreen.tsx

import { type FormEvent, useEffect, useState } from 'react'
import type { AppServices } from '../data/appServices'
import { type ExamQuestion, isMultipleChoiceCorrect, isTypedAnswerCorrect } from '../domain/examRules'
import type { LocalDate } from '../domain/localDate'
import type { StageState } from '../domain/stageRules'

interface Props {
  services: AppServices
  today: LocalDate
  onClose: () => void
}

/** 답 한 번의 결과. 다음 문제로 넘어가면 비운다. */
interface Answered {
  isCorrect: boolean
  userInput: string
  stageNote: string | null
}

/**
 * 시험 한 회차. 객관식 (영단어 → 뜻 고르기)과 주관식 (뜻 → 영단어 입력)을 섞어 낸다.
 * 답은 시험 이력에 저장되고, 단계가 바뀌면 (승격, 강등) 화면에 알려 준다. 재노출 일정은 바꾸지 않는다 (FR-037).
 */
export function ExamScreen({ services, today, onClose }: Props) {
  const [questions, setQuestions] = useState<ExamQuestion[] | null>(null)
  const [index, setIndex] = useState(0)
  const [correctCount, setCorrectCount] = useState(0)
  const [input, setInput] = useState('')
  const [answered, setAnswered] = useState<Answered | null>(null)
  const [errorText, setErrorText] = useState<string | null>(null)

  useEffect(() => {
    let cancelled = false
    const load = async () => {
      try {
        const settings = await services.settings.load()
        const todayWords = (await services.daily.getOrCreate(today, settings.dailyCount, settings.levelFilter)).map((p) => p.word)
        const built = await services.exam.buildQuestions(today, settings.examSize, todayWords)
        if (!cancelled) setQuestions(built)
      } catch (error) {
        if (!cancelled) setErrorText(`시험을 만들지 못했습니다: ${error instanceof Error ? error.message : error}`)
      }
    }
    void load()
    return () => {
      cancelled = true
    }
  }, [services, today])

  /** 답을 채점하고 기록한다. 화면 표시는 바로 하고, 단계 안내는 저장이 끝나면 붙인다. */
  const submit = async (question: ExamQuestion, isCorrect: boolean, userInput: string) => {
    if (answered) return // 같은 문제에 두 번 답하지 않게 막는다
    if (isCorrect) setCorrectCount((n) => n + 1)
    setAnswered({ isCorrect, userInput, stageNote: null })
    const before = await services.exam.stageOf(question.word.id)
    const after = await services.exam.recordAttempt({
      wordId: question.word.id,
      date: today,
      shownMeaning: question.word.meaningKo,
      userInput,
      isCorrect,
      type: question.type,
    })
    const stageNote = stageChangeNote(before, after)
    setAnswered((current) => (current ? { ...current, stageNote } : current))
  }

  const next = () => {
    setIndex((n) => n + 1)
    setInput('')
    setAnswered(null)
  }

  return (
    <section className="stack" aria-labelledby="exam-title">
      <h2 id="exam-title" className="screen-title" style={{ color: 'var(--primary)' }}>
        시험
      </h2>
      {errorText ? (
        <p role="alert">{errorText}</p>
      ) : questions === null ? (
        <p className="muted">불러오는 중</p>
      ) : questions.length === 0 ? (
        <p>오늘 시험할 단어가 없습니다. 먼저 단어를 가져오세요.</p>
      ) : index >= questions.length ? (
        <p className="exam-result">
          결과: {correctCount} / {questions.length}
        </p>
      ) : (
        <QuestionView
          key={index}
          question={questions[index]}
          number={index + 1}
          total={questions.length}
          input={input}
          onInputChange={setInput}
          answered={answered}
          onSubmit={(isCorrect, userInput) => void submit(questions[index], isCorrect, userInput)}
          onNext={next}
          isLast={index + 1 >= questions.length}
        />
      )}
      <button type="button" className="button outlined" onClick={onClose}>
        닫기
      </button>
    </section>
  )
}

interface QuestionViewProps {
  question: ExamQuestion
  number: number
  total: number
  input: string
  onInputChange: (value: string) => void
  answered: Answered | null
  onSubmit: (isCorrect: boolean, userInput: string) => void
  onNext: () => void
  isLast: boolean
}

function QuestionView({ question, number, total, input, onInputChange, answered, onSubmit, onNext, isLast }: QuestionViewProps) {
  const { word } = question
  const submitTyped = (event: FormEvent) => {
    event.preventDefault() // 폼 기본 동작(페이지 새로고침)을 막는다. Enter 키로도 제출된다
    if (input.trim() !== '') onSubmit(isTypedAnswerCorrect(input, word.word), input)
  }

  return (
    <div className="stack">
      <p className="muted">
        문제 {number} / {total}
      </p>

      {question.type === 'mc' ? (
        <>
          <p className="exam-word" lang="en">
            {word.word}
          </p>
          <p className="muted">알맞은 뜻을 고르세요</p>
          {(question.choices ?? []).map((choice) => {
            const state = !answered ? '' : choice === word.meaningKo ? 'is-answer' : choice === answered.userInput ? 'is-picked-wrong' : ''
            return (
              <button
                key={choice}
                type="button"
                className={`choice ${state}`}
                disabled={answered !== null}
                onClick={() => onSubmit(isMultipleChoiceCorrect(choice, word), choice)}
              >
                {choice}
              </button>
            )
          })}
        </>
      ) : (
        <form className="stack" onSubmit={submitTyped}>
          <p className="exam-meaning">뜻: {word.meaningKo}</p>
          <label className="field">
            <span className="field-label">영단어</span>
            {/* 휴대폰 키보드가 첫 글자를 대문자로 바꾸거나 자동 수정하지 않게 끈다 */}
            <input
              className="text-input"
              lang="en"
              value={input}
              onChange={(event) => onInputChange(event.target.value)}
              disabled={answered !== null}
              autoFocus
              autoComplete="off"
              autoCapitalize="none"
              autoCorrect="off"
              spellCheck={false}
            />
          </label>
          {!answered && (
            <button type="submit" className="button" disabled={input.trim() === ''}>
              확인
            </button>
          )}
        </form>
      )}

      {answered && (
        <>
          <p className={`feedback ${answered.isCorrect ? 'correct' : 'wrong'}`} role="status">
            {answered.isCorrect ? '정답입니다' : `오답. 정답: ${word.word} (${word.meaningKo})`}
          </p>
          {answered.stageNote && <p className="stage-note">{answered.stageNote}</p>}
          <button type="button" className="button" onClick={onNext} autoFocus>
            {isLast ? '결과 보기' : '다음'}
          </button>
        </>
      )}
    </div>
  )
}

/** 단계가 바뀌었을 때만 안내한다. 객관식에서 주관식으로 올라가면 승격, 주관식에서 내려오면 강등. */
function stageChangeNote(before: StageState, after: StageState): string | null {
  if (before.type === 'mc' && after.type === 'typed') return '승격: 이 단어는 이제 주관식으로 나옵니다'
  if (before.type === 'typed' && after.type === 'mc') return '강등: 이 단어는 다시 객관식으로 나옵니다'
  return null
}
