// 2026-10-28 - Smart Voca - CSV 텍스트를 행(record) 단위로 나눔 (따옴표 안의 쉼표·줄바꿈 처리)
// 파일 위치: src/main/kotlin/com/rattatto/smartvoca/data/csv/CsvRecordReader.kt

package com.rattatto.smartvoca.data.csv

/**
 * 외부 라이브러리 없이 CSV 를 읽는다. 학습용 단순 구현이라 다음만 지원한다.
 * - 쉼표 구분, 큰따옴표로 감싼 필드 (안의 쉼표, 줄바꿈 허용)
 * - `""` 는 큰따옴표 하나로 해석
 * - `\r\n` 과 `\n` 모두 행 구분으로 인정
 */
object CsvRecordReader {

    /** 한 행과 그 행이 시작된 원본 줄 번호 (오류 안내에 쓴다). */
    data class Record(val lineNumber: Int, val fields: List<String>)

    /** 따옴표가 닫히지 않은 채 파일이 끝나면 던진다. */
    class UnclosedQuoteException(lineNumber: Int) :
        IllegalArgumentException("${lineNumber}행에서 따옴표가 닫히지 않았습니다.")

    fun read(text: String): List<Record> {
        val records = mutableListOf<Record>()
        val fields = mutableListOf<String>()
        val field = StringBuilder()
        var inQuotes = false
        var currentLine = 1
        var recordStartLine = 1
        var index = 0

        fun endField() {
            fields += field.toString()
            field.clear()
        }

        fun endRecord() {
            endField()
            records += Record(recordStartLine, fields.toList())
            fields.clear()
        }

        while (index < text.length) {
            val c = text[index]
            when {
                inQuotes -> when {
                    c == '"' && text.getOrNull(index + 1) == '"' -> {
                        field.append('"')
                        index++
                    }
                    c == '"' -> inQuotes = false
                    else -> {
                        if (c == '\n') currentLine++
                        field.append(c)
                    }
                }
                c == '"' -> inQuotes = true
                c == ',' -> endField()
                c == '\n' -> {
                    endRecord()
                    currentLine++
                    recordStartLine = currentLine
                }
                c == '\r' -> Unit // \r\n 의 \r 은 무시
                else -> field.append(c)
            }
            index++
        }

        if (inQuotes) throw UnclosedQuoteException(recordStartLine)
        // 마지막 줄 끝에 줄바꿈이 없어도 마지막 행을 살린다
        if (field.isNotEmpty() || fields.isNotEmpty()) endRecord()
        return records
    }
}
