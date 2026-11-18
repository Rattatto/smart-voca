// 2026-11-18 - Smart Voca - 시험 출제와 채점 규칙 테스트 (FR-031, FR-033, FR-035)
// 파일 위치: src/test/kotlin/com/rattatto/smartvoca/domain/ExamRulesTest.kt

package com.rattatto.smartvoca.domain

import com.rattatto.smartvoca.data.db.WordEntity
import kotlin.random.Random
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ExamRulesTest {

    private fun word(id: Long) = WordEntity(
        id = id,
        word = "word$id",
        pos = null,
        meaningKo = "뜻$id",
        exampleEn = null,
        exampleKo = null,
        freqRank = id.toInt(),
        level = "basic",
        rating = null,
    )

    @Test
    fun `오늘 목록과 재노출 대상을 합치되 같은 단어는 한 번만 낸다`() {
        val today = listOf(word(1), word(2), word(3))
        val review = listOf(word(3), word(4))

        val planned = ExamPlanner.plan(today, review, examSize = 10, random = Random(1))

        assertEquals(setOf(1L, 2L, 3L, 4L), planned.map { it.id }.toSet())
        assertEquals(4, planned.size)
    }

    @Test
    fun `설정 문제 수보다 후보가 많으면 문제 수만큼만 뽑는다`() {
        val today = (1..30).map { word(it.toLong()) }

        val planned = ExamPlanner.plan(today, emptyList(), examSize = 10, random = Random(7))

        assertEquals(10, planned.size)
        assertEquals(10, planned.map { it.id }.toSet().size)
    }

    @Test
    fun `후보가 없으면 빈 목록이다`() {
        assertTrue(ExamPlanner.plan(emptyList(), emptyList(), examSize = 10).isEmpty())
    }

    @Test
    fun `문제 수가 1 미만이면 예외를 던진다`() {
        assertFailsWith<IllegalArgumentException> {
            ExamPlanner.plan(listOf(word(1)), emptyList(), examSize = 0)
        }
    }

    @Test
    fun `앞뒤 공백과 대소문자는 무시하고 채점한다`() {
        assertTrue(ExamGrader.isCorrect("  Go ", "go"))
        assertTrue(ExamGrader.isCorrect("GO", "go"))
    }

    @Test
    fun `철자가 다르면 오답이다`() {
        assertFalse(ExamGrader.isCorrect("went", "go"))
        assertFalse(ExamGrader.isCorrect("", "go"))
        assertFalse(ExamGrader.isCorrect("g o", "go"))
    }
}
