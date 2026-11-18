// 2026-11-11 - Smart Voca - 설정 값 저장, 범위 검사, 기본값 복귀 테스트 (메모리 DataStore) (FR-040~042, FR-044)
// 파일 위치: src/test/kotlin/com/rattatto/smartvoca/data/SettingsStoreTest.kt

package com.rattatto.smartvoca.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.mutablePreferencesOf
import com.rattatto.smartvoca.data.csv.Level
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

/**
 * 파일 기반 DataStore 대신 메모리 DataStore 로 저장 규칙만 확인한다.
 * 파일 저장은 Windows 에서 임시 파일 이름 바꾸기가 막혀 JVM 테스트가 불안정하므로 기기에서 확인한다.
 */
class SettingsStoreTest {

    private fun storeWith(initial: Preferences = emptyPreferences()): SettingsStore =
        SettingsStore(InMemoryDataStore(initial))

    @Test
    fun `저장한 값을 다시 읽는다`() = runBlocking {
        val store = storeWith()

        store.saveDailyCount(20)
        store.saveLevelFilter(Level.ADVANCED)
        store.saveVerbRatio(80)

        assertEquals(AppSettings(dailyCount = 20, levelFilter = Level.ADVANCED, verbRatio = 80), store.settings.first())
    }

    @Test
    fun `저장한 적이 없으면 기본값을 돌려준다`() = runBlocking {
        assertEquals(AppSettings.DEFAULT, storeWith().settings.first())
    }

    @Test
    fun `난이도를 전체로 되돌리면 null 로 읽힌다`() = runBlocking {
        val store = storeWith()

        store.saveLevelFilter(Level.BASIC)
        store.saveLevelFilter(null)

        assertEquals(null, store.settings.first().levelFilter)
    }

    @Test
    fun `일일 개수는 5에서 30 사이만 받는다`() = runBlocking {
        val store = storeWith()

        assertFailsWith<IllegalArgumentException> { store.saveDailyCount(4) }
        assertFailsWith<IllegalArgumentException> { store.saveDailyCount(31) }
        store.saveDailyCount(5)
        store.saveDailyCount(30)
        assertEquals(30, store.settings.first().dailyCount)
    }

    @Test
    fun `동사 비중은 30에서 90 사이의 10 단위만 받는다`() = runBlocking {
        val store = storeWith()

        assertFailsWith<IllegalArgumentException> { store.saveVerbRatio(35) }
        assertFailsWith<IllegalArgumentException> { store.saveVerbRatio(100) }
        store.saveVerbRatio(90)
        assertEquals(90, store.settings.first().verbRatio)
    }

    @Test
    fun `시험 문제 수는 5에서 20 사이만 받고 기본값은 10이다`() = runBlocking {
        val store = storeWith()

        assertEquals(10, store.settings.first().examSize)
        assertFailsWith<IllegalArgumentException> { store.saveExamSize(4) }
        assertFailsWith<IllegalArgumentException> { store.saveExamSize(21) }
        store.saveExamSize(20)
        assertEquals(20, store.settings.first().examSize)
    }

    @Test
    fun `저장된 값이 범위를 벗어나면 기본값으로 읽는다`() = runBlocking {
        // 범위 검사를 거치지 않은 값이 들어 있는 경우를 가정한다
        val store = storeWith(initial = mutablePreferencesOf(intPreferencesKey("daily_count") to 99))

        assertEquals(AppSettings.DEFAULT.dailyCount, store.settings.first().dailyCount)
    }
}

/** 테스트용 메모리 DataStore. 변환 규칙은 실제 DataStore 와 같다. */
private class InMemoryDataStore(initial: Preferences) : DataStore<Preferences> {

    private val state = MutableStateFlow(initial)

    override val data = state

    override suspend fun updateData(transform: suspend (t: Preferences) -> Preferences): Preferences {
        val updated = transform(state.value)
        state.value = updated
        return updated
    }
}
