// 2026-11-11 - Smart Voca - 기능어 목록 읽기와 일일 TIP 선택 테스트 (FR-010 관련 기능어 분리)
// 파일 위치: src/test/kotlin/com/rattatto/smartvoca/domain/FunctionWordsTest.kt

package com.rattatto.smartvoca.domain

import java.time.LocalDate
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import kotlin.test.assertTrue

class FunctionWordsTest {

    private val csv = """
        word,category,tip_ko
        The,관사,이미 알고 있는 대상 앞에 쓴다 (the sun)
        to,전치사,방향과 도착점을 나타낸다 (to go)
    """.trimIndent()

    @Test
    fun `헤더를 건너뛰고 단어를 소문자로 모은다`() {
        val functionWords = FunctionWords.parse(csv)

        assertEquals(listOf("The", "to"), functionWords.tips.map { it.word })
        assertEquals(setOf("the", "to"), functionWords.words)
    }

    @Test
    fun `같은 날짜에는 같은 TIP 을 고르고 날짜가 바뀌면 다음 TIP 을 고른다`() {
        val functionWords = FunctionWords.parse(csv)
        val day = LocalDate.of(2026, 11, 11)

        assertEquals(functionWords.dailyTip(day), functionWords.dailyTip(day))
        assertTrue(functionWords.dailyTip(day) != functionWords.dailyTip(day.plusDays(1)))
    }

    @Test
    fun `기능어가 없으면 TIP 은 null 이다`() {
        assertNull(FunctionWords(emptyList()).dailyTip(LocalDate.of(2026, 11, 11)))
    }

    @Test
    fun `형식이 틀린 행이 있으면 예외를 던진다`() {
        assertFailsWith<IllegalArgumentException> {
            FunctionWords.parse("word,category,tip_ko\nthe,관사")
        }
    }
}
