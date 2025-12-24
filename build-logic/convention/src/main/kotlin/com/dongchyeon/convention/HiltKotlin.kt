package com.dongchyeon.convention

import org.gradle.api.Project
import org.gradle.kotlin.dsl.dependencies

/**
 * Configure Hilt for Kotlin (JVM only, no Android)
 */
internal fun Project.configureHiltKotlin() {
    with(pluginManager) {
        apply("com.google.devtools.ksp")
    }

    dependencies {
        add("implementation", libs.findLibrary("hilt.core").get())
        add("ksp", libs.findLibrary("hilt.compiler").get())
    }
}
