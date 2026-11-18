// 2026-11-18 - Smart Voca - 오늘의 일일 단어 목록, 학습 완료 버튼, 기능어 TIP 화면 (FR-010, FR-013, FR-020)
// 파일 위치: src/main/kotlin/com/rattatto/smartvoca/ui/TodayWordScreen.kt

package com.rattatto.smartvoca.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.rattatto.smartvoca.data.DailyItem
import com.rattatto.smartvoca.data.DailyListRepository
import com.rattatto.smartvoca.data.SettingsStore
import com.rattatto.smartvoca.data.StudyRepository
import com.rattatto.smartvoca.domain.FunctionWordTip
import com.rattatto.smartvoca.domain.FunctionWords
import com.rattatto.smartvoca.domain.DailyWordSelector
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate

/**
 * 오늘의 단어 목록 (왼쪽), 기능어 TIP (오른쪽). 각 단어에 학습 완료 버튼이 있다.
 * 목록이 없으면 현재 설정으로 만들고, 있으면 저장된 목록을 쓴다. 설정을 바꿔도 오늘 목록은 바뀌지 않는다 (FR-045).
 * [refreshKey] 가 바뀌면 목록을 다시 읽는다 (가져오기 성공 등).
 */
@Composable
fun TodayWordScreen(
    repository: DailyListRepository,
    studyRepository: StudyRepository,
    settingsStore: SettingsStore,
    functionWords: FunctionWords,
    refreshKey: Int = 0,
    modifier: Modifier = Modifier,
) {
    val today = remember { LocalDate.now() }
    var items by remember { mutableStateOf<List<DailyItem>?>(null) }
    var studiedIds by remember { mutableStateOf<Set<Long>>(emptySet()) }
    var errorText by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(today, refreshKey) {
        try {
            val current = settingsStore.settings.first()
            items = repository.getOrCreate(today, current.dailyCount, current.levelFilter)
            studiedIds = studyRepository.studiedIdsOn(today)
        } catch (error: IllegalStateException) {
            errorText = "목록을 만들지 못했습니다: ${error.message}"
        }
    }

    Column(modifier = modifier.fillMaxWidth().padding(16.dp)) {
        Text(text = "오늘의 단어 ($today)")
        Row(modifier = Modifier.fillMaxWidth().weight(1f)) {
            WordList(
                errorText = errorText,
                items = items,
                studiedIds = studiedIds,
                onStudied = { wordId ->
                    scope.launch {
                        studyRepository.markStudied(wordId, today, Instant.now())
                        studiedIds = studiedIds + wordId
                    }
                },
                modifier = Modifier.weight(0.65f),
            )
            DailyTipPanel(
                tip = functionWords.dailyTip(today),
                modifier = Modifier.weight(0.35f).padding(start = 8.dp),
            )
        }
    }
}

@Composable
private fun WordList(
    errorText: String?,
    items: List<DailyItem>?,
    studiedIds: Set<Long>,
    onStudied: (Long) -> Unit,
    modifier: Modifier,
) {
    val currentItems = items
    when {
        errorText != null -> Text(text = errorText, modifier = modifier)
        currentItems == null -> Text(text = "불러오는 중", modifier = modifier)
        currentItems.isEmpty() -> Text(text = "가져온 단어가 없습니다. CSV 를 먼저 가져오세요", modifier = modifier)
        else -> LazyColumn(modifier = modifier) {
            itemsIndexed(currentItems) { index, item ->
                val word = item.word
                val studied = word.id in studiedIds
                Column(modifier = Modifier.padding(vertical = 6.dp)) {
                    val tag = if (item.source == DailyWordSelector.SOURCE_REVIEW) " (재노출)" else ""
                    Text(text = "${index + 1}. ${word.word}  ${word.meaningKo}$tag")
                    word.exampleEn?.let { Text(text = it, style = MaterialTheme.typography.bodySmall) }
                    word.exampleKo?.let { Text(text = it, style = MaterialTheme.typography.bodySmall) }
                    if (studied) {
                        Text(text = "오늘 완료됨", style = MaterialTheme.typography.bodySmall)
                    } else {
                        Button(onClick = { onStudied(word.id) }) {
                            Text(text = "학습 완료")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DailyTipPanel(tip: FunctionWordTip?, modifier: Modifier) {
    Column(modifier = modifier) {
        Text(text = "오늘의 TIP", style = MaterialTheme.typography.titleSmall)
        if (tip == null) {
            Text(text = "TIP 이 없습니다", style = MaterialTheme.typography.bodySmall)
        } else {
            Text(text = tip.word, style = MaterialTheme.typography.bodyLarge)
            Text(text = tip.category, style = MaterialTheme.typography.bodySmall)
            Text(text = tip.tipKo, style = MaterialTheme.typography.bodySmall)
        }
    }
}
