// 2026-11-25 - Smart Voca - CSV 가져오기 (파일 선택, 저장, 결과와 건너뛴 행 표시) (FR-001~003). Kotlin ImportScreen.kt 이관
// 파일 위치: web/src/ui/ImportPanel.tsx

import { type ChangeEvent, useState } from 'react'
import type { AppServices } from '../data/appServices'
import { REQUIRED_COLUMNS } from '../data/csv/wordCsvParser'
import type { ImportSummary } from '../data/wordRepository'

const MAX_SKIPPED_ROWS_SHOWN = 50

interface Props {
  services: AppServices
  /** 저장이 성공했을 때 한 번 호출된다. 오늘 목록을 다시 읽히는 데 쓴다. */
  onImported: () => void
}

export function ImportPanel({ services, onImported }: Props) {
  const [statusText, setStatusText] = useState('CSV 파일을 고르면 저장 결과가 여기에 표시됩니다.')
  const [summary, setSummary] = useState<ImportSummary | null>(null)
  const [busy, setBusy] = useState(false)

  const onFileChosen = async (event: ChangeEvent<HTMLInputElement>) => {
    const input = event.target
    const file = input.files?.[0]
    if (!file) return
    setBusy(true)
    try {
      // File.text() 는 UTF-8 로 읽는다. 엑셀의 "CSV UTF-8" 형식으로 저장한 파일을 쓴다
      const result = await services.words.importCsv(await file.text())
      setSummary(result)
      setStatusText(`저장 ${result.inserted}개, 갱신 ${result.updated}개, 건너뜀 ${result.skipped.length}개`)
      onImported()
    } catch (error) {
      // 필수 열 누락, 헤더 없음, 따옴표 오류 등 파일 형식 문제
      setSummary(null)
      setStatusText(`가져오기 실패: ${error instanceof Error ? error.message : error}`)
    } finally {
      setBusy(false)
      input.value = '' // 같은 파일을 고쳐서 다시 고를 때도 change 이벤트가 나게 비운다
    }
  }

  return (
    <div className="stack">
      <h3 className="field-label">단어 가져오기</h3>
      <p className="muted">
        필수 열: {REQUIRED_COLUMNS.join(', ')} (선택: pos, example_en, example_ko, rating). UTF-8 CSV 파일.
      </p>
      <input className="file-input" type="file" accept=".csv,text/csv" onChange={(e) => void onFileChosen(e)} disabled={busy} aria-label="단어 CSV 파일 선택" />
      <p role="status">{busy ? '저장 중…' : statusText}</p>
      {summary && summary.skipped.length > 0 && (
        // 건너뛴 행이 많을 수 있어서 앞의 50개만 보여 준다
        <ul className="skipped-list">
          {summary.skipped.slice(0, MAX_SKIPPED_ROWS_SHOWN).map((row) => (
            <li key={row.lineNumber}>
              {row.lineNumber}행: {row.reason}
            </li>
          ))}
        </ul>
      )}
    </div>
  )
}
