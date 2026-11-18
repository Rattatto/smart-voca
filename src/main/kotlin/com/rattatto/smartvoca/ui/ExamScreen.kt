// 2026-11-18 - Smart Voca - 시험 화면 (뜻을 보고 영단어 입력, 채점, 결과) (FR-031, FR-033, FR-035, FR-036, FR-038)
// 파일 위치: src/main/kotlin/com/rattatto/smartvoca/ui/ExamScreen.kt

package com.rattatto.smartvoca.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
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
import androidx.compose.ui.unit.dp
import com.rattatto.smartvoca.data.DailyListRepository
import com.rattatto.smartvoca.data.ExamRepository
import com.rattatto.smartvoca.data.SettingsStore
import com.rattatto.smartvoca.data.db.WordEntity
import com.rattatto.smartvoca.domain.ExamGrader
import com.rattatto.smartvoca.domain.ExamPlanner
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate

/**
 * 시험 한 회차. 오늘 목록과 오늘 재노출 대상에서 설정 문제 수만큼 뽑아 뜻을 보여 주고 영단어를 입력받는다.
 * 채점 결과는 시험 이력에만 저장하고 재노출 일정은 바꾸지 않는다 (FR-037).
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
    var questions by remember { mutableStateOf<List<WordEntity>?>(null) }
    var index by remember { mutableIntStateOf(0) }
    var correctCount by remember { mutableIntStateOf(0) }
    var input by remember { mutableStateOf("") }
    var feedback by remember { mutableStateOf<String?>(null) }
    var errorText by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(today) {
        try {
            val settings = settingsStore.settings.first()
            val todayWords = dailyListRepository
                .getOrCreate(today, settings.dailyCount, settings.levelFilter)
                .map { it.word }
            val reviewWords = examRepository.reviewWordsDue(today)
            questions = ExamPlanner.plan(todayWords, reviewWords, settings.examSize)
        } catch (error: IllegalStateException) {
            errorText = "시험을 만들지 못했습니다: ${error.message}"
        }
    }

    Column(
        modifier = modifier.fillMaxWidth().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(text = "시험")
        val currentQuestions = questions
        when {
            errorText != null -> Text(text = errorText.orEmpty())
            currentQuestions == null -> Text(text = "불러오는 중")
            currentQuestions.isEmpty() -> Text(text = "오늘 시험할 단어가 없습니다")
            index >= currentQuestions.size -> {
                Text(text = "결과: $correctCount / ${currentQuestions.size}")
            }
            else -> {
                val question = currentQuestions[index]
                Text(text = "문제 ${index + 1} / ${currentQuestions.size}")
                Text(text = "뜻: ${question.meaningKo}")
                OutlinedTextField(
                    value = input,
                    onValueChange = { input = it },
                    label = { Text(text = "영단어") },
                    enabled = feedback == null,
                    modifier = Modifier.fillMaxWidth(),
                )
                val currentFeedback = feedback
                if (currentFeedback == null) {
                    Button(onClick = {
                        val isCorrect = ExamGrader.isCorrect(input, question.word)
                        if (isCorrect) correctCount++
                        feedback = if (isCorrect) "정답입니다" else "오답. 정답: ${question.word}"
                        scope.launch {
                            examRepository.recordAttempt(
                                wordId = question.id,
                                date = today,
                                shownMeaning = question.meaningKo,
                                userInput = input,
                                isCorrect = isCorrect,
                                now = Instant.now(),
                            )
                        }
                    }) {
                        Text(text = "확인")
                    }
                } else {
                    Text(text = currentFeedback)
                    Button(onClick = {
                        index++
                        input = ""
                        feedback = null
                    }) {
                        Text(text = if (index + 1 >= currentQuestions.size) "결과 보기" else "다음")
                    }
                }
            }
        }
        Button(onClick = onClose) {
            Text(text = "닫기")
        }
    }
}
