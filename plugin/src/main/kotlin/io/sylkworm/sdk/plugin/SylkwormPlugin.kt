package io.sylkworm.sdk.plugin

import org.gradle.api.Plugin
import org.gradle.api.Project
import com.android.build.gradle.AppExtension
import com.android.build.gradle.LibraryExtension
import com.android.build.gradle.api.ApkVariantOutput
import com.android.build.gradle.api.TestVariant
import com.facebook.testing.screenshot.build.PullScreenshotsTask


open class SylkwormPluginExtension {

}

class SylkwormPlugin : Plugin<Project> {

    private lateinit var sylkwormExtensions: SylkwormPluginExtension

    override fun apply(project: Project) {
        val extensions = project.extensions

        val plugins = project.plugins
        sylkwormExtensions = extensions.create("screenshots", SylkwormPluginExtension::class.java)

        val variants = when {
            plugins.hasPlugin("com.android.application") ->
                extensions.findByType(AppExtension::class.java)!!.testVariants
            plugins.hasPlugin("com.android.library") ->
                extensions.findByType(LibraryExtension::class.java)!!.testVariants
            else -> throw IllegalArgumentException("Screenshot Test plugin requires Android's plugin")
        }

        variants.all {
            generateTasksFor(project, it)
        }
    }

    private fun generateTasksFor(project: Project, variant: TestVariant) {
        variant.outputs.all {
            if (it is ApkVariantOutput) {
                project.tasks.create(
                        SylkwormPushTask.taskName(variant),
                        SylkwormPushTask::class.java
                ).apply {
                    init(variant)
                }.dependsOn(project.tasks.findByName(PullScreenshotsTask.taskName(variant)))
            }
        }
    }

}
