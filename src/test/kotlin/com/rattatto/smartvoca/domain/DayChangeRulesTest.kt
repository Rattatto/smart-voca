// 2026-11-18 - Smart Voca - 날짜 변경 감지 규칙 테스트 (처음 실행, 같은 날, 날짜 바뀜)
// 파일 위치: src/test/kotlin/com/rattatto/smartvoca/domain/DayChangeRulesTest.kt

package com.rattatto.smartvoca.domain

import java.time.LocalDate
import org.junit.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class DayChangeRulesTest {

    private val day1 = LocalDate.of(2026, 11, 18)

    @Test
    fun `처음 실행이면 알리지 않는다`() {
        assertFalse(DayChangeRules.shouldNotify(lastSeen = null, today = day1))
    }

    @Test
    fun `같은 날이면 알리지 않는다`() {
        assertFalse(DayChangeRules.shouldNotify(lastSeen = day1, today = day1))
    }

    @Test
    fun `마지막으로 본 날짜와 다르면 알린다`() {
        assertTrue(DayChangeRules.shouldNotify(lastSeen = day1, today = day1.plusDays(1)))
    }
}
