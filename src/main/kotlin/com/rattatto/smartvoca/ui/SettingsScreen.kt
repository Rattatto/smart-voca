// 2026-11-11 - Smart Voca - 설정 화면 (일일 개수, 난이도, 동사 비중 조절) (FR-040~042, FR-045)
// 파일 위치: src/main/kotlin/com/rattatto/smartvoca/ui/SettingsScreen.kt

package com.rattatto.smartvoca.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.rattatto.smartvoca.data.AppSettings
import com.rattatto.smartvoca.data.SettingsStore
import com.rattatto.smartvoca.data.csv.Level
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

/**
 * 설정을 바꾸면 바로 저장된다. 이미 만들어진 오늘의 목록은 바뀌지 않고, 다음 날 목록부터 반영된다 (FR-045).
 */
@Composable
fun SettingsScreen(settingsStore: SettingsStore, onClose: () -> Unit, modifier: Modifier = Modifier) {
    val settings by settingsStore.settings.collectAsState(initial = AppSettings.DEFAULT)
    val scope = rememberCoroutineScope()

    Column(
        modifier = modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(text = "설정")

        Text(text = "일일 노출 개수: ${settings.dailyCount}개")
        Slider(
            value = settings.dailyCount.toFloat(),
            onValueChange = { scope.launch { settingsStore.saveDailyCount(it.roundToInt()) } },
            valueRange = 5f..30f,
            steps = 24,
        )

        Text(text = "난이도")
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            LevelChip("전체", selected = settings.levelFilter == null) {
                scope.launch { settingsStore.saveLevelFilter(null) }
            }
            LevelChip("기초", selected = settings.levelFilter == Level.BASIC) {
                scope.launch { settingsStore.saveLevelFilter(Level.BASIC) }
            }
            LevelChip("중급", selected = settings.levelFilter == Level.INTERMEDIATE) {
                scope.launch { settingsStore.saveLevelFilter(Level.INTERMEDIATE) }
            }
            LevelChip("심화", selected = settings.levelFilter == Level.ADVANCED) {
                scope.launch { settingsStore.saveLevelFilter(Level.ADVANCED) }
            }
        }

        Text(text = "동사 비중: ${settings.verbRatio}%")
        Slider(
            value = settings.verbRatio.toFloat(),
            onValueChange = { scope.launch { settingsStore.saveVerbRatio(((it / 10).roundToInt()) * 10) } },
            valueRange = 30f..90f,
            steps = 5,
        )
        Text(text = "동사 비중은 저장만 되며, 품사 데이터가 준비되면 목록에 적용된다.")

        Button(onClick = onClose) {
            Text(text = "닫기")
        }
    }
}

@Composable
private fun LevelChip(label: String, selected: Boolean, onClick: () -> Unit) {
    FilterChip(selected = selected, onClick = onClick, label = { Text(text = label) })
}
