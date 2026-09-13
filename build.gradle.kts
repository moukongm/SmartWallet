import org.gradle.api.GradleException
import org.gradle.api.artifacts.ProjectDependency

// Top-level build file where you can add configuration options common to all sub-projects/modules.
buildscript {
    repositories {
        google()
        mavenCentral()
        maven("https://jitpack.io")
    }
}


plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.kapt) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.android.library) apply false

    alias(libs.plugins.kotlin.serialization) apply false
}

gradle.projectsEvaluated {
    val businessModulePattern = Regex("^:business:([^:]+):(api|impl)$")

    allprojects.forEach { sourceProject ->
        sourceProject.configurations.forEach { configuration ->
            configuration.dependencies
                .withType(ProjectDependency::class.java)
                .forEach { dependency ->
                    val sourcePath = sourceProject.path
                    val targetPath = dependency.path

                    if (sourcePath.startsWith(":foundation") &&
                        targetPath.startsWith(":business")
                    ) {
                        throw GradleException(
                            "Illegal module dependency: $sourcePath cannot depend on $targetPath"
                        )
                    }

                    val sourceBusinessModule = businessModulePattern.matchEntire(sourcePath)
                    val targetBusinessModule = businessModulePattern.matchEntire(targetPath)
                    if (sourceBusinessModule != null && targetBusinessModule != null) {
                        val sourceFeature = sourceBusinessModule.groupValues[1]
                        val targetFeature = targetBusinessModule.groupValues[1]
                        val targetLayer = targetBusinessModule.groupValues[2]

                        if (sourceFeature != targetFeature && targetLayer != "api") {
                            throw GradleException(
                                "Illegal module dependency: $sourcePath must communicate " +
                                    "with $targetFeature through :business:$targetFeature:api"
                            )
                        }
                    }
                }
        }
    }
}
