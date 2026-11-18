// 2026-11-18 - Smart Voca - 오늘의 일일 단어 카드 목록, 학습 완료 버튼, 기능어 TIP 카드 (FR-010, FR-013, FR-020)
// 파일 위치: src/main/kotlin/com/rattatto/smartvoca/ui/TodayWordScreen.kt

package com.rattatto.smartvoca.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
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
 * 오늘의 단어 목록 (왼쪽, 카드 형식), 기능어 TIP (오른쪽 카드). 각 카드에 학습 완료 버튼이 있다.
 * 목록이 없으면 현재 설정으로 만들고, 있으면 저장된 목록을 쓴다. 설정을 바꿔도 오늘 목록은 바뀌지 않는다 (FR-045).
 * [refreshKey] 가 바뀌면 목록을 다시 읽는다 (가져오기 성공 등).
 */
@Composable
fun TodayWordScreen(
    repository: DailyListRepository,
    studyRepository: StudyRepository,
    settingsStore: SettingsStore,
    functionWords: FunctionWords,
    today: LocalDate,
    refreshKey: Int = 0,
    modifier: Modifier = Modifier,
) {
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
        Text(
            text = "오늘의 단어 ($today)",
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onBackground,
        )
        Spacer(modifier = Modifier.height(8.dp))
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
        else -> LazyColumn(
            modifier = modifier,
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            itemsIndexed(currentItems) { index, item ->
                WordCard(
                    number = index + 1,
                    item = item,
                    studied = item.word.id in studiedIds,
                    onStudied = { onStudied(item.word.id) },
                )
            }
        }
    }
}

/** 단어 한 개를 카드로 보여준다. 단어와 뜻이 위, 예문이 가운데, 완료 버튼이 아래에 온다. */
@Composable
private fun WordCard(number: Int, item: DailyItem, studied: Boolean, onStudied: () -> Unit) {
    val word = item.word
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "$number",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                )
                Text(
                    text = word.word,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(start = 8.dp),
                )
                if (item.source == DailyWordSelector.SOURCE_REVIEW) {
                    Surface(
                        modifier = Modifier.padding(start = 8.dp),
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.primaryContainer,
                    ) {
                        Text(
                            text = "재노출",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                        )
                    }
                }
            }
            Text(
                text = word.meaningKo,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(top = 4.dp),
            )
            word.exampleEn?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 8.dp),
                )
            }
            word.exampleKo?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Spacer(modifier = Modifier.height(10.dp))
            if (studied) {
                Text(
                    text = "오늘 완료됨",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
            } else {
                Button(
                    onClick = onStudied,
                    shape = RoundedCornerShape(24.dp),
                    modifier = Modifier.fillMaxWidth().height(44.dp),
                ) {
                    Text(text = "학습 완료")
                }
            }
        }
    }
}

/** 기능어 TIP 을 연한 주황 카드에 보여준다. */
@Composable
private fun DailyTipPanel(tip: FunctionWordTip?, modifier: Modifier) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        modifier = modifier,
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(
                text = "오늘의 TIP",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary,
            )
            if (tip == null) {
                Text(
                    text = "TIP 이 없습니다",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 8.dp),
                )
            } else {
                Text(
                    text = tip.word,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(top = 8.dp),
                )
                Text(
                    text = tip.category,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 2.dp),
                )
                Text(
                    text = tip.tipKo,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(top = 8.dp),
                )
            }
        }
    }
}
