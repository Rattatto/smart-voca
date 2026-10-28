// 2026-10-28 - Smart Voca - Gradle 프로젝트 구성 (모듈 포함, 저장소 설정)
// 파일 위치: settings.gradle.kts

pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "smart-voca"
include(":app")
