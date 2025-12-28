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

    // Extended Icons
    implementation(libs.androidx.compose.material.icons.extended)

    // Media3
    implementation(libs.androidx.media3.exoplayer)
    implementation(libs.androidx.media3.ui)
    implementation(libs.androidx.media3.session)

    // Image Loading
    implementation(libs.coil.compose)
}
