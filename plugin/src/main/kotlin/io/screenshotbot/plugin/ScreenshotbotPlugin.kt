package io.screenshotbot.plugin

import org.gradle.api.Plugin
import org.gradle.api.Project
import com.android.build.gradle.AppExtension
import com.android.build.gradle.LibraryExtension
import com.android.build.gradle.api.ApkVariantOutput
import com.android.build.gradle.api.TestVariant
import com.facebook.testing.screenshot.build.PullScreenshotsTask


open class ScreenshotbotExtensions(var channelName: String? = null, var githubRepo: String? = null){
}

class ScreenshotbotPlugin : Plugin<Project> {
    companion object {
        const val GROUP = "Screenshotbot"
    }


    private lateinit var screenshotbotExtensions: ScreenshotbotExtensions

    override fun apply(project: Project) {
        val extensions = project.extensions

        val plugins = project.plugins
        screenshotbotExtensions = extensions.create("screenshotbot", ScreenshotbotExtensions::class.java)

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
                    init(variant, screenshotbotExtensions)
                }.dependsOn(project.tasks.findByName(PullScreenshotsTask.taskName(variant)))
            }
        }
    }

}
