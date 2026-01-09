plugins {
    id("cap.android.feature")
}

android {
    namespace = "com.dongchyeon.feature.player"
}

dependencies {
    implementation(projects.domain)
    implementation(projects.core.designsystem)
    implementation(projects.core.ui)

    implementation(libs.androidx.compose.material.icons.extended)

    // Image Loading
    implementation(libs.coil.compose)
}
