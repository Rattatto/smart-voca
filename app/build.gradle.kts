// 2026-10-28 - Smart Voca - 안드로이드 앱 모듈 설정 (SDK 버전, 소스 경로, 의존성)
// 파일 위치: app/build.gradle.kts

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
}

android {
    namespace = "com.rattatto.smartvoca"

    compileSdk {
        // 설치한 플랫폼 android-37.1 과 맞춘다
        version = release(37) {
            minorApiLevel = 1
        }
    }

    defaultConfig {
        applicationId = "com.rattatto.smartvoca"
        // 기획서 OI-09 확정: 최소 지원 API 26 (NFR-007)
        minSdk = 26
        targetSdk {
            version = release(37)
        }
        versionCode = 1
        versionName = "0.1.0"
    }

    buildFeatures {
        compose = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    // 소스는 저장소 규칙(CLAUDE.md)에 따라 루트의 src/ 에 둔다
    sourceSets {
        getByName("main") {
            kotlin.directories.add("../src/main/kotlin")
        }
        getByName("test") {
            kotlin.directories.add("../src/test/kotlin")
        }
    }
}

dependencies {
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    debugImplementation(libs.androidx.compose.ui.tooling)

    // Room: 로컬 DB (ADR-002). 컴파일러는 KSP 로 실행한다
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)

    testImplementation(libs.kotlin.test.junit)
    // JVM 에서 Room 을 돌리기 위한 Robolectric (실기기 없이 DAO 테스트)
    testImplementation(libs.robolectric)
}
