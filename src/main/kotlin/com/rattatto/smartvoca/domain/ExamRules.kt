// 2026-11-18 - Smart Voca - 시험 출제, 객관식 보기, 채점 규칙 (FR-031, FR-033, FR-035, FR-038, ADR-012)
// 파일 위치: src/main/kotlin/com/rattatto/smartvoca/domain/ExamRules.kt

package com.rattatto.smartvoca.domain

import com.rattatto.smartvoca.data.db.WordEntity
import kotlin.random.Random

/** 문제 형식. 객관식은 영단어를 보고 뜻을 고르고, 주관식은 뜻을 보고 영단어를 입력한다. */
enum class QuestionType(val dbValue: String) {
    MULTIPLE_CHOICE("mc"),
    TYPED("typed");

    companion object {
        fun fromDb(value: String): QuestionType = entries.firstOrNull { it.dbValue == value } ?: TYPED
    }
}

/** 출제된 문제 한 개. 객관식이면 [choices] 에 뜻 4개가 섞여 있다. */
data class ExamQuestion(
    val word: WordEntity,
    val type: QuestionType,
    val choices: List<String>? = null,
)

/** 객관식 보기를 만든다. 정답 뜻 1개와 같은 난이도의 다른 단어 뜻 3개를 쓴다 (D5, D6). */
object ChoiceBuilder {

    const val CHOICE_COUNT = 4

    /** 보기를 만들 수 없으면 (같은 난이도 뜻이 부족하면) null. */
    fun build(answer: WordEntity, pool: List<WordEntity>, random: Random = Random.Default): List<String>? {
        val distractors = pool
            .asSequence()
            .filter { it.id != answer.id && it.level == answer.level && it.meaningKo != answer.meaningKo }
            .map { it.meaningKo }
            .distinct()
            .toList()
            .shuffled(random)
            .take(CHOICE_COUNT - 1)
        if (distractors.size < CHOICE_COUNT - 1) return null
        return (distractors + answer.meaningKo).shuffled(random)
    }
}

/**
 * 출제 구성 (P4). 후보는 오늘 목록과 오늘 재노출 대상이다.
 * - 객관식 단계 단어는 항상 객관식이다.
 * - 주관식 단계 단어는 20% 를 객관식으로 섞는다 (D8). 이때 전체 객관식 비율이 70% 를 넘으면 섞지 않는다 (D7).
 * - 보기를 만들 수 없는 문제는 주관식으로 낸다.
 */
object ExamPlanner {

    const val MULTIPLE_CHOICE_CAP_PERCENT = 70
    const val MIXED_MULTIPLE_CHOICE_PERCENT = 20

    /**
     * [candidates] 는 중복 없는 후보 단어다. [typedStage] 는 단어가 주관식 단계인지 알려 준다.
     * [pool] 은 보기 뜻을 뽑을 전체 단어다.
     */
    fun plan(
        candidates: List<WordEntity>,
        examSize: Int,
        typedStage: (WordEntity) -> Boolean,
        pool: List<WordEntity>,
        random: Random = Random.Default,
    ): List<ExamQuestion> {
        require(examSize > 0) { "examSize 는 1 이상이어야 한다: $examSize" }
        val total = minOf(examSize, candidates.size)
        val multipleChoiceCap = examSize * MULTIPLE_CHOICE_CAP_PERCENT / 100

        val questions = mutableListOf<ExamQuestion>()
        var multipleChoiceCount = 0
        for (word in candidates.shuffled(random).take(total)) {
            val isTypedStage = typedStage(word)
            val wantsMultipleChoice = !isTypedStage || random.nextInt(100) < MIXED_MULTIPLE_CHOICE_PERCENT
            val multipleChoiceAllowed = !isTypedStage || multipleChoiceCount < multipleChoiceCap
            val choices = if (wantsMultipleChoice && multipleChoiceAllowed) ChoiceBuilder.build(word, pool, random) else null
            if (choices != null) {
                multipleChoiceCount++
                questions += ExamQuestion(word, QuestionType.MULTIPLE_CHOICE, choices)
            } else {
                questions += ExamQuestion(word, QuestionType.TYPED)
            }
        }
        return questions
    }
}

/** 답을 채점한다 (P5). 주관식은 앞뒤 공백과 대소문자만 무시하고 원형 그대로 비교한다 (FR-033, D9). */
object ExamGrader {

    fun isCorrect(userInput: String, answer: String): Boolean =
        userInput.trim().equals(answer.trim(), ignoreCase = true)

    /** 객관식은 고른 뜻이 정답 뜻과 같은지 본다. */
    fun isMultipleChoiceCorrect(selectedMeaning: String, answer: WordEntity): Boolean =
        selectedMeaning == answer.meaningKo
}
