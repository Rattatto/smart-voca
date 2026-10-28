// 2026-10-28 - Smart Voca - 단어 CSV 헤더·행을 검증해 저장할 항목과 건너뛴 행을 나눔 (FR-001~005)
// 파일 위치: src/main/kotlin/com/rattatto/smartvoca/data/csv/WordCsvParser.kt

package com.rattatto.smartvoca.data.csv

/** 필수 열이 없으면 적재를 중단한다 (FR-002). 누락된 열 이름을 담는다. */
class MissingColumnsException(val missingColumns: List<String>) :
    IllegalArgumentException("필수 열이 없습니다: ${missingColumns.joinToString(", ")}")

/** 건너뛴 행의 원본 줄 번호와 사유 (FR-003). */
data class SkippedRow(val lineNumber: Int, val reason: String)

/** 가져오기 결과. 저장할 단어와 건너뛴 행 목록을 함께 준다. */
data class WordImportResult(
    val words: List<WordItem>,
    val skipped: List<SkippedRow>,
)

/**
 * 단어 CSV 파서. 파일 전체를 검증하되, 행 단위 오류는 건너뛰고 기록만 한다.
 * 헤더 오류는 파일 전체가 잘못된 것이므로 예외로 중단한다.
 */
class WordCsvParser {

    fun parse(text: String): WordImportResult {
        // Windows 메모장이 저장한 CSV 는 맨 앞에 BOM(U+FEFF) 이 붙어 있어 먼저 제거한다
        val records = CsvRecordReader.read(text.removePrefix(BYTE_ORDER_MARK))
            .filterNot { it.fields.all(String::isBlank) }
        val headerRecord = records.firstOrNull()
            ?: throw IllegalArgumentException("CSV 에 헤더 행이 없습니다.")

        val header = headerRecord.fields.map { it.trim() }
        val missingColumns = REQUIRED_COLUMNS.filterNot { it in header }
        if (missingColumns.isNotEmpty()) throw MissingColumnsException(missingColumns)

        val columnIndexByName = header.withIndex().associate { it.value to it.index }
        val words = mutableListOf<WordItem>()
        val skipped = mutableListOf<SkippedRow>()
        // 같은 빈도 순위가 두 번 나오면 먼저 나온 행을 남긴다 (데이터 설계 §4 의 중복 키)
        val lineByRank = mutableMapOf<Int, Int>()

        for (record in records.drop(1)) {
            try {
                val item = toWordItem(record.fields, header.size, columnIndexByName)
                val firstLine = lineByRank[item.freqRank]
                if (firstLine != null) {
                    skipped += SkippedRow(record.lineNumber, "freq_rank ${item.freqRank} 중복 (${firstLine}행과 같음)")
                    continue
                }
                lineByRank[item.freqRank] = record.lineNumber
                words += item
            } catch (error: RowFormatException) {
                skipped += SkippedRow(record.lineNumber, error.message.orEmpty())
            }
        }
        return WordImportResult(words, skipped)
    }

    private fun toWordItem(
        fields: List<String>,
        expectedSize: Int,
        columnIndexByName: Map<String, Int>,
    ): WordItem {
        if (fields.size != expectedSize) {
            throw RowFormatException("열 개수 불일치 (헤더 ${expectedSize}개, 행 ${fields.size}개)")
        }

        fun valueOf(column: String): String = columnIndexByName[column]?.let { fields[it].trim() }.orEmpty()
        fun optionalValueOf(column: String): String? = valueOf(column).ifBlank { null }

        val word = valueOf("word")
        if (word.isBlank()) throw RowFormatException("word 누락")
        if (!WORD_PATTERN.matches(word)) throw RowFormatException("word 형식 오류 ($word)")

        val meaningKo = valueOf("meaning_ko")
        if (meaningKo.isBlank()) throw RowFormatException("meaning_ko 누락")

        val rankText = valueOf("freq_rank")
        if (rankText.isBlank()) throw RowFormatException("freq_rank 누락")
        val freqRank = rankText.toIntOrNull()?.takeIf { it > 0 }
            ?: throw RowFormatException("freq_rank 는 1 이상의 정수여야 함 ($rankText)")

        val levelText = valueOf("level")
        if (levelText.isBlank()) throw RowFormatException("level 누락")
        val level = Level.fromCsv(levelText)
            ?: throw RowFormatException("level 값 오류 ($levelText, basic·intermediate·advanced 중 하나)")

        return WordItem(
            word = word,
            pos = optionalValueOf("pos"),
            meaningKo = meaningKo,
            exampleEn = optionalValueOf("example_en"),
            exampleKo = optionalValueOf("example_ko"),
            freqRank = freqRank,
            level = level,
            rating = optionalValueOf("rating"),
        )
    }

    /** 행 하나의 검증 실패. 메시지가 그대로 건너뛴 사유로 쓰인다. */
    private class RowFormatException(message: String) : IllegalArgumentException(message)

    private companion object {
        const val BYTE_ORDER_MARK = "﻿"

        val REQUIRED_COLUMNS = listOf("word", "meaning_ko", "freq_rank", "level")

        // 영문자로 시작하고 영문자, 아포스트로피, 하이픈, 공백만 허용 (데이터 설계 §4)
        val WORD_PATTERN = Regex("[A-Za-z][A-Za-z'\\- ]*")
    }
}
