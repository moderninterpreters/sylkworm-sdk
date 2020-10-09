package io.screenshotbot.plugin

import com.android.build.gradle.api.TestVariant
import com.facebook.testing.screenshot.build.PullScreenshotsTask
import io.screenshotbot.sdk.Recorder
import org.gradle.api.DefaultTask
import org.gradle.api.tasks.TaskAction

open class ScreenshotbotPushTask : DefaultTask() {
    private var sylkwormExtensions: ScreenshotbotExtensions? = null
    private var variant: TestVariant? = null

    companion object {
        fun taskName(variant: TestVariant) = "${variant.name}Screenshotbot"
    }

    init {
        description = "Pull screenshots and push it to Sylkworm"
        group = ScreenshotbotPlugin.GROUP
    }

    public fun init(variant: TestVariant, sylkwormExtensions: ScreenshotbotExtensions) {
        this.variant = variant;
        this.sylkwormExtensions = sylkwormExtensions
    }

    @TaskAction
    fun pushSylkworm() {
        val dir = PullScreenshotsTask.getReportDir(project, variant!!);
        val recorder = Recorder()
        recorder.setGithubRepo(sylkwormExtensions?.githubRepo)
        val channelName =  sylkwormExtensions?.channelName?:("root-project" + project.path)

        System.out.println("Uploading images to sylkworm.io (run with -i to see progress, this might be slow on first run)")
        recorder.doRecorder(
            channelName,
            dir.absolutePath
        )
    }
}
