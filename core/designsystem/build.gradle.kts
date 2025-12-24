plugins {
    id("cap.android.library")
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "com.dongchyeon.core.designsystem"

    buildFeatures {
        compose = true
    }
}

dependencies {
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.core)
    debugImplementation(libs.androidx.compose.ui.tooling)
}
