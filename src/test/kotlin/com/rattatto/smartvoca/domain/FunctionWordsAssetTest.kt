// 2026-11-11 - Smart Voca - 기능어 자산 파일이 제자리에 있고 형식이 맞는지 확인
// 파일 위치: src/test/kotlin/com/rattatto/smartvoca/domain/FunctionWordsAssetTest.kt

package com.rattatto.smartvoca.domain

import java.io.File
import org.junit.Test
import kotlin.test.assertTrue

/**
 * 파일 위치가 틀리면 앱이 시작하자마자 죽는다. 이 테스트는 소스 위치의 파일과 형식을 확인한다.
 * APK 에 실제로 들어갔는지는 빌드 결과 (assets/function_words.csv) 로 따로 확인한다.
 */
class FunctionWordsAssetTest {

    @Test
    fun `기능어 자산 파일이 소스 위치에 있고 읽을 수 있다`() {
        // 단위 테스트의 작업 폴더는 app/ 이다
        val assetFile = File("../src/main/assets/${FunctionWords.ASSET_FILE_NAME}")
        assertTrue(assetFile.exists(), "기능어 파일이 없다: ${assetFile.absolutePath}")

        val functionWords = FunctionWords.parse(assetFile.readText(Charsets.UTF_8))

        assertTrue(functionWords.tips.size > 100, "기능어가 ${functionWords.tips.size}개뿐이다")
    }
}
