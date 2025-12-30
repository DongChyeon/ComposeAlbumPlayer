plugins {
    id("cap.android.feature")
}

android {
    namespace = "com.dongchyeon.feature.album"
}

dependencies {
    implementation(projects.domain)
    implementation(projects.core.data)
    implementation(projects.core.designsystem)
    implementation(projects.core.ui)
    implementation(projects.core.media)

    // Extended Icons (for Player controls)
    implementation(libs.androidx.compose.material.icons.extended)

    // Image Loading
    implementation(libs.coil.compose)
}
