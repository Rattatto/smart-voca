// 2026-11-18 - Smart Voca - 일일 목록 구성 규칙 테스트 (재노출 상한, 신규 채우기, 순서) (FR-010, FR-014)
// 파일 위치: src/test/kotlin/com/rattatto/smartvoca/domain/DailyWordSelectorTest.kt

package com.rattatto.smartvoca.domain

import com.rattatto.smartvoca.data.csv.Level
import com.rattatto.smartvoca.data.db.WordEntity
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class DailyWordSelectorTest {

    private fun word(rank: Int, level: Level = Level.BASIC, text: String = "word$rank") = WordEntity(
        id = rank.toLong(),
        word = text,
        pos = null,
        meaningKo = "뜻$rank",
        exampleEn = null,
        exampleKo = null,
        freqRank = rank,
        level = level.csvValue,
        rating = null,
    )

    @Test
    fun `재노출은 설정 개수의 30 퍼센트까지만 먼저 넣고 나머지는 신규로 채운다`() {
        val reviews = (1..5).map { word(it) }
        val news = (101..120).map { word(it) }

        val picked = DailyWordSelector.compose(dailyCount = 10, reviewCandidates = reviews, newCandidates = news)

        assertEquals(3, picked.count { it.source == DailyWordSelector.SOURCE_REVIEW })
        assertEquals(7, picked.count { it.source == DailyWordSelector.SOURCE_NEW })
        assertEquals(listOf(1, 2, 3), picked.take(3).map { it.word.freqRank })
    }

    @Test
    fun `신규 단어가 부족하면 재노출 후보를 상한을 넘겨 채운다`() {
        val reviews = (1..6).map { word(it) }
        val news = listOf(word(101), word(102))

        val picked = DailyWordSelector.compose(dailyCount = 10, reviewCandidates = reviews, newCandidates = news)

        // 재노출 후보 6개 전부 들어가고, 신규는 2개뿐이라 목록은 8개가 된다
        assertEquals(6, picked.count { it.source == DailyWordSelector.SOURCE_REVIEW })
        assertEquals(2, picked.count { it.source == DailyWordSelector.SOURCE_NEW })
        assertEquals(8, picked.size)
    }

    @Test
    fun `재노출이 먼저 나오고 신규 단어가 뒤에 나온다`() {
        val picked = DailyWordSelector.compose(
            dailyCount = 10,
            reviewCandidates = listOf(word(1)),
            newCandidates = listOf(word(101), word(102)),
        )

        assertEquals(listOf(DailyWordSelector.SOURCE_REVIEW, DailyWordSelector.SOURCE_NEW, DailyWordSelector.SOURCE_NEW),
            picked.map { it.source })
    }

    @Test
    fun `후보가 모두 부족하면 있는 만큼만 돌려준다`() {
        val picked = DailyWordSelector.compose(dailyCount = 10, reviewCandidates = emptyList(), newCandidates = listOf(word(1)))

        assertEquals(1, picked.size)
    }

    @Test
    fun `개수가 1 미만이면 예외를 던진다`() {
        assertFailsWith<IllegalArgumentException> {
            DailyWordSelector.compose(dailyCount = 0, reviewCandidates = emptyList(), newCandidates = emptyList())
        }
    }
}
