// 2026-11-11 - Smart Voca - 설정 값 저장과 읽기 (FR-040~042, FR-044, DataStore, ADR-003)
// 파일 위치: src/main/kotlin/com/rattatto/smartvoca/data/SettingsStore.kt

package com.rattatto.smartvoca.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.rattatto.smartvoca.data.csv.Level
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.time.LocalDate

/** 화면과 선정 규칙이 쓰는 설정 값 묶음. [levelFilter] 가 null 이면 전체 난이도다. */
data class AppSettings(
    val dailyCount: Int,
    val levelFilter: Level?,
    val verbRatio: Int,
    val examSize: Int = SettingsStore.DEFAULT_EXAM_SIZE,
) {
    companion object {
        val DEFAULT = AppSettings(
            dailyCount = SettingsStore.DEFAULT_DAILY_COUNT,
            levelFilter = null,
            verbRatio = SettingsStore.DEFAULT_VERB_RATIO,
            examSize = SettingsStore.DEFAULT_EXAM_SIZE,
        )
    }
}

/**
 * 설정 값을 기기에 저장한다. 앱을 다시 켜도 유지된다 (FR-044).
 * 값 범위는 여기서 검사한다. 저장된 값이 범위를 벗어나면 기본값으로 되돌려 읽는다.
 */
class SettingsStore(private val dataStore: DataStore<Preferences>) {

    val settings: Flow<AppSettings> = dataStore.data.map { prefs ->
        AppSettings(
            dailyCount = prefs[KEY_DAILY_COUNT]
                ?.takeIf { it in DAILY_COUNT_RANGE }
                ?: DEFAULT_DAILY_COUNT,
            levelFilter = parseLevelFilter(prefs[KEY_LEVEL_FILTER]),
            verbRatio = prefs[KEY_VERB_RATIO]
                ?.takeIf { it in VERB_RATIO_VALUES }
                ?: DEFAULT_VERB_RATIO,
            examSize = prefs[KEY_EXAM_SIZE]
                ?.takeIf { it in EXAM_SIZE_RANGE }
                ?: DEFAULT_EXAM_SIZE,
        )
    }

    /** 시험 한 회차의 문제 수. 5~20, 1 단위 (FR-043). */
    suspend fun saveExamSize(value: Int) {
        require(value in EXAM_SIZE_RANGE) { "시험 문제 수는 5~20 이어야 한다: $value" }
        dataStore.edit { it[KEY_EXAM_SIZE] = value }
    }

    /** 일일 노출 개수. 5~30, 1 단위 (FR-040). */
    suspend fun saveDailyCount(value: Int) {
        require(value in DAILY_COUNT_RANGE) { "일일 개수는 5~30 이어야 한다: $value" }
        dataStore.edit { it[KEY_DAILY_COUNT] = value }
    }

    /** 난이도. null 은 전체 (FR-041). */
    suspend fun saveLevelFilter(level: Level?) {
        dataStore.edit { it[KEY_LEVEL_FILTER] = level?.csvValue ?: LEVEL_FILTER_ALL }
    }

    /** 동사 비중. 30~90, 10 단위 (FR-042). 품사 확보 전까지 저장만 하고 적용하지 않는다. */
    suspend fun saveVerbRatio(value: Int) {
        require(value in VERB_RATIO_VALUES) { "동사 비중은 30~90 의 10 단위여야 한다: $value" }
        dataStore.edit { it[KEY_VERB_RATIO] = value }
    }

    /** 앱이 마지막으로 본 날짜. 처음 실행이면 null. 날짜 변경 알림에 쓴다. */
    suspend fun lastSeenDate(): LocalDate? =
        dataStore.data.first()[KEY_LAST_SEEN_DATE]?.let { runCatching { LocalDate.parse(it) }.getOrNull() }

    suspend fun saveLastSeenDate(date: LocalDate) {
        dataStore.edit { it[KEY_LAST_SEEN_DATE] = date.toString() }
    }

    private fun parseLevelFilter(stored: String?): Level? =
        if (stored == null || stored == LEVEL_FILTER_ALL) null else Level.fromCsv(stored)

    companion object {
        val DAILY_COUNT_RANGE = 5..30
        const val DEFAULT_DAILY_COUNT = 10
        val VERB_RATIO_VALUES = listOf(30, 40, 50, 60, 70, 80, 90)
        const val DEFAULT_VERB_RATIO = 60
        val EXAM_SIZE_RANGE = 5..20
        const val DEFAULT_EXAM_SIZE = 10

        private const val LEVEL_FILTER_ALL = "all"
        private val KEY_DAILY_COUNT = intPreferencesKey("daily_count")
        private val KEY_LEVEL_FILTER = stringPreferencesKey("level_filter")
        private val KEY_VERB_RATIO = intPreferencesKey("verb_ratio")
        private val KEY_EXAM_SIZE = intPreferencesKey("exam_size")
        private val KEY_LAST_SEEN_DATE = stringPreferencesKey("last_seen_date")

        private val Context.settingsDataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

        fun create(context: Context): SettingsStore = SettingsStore(context.applicationContext.settingsDataStore)
    }
}
