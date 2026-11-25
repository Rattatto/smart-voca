// 2026-11-25 - Smart Voca - CSV 텍스트를 행(record) 단위로 나눔 (따옴표 안의 쉼표·줄바꿈 처리). Kotlin CsvRecordReader.kt 이관
// 파일 위치: web/src/data/csv/csvRecordReader.ts

/** 한 행과 그 행이 시작된 원본 줄 번호 (오류 안내에 쓴다). */
export interface CsvRecord {
  lineNumber: number
  fields: string[]
}

/** 따옴표가 닫히지 않은 채 파일이 끝나면 던진다. */
export class UnclosedQuoteError extends Error {
  constructor(lineNumber: number) {
    super(`${lineNumber}행에서 따옴표가 닫히지 않았습니다.`)
    this.name = 'UnclosedQuoteError'
  }
}

/**
 * 외부 라이브러리 없이 CSV 를 읽는다. 학습용 단순 구현이라 다음만 지원한다.
 * - 쉼표 구분, 큰따옴표로 감싼 필드 (안의 쉼표, 줄바꿈 허용)
 * - `""` 는 큰따옴표 하나로 해석
 * - `\r\n` 과 `\n` 모두 행 구분으로 인정
 */
export function readCsvRecords(text: string): CsvRecord[] {
  const records: CsvRecord[] = []
  let fields: string[] = []
  let field = ''
  let inQuotes = false
  let currentLine = 1
  let recordStartLine = 1

  const endField = () => {
    fields.push(field)
    field = ''
  }
  const endRecord = () => {
    endField()
    records.push({ lineNumber: recordStartLine, fields })
    fields = []
  }

  for (let index = 0; index < text.length; index++) {
    const char = text[index]
    if (inQuotes) {
      if (char === '"' && text[index + 1] === '"') {
        field += '"'
        index++
      } else if (char === '"') {
        inQuotes = false
      } else if (char === '\r' && text[index + 1] === '\n') {
        // 따옴표 안의 \r\n 도 \n 으로 맞춘다 (Kotlin 판은 \r 이 남았음, 웹 이관 때 수정)
      } else {
        if (char === '\n') currentLine++
        field += char
      }
    } else if (char === '"') {
      inQuotes = true
    } else if (char === ',') {
      endField()
    } else if (char === '\n') {
      endRecord()
      currentLine++
      recordStartLine = currentLine
    } else if (char !== '\r') {
      // \r\n 의 \r 은 무시
      field += char
    }
  }

  if (inQuotes) throw new UnclosedQuoteError(recordStartLine)
  // 마지막 줄 끝에 줄바꿈이 없어도 마지막 행을 살린다
  if (field !== '' || fields.length > 0) endRecord()
  return records
}
