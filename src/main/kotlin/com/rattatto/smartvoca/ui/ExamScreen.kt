// 2026-11-18 - Smart Voca - 시험 화면 (객관식 우선, 승격 후 주관식, 강등 안내) (FR-031, FR-033, FR-035, FR-036, FR-038)
// 파일 위치: src/main/kotlin/com/rattatto/smartvoca/ui/ExamScreen.kt

package com.rattatto.smartvoca.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.rattatto.smartvoca.data.DailyListRepository
import com.rattatto.smartvoca.data.ExamRepository
import com.rattatto.smartvoca.data.SettingsStore
import com.rattatto.smartvoca.domain.ExamGrader
import com.rattatto.smartvoca.domain.ExamQuestion
import com.rattatto.smartvoca.domain.QuestionType
import com.rattatto.smartvoca.domain.StageState
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate

private val CorrectColor = Color(0xFF2E7D32)
private val WrongColor = Color(0xFFC62828)

/**
 * 시험 한 회차. 객관식 (영단어 → 뜻 고르기)과 주관식 (뜻 → 영단어 입력)을 섞어 낸다.
 * 답은 시험 이력에 저장되고, 단계가 바뀌면 (승격, 강등) 화면에 알려 준다. 재노출 일정은 바꾸지 않는다 (FR-037).
 */
@Composable
fun ExamScreen(
    dailyListRepository: DailyListRepository,
    examRepository: ExamRepository,
    settingsStore: SettingsStore,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val today = remember { LocalDate.now() }
    val scope = rememberCoroutineScope()
    var questions by remember { mutableStateOf<List<ExamQuestion>?>(null) }
    var index by remember { mutableIntStateOf(0) }
    var correctCount by remember { mutableIntStateOf(0) }
    var input by remember { mutableStateOf("") }
    var feedback by remember { mutableStateOf<String?>(null) }
    var feedbackIsCorrect by remember { mutableStateOf(false) }
    var stageNote by remember { mutableStateOf<String?>(null) }
    var errorText by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(today) {
        try {
            val settings = settingsStore.settings.first()
            val todayWords = dailyListRepository
                .getOrCreate(today, settings.dailyCount, settings.levelFilter)
                .map { it.word }
            questions = examRepository.buildQuestions(today, settings.examSize, todayWords)
        } catch (error: IllegalStateException) {
            errorText = "시험을 만들지 못했습니다: ${error.message}"
        }
    }

    /** 답을 채점하고 기록한다. 단계가 바뀌면 안내 문구를 만든다. */
    fun submit(question: ExamQuestion, isCorrect: Boolean, userInput: String) {
        if (isCorrect) correctCount++
        feedbackIsCorrect = isCorrect
        feedback = if (isCorrect) "정답입니다" else "오답. 정답: ${question.word.word} (${question.word.meaningKo})"
        scope.launch {
            val before = examRepository.stageOf(question.word.id)
            val after = examRepository.recordAttempt(
                wordId = question.word.id,
                date = today,
                shownMeaning = question.word.meaningKo,
                userInput = userInput,
                isCorrect = isCorrect,
                type = question.type,
                now = Instant.now(),
            )
            stageNote = stageChangeNote(before, after)
        }
    }

    Column(
        modifier = modifier.fillMaxWidth().padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Text(text = "시험", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.primary)
        val currentQuestions = questions
        when {
            errorText != null -> Text(text = errorText.orEmpty())
            currentQuestions == null -> Text(text = "불러오는 중")
            currentQuestions.isEmpty() -> Text(text = "오늘 시험할 단어가 없습니다")
            index >= currentQuestions.size -> Text(
                text = "결과: $correctCount / ${currentQuestions.size}",
                style = MaterialTheme.typography.headlineSmall,
            )
            else -> {
                val question = currentQuestions[index]
                Text(text = "문제 ${index + 1} / ${currentQuestions.size}")
                if (question.type == QuestionType.MULTIPLE_CHOICE) {
                    MultipleChoiceBody(
                        question = question,
                        feedback = feedback,
                        feedbackIsCorrect = feedbackIsCorrect,
                        onSelect = { choice ->
                            submit(question, ExamGrader.isMultipleChoiceCorrect(choice, question.word), choice)
                        },
                    )
                } else {
                    TypedBody(
                        question = question,
                        input = input,
                        onInputChange = { input = it },
                        feedback = feedback,
                        feedbackIsCorrect = feedbackIsCorrect,
                        onSubmit = { submit(question, ExamGrader.isCorrect(input, question.word.word), input) },
                    )
                }
                stageNote?.let { Text(text = it, color = MaterialTheme.colorScheme.primary) }
                if (feedback != null) {
                    Button(onClick = {
                        index++
                        input = ""
                        feedback = null
                        stageNote = null
                    }) {
                        Text(text = if (index + 1 >= currentQuestions.size) "결과 보기" else "다음")
                    }
                }
            }
        }
        OutlinedButton(onClick = onClose) {
            Text(text = "닫기")
        }
    }
}

@Composable
private fun MultipleChoiceBody(
    question: ExamQuestion,
    feedback: String?,
    feedbackIsCorrect: Boolean,
    onSelect: (String) -> Unit,
) {
    Text(text = question.word.word, style = MaterialTheme.typography.headlineMedium)
    Text(text = "알맞은 뜻을 고르세요", style = MaterialTheme.typography.bodySmall)
    question.choices.orEmpty().forEach { choice ->
        OutlinedButton(
            onClick = { if (feedback == null) onSelect(choice) },
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(text = choice)
        }
    }
    feedback?.let { FeedbackText(it, feedbackIsCorrect) }
}

@Composable
private fun TypedBody(
    question: ExamQuestion,
    input: String,
    onInputChange: (String) -> Unit,
    feedback: String?,
    feedbackIsCorrect: Boolean,
    onSubmit: () -> Unit,
) {
    Text(text = "뜻: ${question.word.meaningKo}", style = MaterialTheme.typography.headlineSmall)
    OutlinedTextField(
        value = input,
        onValueChange = onInputChange,
        label = { Text(text = "영단어") },
        enabled = feedback == null,
        modifier = Modifier.fillMaxWidth(),
    )
    if (feedback == null) {
        Button(onClick = onSubmit) {
            Text(text = "확인")
        }
    } else {
        FeedbackText(feedback, feedbackIsCorrect)
    }
}

@Composable
private fun FeedbackText(text: String, isCorrect: Boolean) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleMedium,
        color = if (isCorrect) CorrectColor else WrongColor,
    )
}

/** 단계가 바뀌었을 때만 안내한다. 객관식에서 주관식으로 올라가면 승격, 주관식에서 내려오면 강등. */
private fun stageChangeNote(before: StageState, after: StageState): String? = when {
    before.type == QuestionType.MULTIPLE_CHOICE && after.type == QuestionType.TYPED ->
        "승격: 이 단어는 이제 주관식으로 나옵니다"
    before.type == QuestionType.TYPED && after.type == QuestionType.MULTIPLE_CHOICE ->
        "강등: 이 단어는 다시 객관식으로 나옵니다"
    else -> null
}
