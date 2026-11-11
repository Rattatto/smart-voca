// 2026-11-11 - Smart Voca - 일일 단어 선정 규칙 테스트 (FR-010, FR-011)
// 파일 위치: src/test/kotlin/com/rattatto/smartvoca/domain/DailyWordSelectorTest.kt

package com.rattatto.smartvoca.domain

import com.rattatto.smartvoca.data.csv.Level
import com.rattatto.smartvoca.data.db.WordEntity
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class DailyWordSelectorTest {

    private fun word(rank: Int, level: Level) = WordEntity(
        id = rank.toLong(),
        word = "word$rank",
        pos = null,
        meaningKo = "뜻$rank",
        exampleEn = null,
        exampleKo = null,
        freqRank = rank,
        level = level.csvValue,
        rating = null,
    )

    @Test
    fun `설정 개수만큼 빈도 순위가 높은 단어부터 고른다`() {
        val words = (1..30).map { word(it, Level.BASIC) }.shuffled()

        val selected = DailyWordSelector.select(words, dailyCount = 10, levelFilter = null)

        assertEquals((1..10).toList(), selected.map { it.freqRank })
    }

    @Test
    fun `난이도 필터를 적용한다`() {
        val words = listOf(
            word(1, Level.BASIC),
            word(2, Level.INTERMEDIATE),
            word(3, Level.ADVANCED),
            word(4, Level.INTERMEDIATE),
        )

        val selected = DailyWordSelector.select(words, dailyCount = 10, levelFilter = Level.INTERMEDIATE)

        assertEquals(listOf(2, 4), selected.map { it.freqRank })
    }

    @Test
    fun `후보가 개수보다 적으면 있는 만큼만 돌려준다`() {
        val words = listOf(word(1, Level.BASIC), word(2, Level.BASIC))

        val selected = DailyWordSelector.select(words, dailyCount = 10, levelFilter = null)

        assertEquals(listOf(1, 2), selected.map { it.freqRank })
    }

    @Test
    fun `같은 입력이면 같은 결과를 낸다`() {
        val words = (1..50).map { word(it, Level.values()[it % 3]) }

        val first = DailyWordSelector.select(words, dailyCount = 10, levelFilter = Level.BASIC)
        val second = DailyWordSelector.select(words.reversed(), dailyCount = 10, levelFilter = Level.BASIC)

        assertEquals(first, second)
    }

    @Test
    fun `개수가 1 미만이면 예외를 던진다`() {
        assertFailsWith<IllegalArgumentException> {
            DailyWordSelector.select(emptyList(), dailyCount = 0, levelFilter = null)
        }
    }
}
