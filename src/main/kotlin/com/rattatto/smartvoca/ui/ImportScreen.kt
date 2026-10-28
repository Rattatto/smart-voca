// 2026-10-28 - Smart Voca - CSV 가져오기 화면 (파일 선택, 저장, 결과와 건너뛴 행 표시) (FR-001~003)
// 파일 위치: src/main/kotlin/com/rattatto/smartvoca/ui/ImportScreen.kt

package com.rattatto.smartvoca.ui

import android.content.Context
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.rattatto.smartvoca.data.ImportSummary
import com.rattatto.smartvoca.data.WordRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

// 기기에 따라 CSV 의 MIME 타입이 다르게 잡히므로 여러 개를 허용한다
private val CSV_MIME_TYPES = arrayOf("text/csv", "text/comma-separated-values", "text/plain")

private const val MAX_SKIPPED_ROWS_SHOWN = 50

@Composable
fun ImportScreen(repository: WordRepository, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var statusText by remember { mutableStateOf("아직 가져온 단어가 없습니다.") }
    var summary by remember { mutableStateOf<ImportSummary?>(null) }

    val filePicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            try {
                val csvText = withContext(Dispatchers.IO) { readText(context, uri) }
                val result = repository.importCsv(csvText)
                summary = result
                statusText = "저장 ${result.inserted}개, 갱신 ${result.updated}개, 건너뜀 ${result.skipped.size}개"
            } catch (error: IllegalArgumentException) {
                // 필수 열 누락, 헤더 없음, 따옴표 오류 등 파일 형식 문제
                summary = null
                statusText = "가져오기 실패: ${error.message}"
            } catch (error: IllegalStateException) {
                summary = null
                statusText = "가져오기 실패: ${error.message}"
            }
        }
    }

    Column(modifier = modifier.fillMaxWidth().padding(16.dp)) {
        Button(onClick = { filePicker.launch(CSV_MIME_TYPES) }) {
            Text(text = "단어 CSV 가져오기")
        }
        Spacer(modifier = Modifier.height(12.dp))
        Text(text = statusText)

        // 건너뛴 행이 많을 수 있어서 화면에는 앞의 50개만 보여 준다
        summary?.skipped?.take(MAX_SKIPPED_ROWS_SHOWN)?.forEach { row ->
            Text(text = "${row.lineNumber}행: ${row.reason}")
        }
    }
}

private fun readText(context: Context, uri: Uri): String {
    val stream = context.contentResolver.openInputStream(uri)
        ?: error("파일을 열 수 없습니다.")
    return stream.bufferedReader(Charsets.UTF_8).use { it.readText() }
}
