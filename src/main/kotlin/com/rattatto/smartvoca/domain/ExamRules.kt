// 2026-11-18 - Smart Voca - 시험 출제와 채점 규칙 (FR-031, FR-033, FR-035, FR-037, 프로세스 설계 P4, P5)
// 파일 위치: src/main/kotlin/com/rattatto/smartvoca/domain/ExamRules.kt

package com.rattatto.smartvoca.domain

import com.rattatto.smartvoca.data.db.WordEntity
import kotlin.random.Random

/** 출제 후보에서 시험 문제를 고른다 (P4). 후보는 오늘 목록과 오늘 재노출 대상이다. */
object ExamPlanner {

    /**
     * 후보를 합쳐 중복을 없앤 뒤, [examSize] 개를 무작위로 뽑아 섞는다.
     * 후보가 [examSize] 보다 적으면 전부 낸다. 후보가 없으면 빈 리스트다.
     */
    fun plan(
        todayWords: List<WordEntity>,
        reviewWords: List<WordEntity>,
        examSize: Int,
        random: Random = Random.Default,
    ): List<WordEntity> {
        require(examSize > 0) { "examSize 는 1 이상이어야 한다: $examSize" }
        val candidates = (todayWords + reviewWords).distinctBy { it.id }
        return candidates.shuffled(random).take(examSize)
    }
}

/** 답을 채점한다 (P5). 앞뒤 공백과 대소문자만 무시하고 나머지는 그대로 비교한다 (FR-033). */
object ExamGrader {

    fun isCorrect(userInput: String, answer: String): Boolean =
        userInput.trim().equals(answer.trim(), ignoreCase = true)
}
