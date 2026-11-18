// 2026-10-28 - Smart Voca - 앱 진입 화면 (임시 시작 화면, 기능 화면은 이후 추가)
// 파일 위치: src/main/kotlin/com/rattatto/smartvoca/MainActivity.kt

package com.rattatto.smartvoca

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.compose.material3.Button
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import kotlinx.coroutines.launch
import java.time.LocalDate
import com.rattatto.smartvoca.data.DailyListRepository
import com.rattatto.smartvoca.data.ExamRepository
import com.rattatto.smartvoca.data.SettingsStore
import com.rattatto.smartvoca.data.StudyRepository
import com.rattatto.smartvoca.data.WordRepository
import com.rattatto.smartvoca.data.db.SmartVocaDatabase
import com.rattatto.smartvoca.domain.DayChangeRules
import com.rattatto.smartvoca.domain.FunctionWords
import com.rattatto.smartvoca.ui.ExamScreen
import com.rattatto.smartvoca.ui.SettingsScreen
import com.rattatto.smartvoca.ui.SmartVocaColors
import com.rattatto.smartvoca.ui.TodayWordScreen

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // 앱 전체에서 DB 연결을 하나만 쓰도록 여기서 한 번만 만들고 저장소들이 함께 쓴다
        val database = SmartVocaDatabase.create(applicationContext)
        val functionWords = loadFunctionWords()
        val wordRepository = WordRepository(database)
        val dailyListRepository = DailyListRepository(database, excludedWords = functionWords.words)
        val studyRepository = StudyRepository(database)
        val examRepository = ExamRepository(database)
        val settingsStore = SettingsStore.create(applicationContext)
        setContent {
            MaterialTheme(colorScheme = SmartVocaColors) {
                StartScreen(
                    wordRepository,
                    dailyListRepository,
                    studyRepository,
                    examRepository,
                    settingsStore,
                    functionWords,
                )
            }
        }
    }

    /** 기능어 목록은 앱에 들어 있는 파일이라 시작할 때 한 번 읽는다. */
    private fun loadFunctionWords(): FunctionWords {
        val csvText = assets.open(FunctionWords.ASSET_FILE_NAME).bufferedReader(Charsets.UTF_8).use { it.readText() }
        return FunctionWords.parse(csvText)
    }
}

@Composable
fun StartScreen(
    wordRepository: WordRepository,
    dailyListRepository: DailyListRepository,
    studyRepository: StudyRepository,
    examRepository: ExamRepository,
    settingsStore: SettingsStore,
    functionWords: FunctionWords,
) {
    var showSettings by remember { mutableStateOf(false) }
    var showExam by remember { mutableStateOf(false) }
    // 가져오기가 성공할 때마다 올라가서 오늘 목록 화면을 다시 읽게 한다
    var importCount by remember { mutableStateOf(0) }
    // 화면이 기준으로 삼는 날짜. 앱을 켜거나 앞으로 돌아올 때 다시 본다
    var today by remember { mutableStateOf(LocalDate.now()) }
    // 날짜가 바뀌었을 때 알림에 보여 줄 이전 날짜 (null 이면 알림 없음)
    var changedFrom by remember { mutableStateOf<LocalDate?>(null) }
    val scope = rememberCoroutineScope()
    val lifecycleOwner = LocalLifecycleOwner.current

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                scope.launch {
                    val now = LocalDate.now()
                    val lastSeen = settingsStore.lastSeenDate()
                    settingsStore.saveLastSeenDate(now)
                    today = now
                    changedFrom = if (DayChangeRules.shouldNotify(lastSeen, now)) lastSeen else null
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    changedFrom?.let { previousDate ->
        AlertDialog(
            onDismissRequest = { changedFrom = null },
            confirmButton = {
                Button(onClick = { changedFrom = null }) {
                    Text(text = "확인")
                }
            },
            title = { Text(text = "날짜가 바뀌었습니다") },
            text = { Text(text = "$previousDate → $today\n오늘의 단어 목록이 새로 준비되었습니다.") },
        )
    }

    Surface(modifier = Modifier.fillMaxSize()) {
        if (showSettings) {
            SettingsScreen(
                settingsStore = settingsStore,
                wordRepository = wordRepository,
                onImported = { importCount++ },
                onClose = { showSettings = false },
                modifier = Modifier.fillMaxSize(),
            )
        } else if (showExam) {
            ExamScreen(
                dailyListRepository = dailyListRepository,
                examRepository = examRepository,
                settingsStore = settingsStore,
                today = today,
                onClose = { showExam = false },
                modifier = Modifier.fillMaxSize(),
            )
        } else {
            Column(
                modifier = Modifier.fillMaxSize().padding(16.dp),
                verticalArrangement = Arrangement.Top,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                // 제목과 부제는 한 덩어리로 묶어 위쪽에 둔다
                Text(text = "스마트보카", style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.primary)
                Text(
                    text = "동사만 알아도 대박이다",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(modifier = Modifier.height(12.dp))
                // 주 기능 버튼은 같은 모양과 높이로 나란히 둔다
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Button(
                        onClick = { showSettings = true },
                        shape = RoundedCornerShape(24.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.secondaryContainer,
                            contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                        ),
                        modifier = Modifier.weight(1f).height(48.dp),
                    ) {
                        Text(text = "설정")
                    }
                    Button(
                        onClick = { showExam = true },
                        shape = RoundedCornerShape(24.dp),
                        modifier = Modifier.weight(1f).height(48.dp),
                    ) {
                        Text(text = "시험 시작")
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
                TodayWordScreen(
                    dailyListRepository,
                    studyRepository,
                    settingsStore,
                    functionWords,
                    today = today,
                    refreshKey = importCount,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}
