plugins {
    id("cap.android.feature")
}

android {
    namespace = "com.dongchyeon.feature.album"
}

dependencies {
    implementation(project(":domain"))
    implementation(project(":core:designsystem"))
    implementation(project(":core:ui"))
}
