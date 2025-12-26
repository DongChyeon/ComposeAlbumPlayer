plugins {
    id("cap.android.feature")
}

android {
    namespace = "com.dongchyeon.feature.home"
}

dependencies {
    implementation(project(":domain"))
    implementation(project(":core:designsystem"))
    implementation(project(":core:ui"))
    
    // Image Loading
    implementation(libs.coil.compose)
}
