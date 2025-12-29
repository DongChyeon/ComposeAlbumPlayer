package com.dongchyeon.convention

import org.gradle.api.Project
import org.gradle.kotlin.dsl.dependencies

/**
 * Configure Kotlin testing (JVM only)
 */
internal fun Project.configureTestKotlin() {
    dependencies {
        add("testImplementation", libs.findLibrary("junit").get())
        add("testImplementation", libs.findLibrary("kotlinx.coroutines.test").get())
    }
}
