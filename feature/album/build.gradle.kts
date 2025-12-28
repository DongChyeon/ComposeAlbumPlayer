plugins {
    id("cap.android.feature")
}

android {
    namespace = "com.dongchyeon.feature.album"
}

dependencies {
    implementation(projects.domain)
    implementation(projects.core.designsystem)
    implementation(projects.core.ui)

    // Image Loading
    implementation(libs.coil.compose)
}
