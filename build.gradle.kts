// 2026-10-28 - Smart Voca - 루트 빌드 스크립트 (플러그인 버전은 gradle/libs.versions.toml 에서 관리)
// 파일 위치: build.gradle.kts

plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.compose) apply false
}
