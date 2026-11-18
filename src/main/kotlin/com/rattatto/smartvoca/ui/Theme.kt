// 2026-11-18 - Smart Voca - 앱 색 테마 (흰 바탕, 주황 포인트, ADR-013)
// 파일 위치: src/main/kotlin/com/rattatto/smartvoca/ui/Theme.kt

package com.rattatto.smartvoca.ui

import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

/**
 * 흰 바탕에 참고 사진의 색을 쓴다 (ADR-013 개정).
 * - 오렌지 (primary): 버튼과 강조 색. 흰 글자와 대비가 4.5 이상이 되도록 어둡게 잡았다.
 * - 시나몬 (secondary): 보조 버튼과 선택 표시.
 * - 코코아 (onBackground 등): 본문 글자색. 순검정 대신 어두운 갈색을 써서 부드럽게 보이게 한다.
 * 참고 디자인의 마스코트나 로고는 쓰지 않는다.
 */
val SmartVocaColors = lightColorScheme(
    primary = Color(0xFFC75B12),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFFBE3D0),
    onPrimaryContainer = Color(0xFF3A1A08),
    secondary = Color(0xFF9A5B32),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFF3E1D3),
    onSecondaryContainer = Color(0xFF3A2418),
    background = Color(0xFFFFFFFF),
    onBackground = Color(0xFF2E1D14),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF2E1D14),
    surfaceVariant = Color(0xFFFBF3EC),
    onSurfaceVariant = Color(0xFF6B4A3A),
    outline = Color(0xFFB89A88),
)
