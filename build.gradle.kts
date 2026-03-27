// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.compose) apply false
}
subprojects {
    afterEvaluate {
        if (repositories.isNotEmpty()) {
            error(
                "Project-level repositories are not allowed in ${project.path}. " +
                        "Declare repositories only in settings.gradle.kts."
            )
        }
    }
}