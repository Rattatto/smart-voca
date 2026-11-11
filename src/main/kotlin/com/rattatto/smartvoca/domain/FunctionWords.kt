// 2026-11-11 - Smart Voca - 기능어 목록과 일일 TIP 선택 (관사, 조동사, 전치사, 접속사, 대명사, 기능 부사)
// 파일 위치: src/main/kotlin/com/rattatto/smartvoca/domain/FunctionWords.kt

package com.rattatto.smartvoca.domain

import java.time.LocalDate

/** 기능어 한 개와 그 쓰임 TIP. 목록에는 뺄 단어이고, 대신 TIP 칸에 보여 준다. */
data class FunctionWordTip(
    val word: String,
    val category: String,
    val tipKo: String,
)

/**
 * 기능어 목록 (assets/function_words.csv). 일일 목록에서 빼고, 날짜마다 TIP 한 개를 보여 준다.
 * 파일 형식은 `word,category,tip_ko` 이며 TIP 에는 쉼표를 쓰지 않는다.
 */
class FunctionWords(val tips: List<FunctionWordTip>) {

    /** 소문자 기준 기능어 집합. 일일 목록 선정에서 이 단어를 뺀다. */
    val words: Set<String> = tips.map { it.word.lowercase() }.toSet()

    /** 날짜마다 다른 TIP 을 고른다. 같은 날짜에는 항상 같은 TIP 이다. */
    fun dailyTip(date: LocalDate): FunctionWordTip? =
        if (tips.isEmpty()) null else tips[(date.toEpochDay() % tips.size).toInt()]

    companion object {
        const val ASSET_FILE_NAME = "function_words.csv"

        fun parse(csvText: String): FunctionWords {
            val tips = csvText.lineSequence()
                .drop(1) // 헤더
                .filter { it.isNotBlank() }
                .map { line ->
                    val fields = line.split(",", limit = 3)
                    require(fields.size == 3) { "기능어 CSV 형식 오류: $line" }
                    FunctionWordTip(fields[0].trim(), fields[1].trim(), fields[2].trim())
                }
                .toList()
            return FunctionWords(tips)
        }
    }
}
