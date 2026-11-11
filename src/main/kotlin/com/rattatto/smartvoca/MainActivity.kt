// 2026-10-28 - Smart Voca - 앱 진입 화면 (임시 시작 화면, 기능 화면은 이후 추가)
// 파일 위치: src/main/kotlin/com/rattatto/smartvoca/MainActivity.kt

package com.rattatto.smartvoca

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.material3.Button
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.rattatto.smartvoca.data.DailyListRepository
import com.rattatto.smartvoca.data.SettingsStore
import com.rattatto.smartvoca.data.WordRepository
import com.rattatto.smartvoca.data.db.SmartVocaDatabase
import com.rattatto.smartvoca.ui.ImportScreen
import com.rattatto.smartvoca.ui.SettingsScreen
import com.rattatto.smartvoca.ui.TodayWordScreen

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // 앱 전체에서 DB 연결을 하나만 쓰도록 여기서 한 번만 만들고 저장소들이 함께 쓴다
        val database = SmartVocaDatabase.create(applicationContext)
        val wordRepository = WordRepository(database)
        val dailyListRepository = DailyListRepository(database)
        val settingsStore = SettingsStore.create(applicationContext)
        setContent {
            MaterialTheme {
                StartScreen(wordRepository, dailyListRepository, settingsStore)
            }
        }
    }
}

@Composable
fun StartScreen(
    wordRepository: WordRepository,
    dailyListRepository: DailyListRepository,
    settingsStore: SettingsStore,
) {
    var showSettings by remember { mutableStateOf(false) }

    Surface(modifier = Modifier.fillMaxSize()) {
        if (showSettings) {
            SettingsScreen(settingsStore, onClose = { showSettings = false }, modifier = Modifier.fillMaxSize())
        } else {
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.Top,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(text = "스마트보카")
                Text(text = "동사만 알아도 대박이다")
                Button(onClick = { showSettings = true }) {
                    Text(text = "설정")
                }
                ImportScreen(wordRepository)
                TodayWordScreen(dailyListRepository, settingsStore, modifier = Modifier.weight(1f))
            }
        }
    }
}
