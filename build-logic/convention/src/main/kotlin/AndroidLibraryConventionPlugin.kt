import com.android.build.gradle.LibraryExtension
import com.dongchyeon.convention.configureKotlinAndroid
import com.dongchyeon.convention.configureTestAndroid
import com.dongchyeon.convention.configureTestKotlin
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure

class AndroidLibraryConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            with(pluginManager) {
                apply("com.android.library")
                apply("org.jetbrains.kotlin.android")
            }

            extensions.configure<LibraryExtension> {
                configureKotlinAndroid(this)
                defaultConfig.targetSdk = 35
            }

            configureTestAndroid()
            configureTestKotlin()
        }
    }
}
