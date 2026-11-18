// 2026-11-18 - Smart Voca - 시험 출제, 객관식 보기, 채점 규칙 테스트 (FR-031, FR-033, FR-035, FR-038)
// 파일 위치: src/test/kotlin/com/rattatto/smartvoca/domain/ExamRulesTest.kt

package com.rattatto.smartvoca.domain

import com.rattatto.smartvoca.data.db.WordEntity
import kotlin.random.Random
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ExamRulesTest {

    private fun word(id: Long, level: String = "basic") = WordEntity(
        id = id,
        word = "word$id",
        pos = null,
        meaningKo = "뜻$id",
        exampleEn = null,
        exampleKo = null,
        freqRank = id.toInt(),
        level = level,
        rating = null,
    )

    private val pool = (1..30).map { word(it.toLong()) }

    @Test
    fun `객관식 보기는 정답 뜻을 포함한 4개이고 같은 난이도에서 뽑는다`() {
        val answer = word(1)

        val choices = ChoiceBuilder.build(answer, pool, Random(3))!!

        assertEquals(4, choices.size)
        assertEquals(4, choices.toSet().size)
        assertTrue("뜻1" in choices)
    }

    @Test
    fun `같은 난이도 뜻이 부족하면 보기를 만들지 않는다`() {
        val answer = word(1, level = "advanced")

        assertNull(ChoiceBuilder.build(answer, pool, Random(3)))
    }

    @Test
    fun `객관식 정답은 고른 뜻이 정답 뜻과 같을 때만 맞다`() {
        val answer = word(1)

        assertTrue(ExamGrader.isMultipleChoiceCorrect("뜻1", answer))
        assertFalse(ExamGrader.isMultipleChoiceCorrect("뜻2", answer))
    }

    @Test
    fun `객관식 단계 단어는 객관식으로만 나오고 설정 문제 수를 넘지 않는다`() {
        val candidates = (1..5).map { word(it.toLong()) }

        val planned = ExamPlanner.plan(candidates, examSize = 10, typedStage = { false }, pool = pool, random = Random(5))

        assertTrue(planned.all { it.type == QuestionType.MULTIPLE_CHOICE })
        assertEquals(5, planned.size)
    }

    @Test
    fun `주관식 단계 단어를 섞은 객관식은 전체 객관식 비율 70퍼센트를 넘지 않는다`() {
        val candidates = (1..20).map { word(it.toLong()) }

        // 전부 주관식 단계라서 섞기 확률이 높아도 객관식은 상한 7개까지만 나온다
        val planned = ExamPlanner.plan(candidates, examSize = 10, typedStage = { true }, pool = pool, random = Random(9))

        assertTrue(planned.count { it.type == QuestionType.MULTIPLE_CHOICE } <= 7)
        assertEquals(10, planned.size)
    }

    @Test
    fun `주관식 단계 단어는 주로 주관식으로 나온다`() {
        val candidates = (1..10).map { word(it.toLong()) }

        val planned = ExamPlanner.plan(candidates, examSize = 10, typedStage = { true }, pool = pool, random = Random(11))

        assertTrue(planned.count { it.type == QuestionType.TYPED } >= 5)
        assertTrue(planned.count { it.type == QuestionType.MULTIPLE_CHOICE } <= 7)
    }

    @Test
    fun `후보가 없으면 빈 목록이다`() {
        assertTrue(ExamPlanner.plan(emptyList(), 10, { false }, pool).isEmpty())
    }

    @Test
    fun `문제 수가 1 미만이면 예외를 던진다`() {
        assertFailsWith<IllegalArgumentException> {
            ExamPlanner.plan(listOf(word(1)), examSize = 0, typedStage = { false }, pool = pool)
        }
    }

    @Test
    fun `주관식은 앞뒤 공백과 대소문자만 무시하고 원형만 맞다`() {
        assertTrue(ExamGrader.isCorrect("  Go ", "go"))
        assertTrue(ExamGrader.isCorrect("GO", "go"))
        assertFalse(ExamGrader.isCorrect("went", "go"))
        assertFalse(ExamGrader.isCorrect("", "go"))
        assertFalse(ExamGrader.isCorrect("g o", "go"))
    }
}
