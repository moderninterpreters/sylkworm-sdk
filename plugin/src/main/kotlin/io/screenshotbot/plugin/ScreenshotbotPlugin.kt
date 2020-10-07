package io.screenshotbot.plugin

import org.gradle.api.Plugin
import org.gradle.api.Project
import com.android.build.gradle.AppExtension
import com.android.build.gradle.LibraryExtension
import com.android.build.gradle.api.ApkVariantOutput
import com.android.build.gradle.api.TestVariant
import com.facebook.testing.screenshot.build.PullScreenshotsTask


open class SylkwormPluginExtension(var channelName: String? = null){
}

class SylkwormPlugin : Plugin<Project> {
    companion object {
        const val GROUP = "Sylkworm"
    }


    private lateinit var sylkwormExtensions: SylkwormPluginExtension

    override fun apply(project: Project) {
        System.out.println("apply sylkworm!")
        val extensions = project.extensions

        val plugins = project.plugins
        sylkwormExtensions = extensions.create("sylkworm", SylkwormPluginExtension::class.java)

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
                val taskName = ScreenshotbotPushTask.taskName(variant)
                System.out.println("creating task name: " + taskName)
                project.tasks.create(
                    taskName,
                    ScreenshotbotPushTask::class.java
                ).apply {
                    init(variant, sylkwormExtensions)
                }.dependsOn(project.tasks.findByName(PullScreenshotsTask.taskName(variant)))
            }
        }
    }

}
