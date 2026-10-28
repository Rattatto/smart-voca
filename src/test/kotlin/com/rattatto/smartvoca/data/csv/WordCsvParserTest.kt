// 2026-10-28 - Smart Voca - 단어 CSV 파서의 헤더 검증, 행 검증, 중복 처리 테스트 (FR-001~003, 005)
// 파일 위치: src/test/kotlin/com/rattatto/smartvoca/data/csv/WordCsvParserTest.kt

package com.rattatto.smartvoca.data.csv

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull

class WordCsvParserTest {

    private val parser = WordCsvParser()

    @Test
    fun `필수 열이 없으면 누락된 열 이름과 함께 중단한다`() {
        val csv = "word,meaning_ko,level\nbe,있다,basic\n"

        val error = assertFailsWith<MissingColumnsException> { parser.parse(csv) }

        assertEquals(listOf("freq_rank"), error.missingColumns)
    }

    @Test
    fun `선택 열이 없어도 적재되고 선택 값은 null 이다`() {
        val csv = "word,meaning_ko,freq_rank,level\nbe,있다,1,basic\n"

        val result = parser.parse(csv)

        assertEquals(1, result.words.size)
        val item = result.words.single()
        assertEquals("be", item.word)
        assertEquals(Level.BASIC, item.level)
        assertNull(item.pos)
        assertNull(item.rating)
        assertEquals(emptyList(), result.skipped)
    }

    @Test
    fun `freq_rank 가 빈 행은 건너뛰고 줄 번호와 사유를 남긴다`() {
        val csv = "word,meaning_ko,freq_rank,level\nbe,있다,1,basic\nhave,가지다,,basic\n"

        val result = parser.parse(csv)

        assertEquals(listOf("be"), result.words.map { it.word })
        assertEquals(listOf(SkippedRow(3, "freq_rank 누락")), result.skipped)
    }

    @Test
    fun `따옴표 안의 쉼표는 한 필드로 읽는다`() {
        val csv = "word,meaning_ko,freq_rank,level\nlevel,\"정도[수준], 수준[단계]\",861,basic\n"

        val item = parser.parse(csv).words.single()

        assertEquals("정도[수준], 수준[단계]", item.meaningKo)
    }

    @Test
    fun `BOM 이 붙은 파일도 첫 열 이름을 정상으로 인식한다`() {
        val csv = "﻿word,meaning_ko,freq_rank,level\nbe,있다,1,basic\n"

        val result = parser.parse(csv)

        assertEquals(1, result.words.size)
        assertEquals(emptyList(), result.skipped)
    }

    @Test
    fun `같은 빈도 순위가 두 번 나오면 뒤의 행을 건너뛴다`() {
        val csv = "word,meaning_ko,freq_rank,level\nbe,있다,1,basic\nam,있다,1,basic\n"

        val result = parser.parse(csv)

        assertEquals(listOf("be"), result.words.map { it.word })
        assertEquals(listOf(SkippedRow(3, "freq_rank 1 중복 (2행과 같음)")), result.skipped)
    }

    @Test
    fun `허용되지 않은 level 값과 영문자가 아닌 word 는 건너뛴다`() {
        val csv = "word,meaning_ko,freq_rank,level\n" +
            "go,가다,2,easy\n" +
            "한글,단어,3,basic\n" +
            "take off,이륙하다,4,basic\n"

        val result = parser.parse(csv)

        assertEquals(listOf("take off"), result.words.map { it.word })
        assertEquals(2, result.skipped.size)
        assertEquals(2, result.skipped[0].lineNumber)
        assertEquals(3, result.skipped[1].lineNumber)
    }

    @Test
    fun `샘플 형식의 행을 모든 선택 열과 함께 읽는다`() {
        val csv = "word,pos,meaning_ko,example_en,example_ko,freq_rank,level,rating\n" +
            "be,,\"있다, 있다, 참석\",,,1,basic,★★★★\n"

        val item = parser.parse(csv).words.single()

        assertNull(item.pos)
        assertEquals("있다, 있다, 참석", item.meaningKo)
        assertEquals("★★★★", item.rating)
    }
}
