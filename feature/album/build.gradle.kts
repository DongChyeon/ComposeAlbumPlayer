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
    implementation(projects.core.media)

    // Extended Icons (for Player controls)
    implementation(libs.androidx.compose.material.icons.extended)

    // Media3 (for Player UI components)
    implementation(libs.androidx.media3.exoplayer)
    implementation(libs.androidx.media3.ui)
    implementation(libs.androidx.media3.session)

    // Image Loading
    implementation(libs.coil.compose)
}
