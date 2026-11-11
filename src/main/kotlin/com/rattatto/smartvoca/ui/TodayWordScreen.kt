// 2026-11-11 - Smart Voca - 오늘의 일일 단어 목록 화면 (FR-010, FR-013). 설정 값은 목록을 만들 때 읽는다
// 파일 위치: src/main/kotlin/com/rattatto/smartvoca/ui/TodayWordScreen.kt

package com.rattatto.smartvoca.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.rattatto.smartvoca.data.DailyListRepository
import com.rattatto.smartvoca.data.SettingsStore
import com.rattatto.smartvoca.data.db.WordEntity
import kotlinx.coroutines.flow.first
import java.time.LocalDate

/**
 * 오늘의 일일 단어 목록을 보여 준다. 목록이 없으면 현재 설정으로 만들고, 있으면 저장된 목록을 그대로 쓴다.
 * 설정을 바꿔도 이미 만들어진 오늘의 목록은 바뀌지 않는다 (FR-045).
 */
@Composable
fun TodayWordScreen(
    repository: DailyListRepository,
    settingsStore: SettingsStore,
    modifier: Modifier = Modifier,
) {
    val today = remember { LocalDate.now() }
    var words by remember { mutableStateOf<List<WordEntity>?>(null) }
    var errorText by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(today) {
        try {
            val current = settingsStore.settings.first()
            words = repository.getOrCreate(today, current.dailyCount, current.levelFilter)
        } catch (error: IllegalStateException) {
            errorText = "목록을 만들지 못했습니다: ${error.message}"
        }
    }

    Column(modifier = modifier.fillMaxWidth().padding(16.dp)) {
        Text(text = "오늘의 단어 ($today)")
        val currentWords = words
        when {
            errorText != null -> Text(text = errorText.orEmpty())
            currentWords == null -> Text(text = "불러오는 중")
            currentWords.isEmpty() -> Text(text = "가져온 단어가 없습니다. CSV 를 먼저 가져오세요")
            else -> LazyColumn {
                itemsIndexed(currentWords) { index, word ->
                    Text(text = "${index + 1}. ${word.word}  ${word.meaningKo}")
                }
            }
        }
    }
}
