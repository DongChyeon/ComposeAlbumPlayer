plugins {
    id("cap.android.library")
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "com.dongchyeon.core.ui"

    buildFeatures {
        compose = true
    }
}

dependencies {
    implementation(projects.core.designsystem)
    
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.ui.tooling.preview)
    debugImplementation(libs.androidx.compose.ui.tooling)

    // Lifecycle
    implementation(libs.androidx.lifecycle.viewmodel.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)

    // Image Loading
    implementation(libs.coil.compose)
}
