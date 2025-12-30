plugins {
    id("cap.android.application")
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.hilt)
    alias(libs.plugins.ksp)
}

android {
    namespace = "com.dongchyeon.compose.album.player"

    defaultConfig {
        applicationId = "com.dongchyeon.compose.album.player"
        versionCode = 1
        versionName = "1.0"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
        }
    }

    buildFeatures {
        compose = true
    }
}

dependencies {
    // Feature Modules (Navigation)
    implementation(projects.feature.home)
    implementation(projects.feature.album)

    // Core Modules
    // ✅ 직접 사용
    implementation(projects.core.designsystem)  // MainActivity에서 AlbumPlayerTheme 사용

    // ✅ Hilt Module 스캔 (필수!)
    // Hilt의 KSP는 컴파일 타임에 @Module을 직접 스캔해야 함
    // 전이 의존성만으로는 @Module을 찾을 수 없음
    implementation(projects.core.data)          // @Module DataModule (AlbumRepository 제공)
    implementation(projects.core.media)         // @Module MediaModule (MusicPlayer 제공)

    // ❌ 제외 가능 (전이 의존성으로 충분)
    // domain - feature 모듈들이 이미 의존
    // core:ui - feature 모듈들이 이미 의존
    // core:network - core:data가 이미 의존

    // Android Core
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)

    // Compose
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)

    // Hilt
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    implementation(libs.hilt.navigation.compose)

    // Navigation
    implementation(libs.androidx.navigation.compose)

    // Test
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
}
