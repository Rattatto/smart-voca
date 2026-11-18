// 2026-11-18 - Smart Voca - 앱 색 테마 (흰 바탕, 주황 포인트, ADR-013)
// 파일 위치: src/main/kotlin/com/rattatto/smartvoca/ui/Theme.kt

package com.rattatto.smartvoca.ui

import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

/** 흰 바탕에 주황을 포인트로 쓴다. 참고 디자인의 마스코트나 로고는 쓰지 않는다 (ADR-013). */
val SmartVocaColors = lightColorScheme(
    primary = Color(0xFFF57C00),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFFFE0B2),
    onPrimaryContainer = Color(0xFF4E2600),
    secondary = Color(0xFFFFB74D),
    onSecondary = Color(0xFF3E2000),
    background = Color(0xFFFFFFFF),
    onBackground = Color(0xFF1F1F1F),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF1F1F1F),
    surfaceVariant = Color(0xFFFFF3E0),
    onSurfaceVariant = Color(0xFF5D4037),
)
