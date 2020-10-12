package io.screenshotbot.plugin

import com.android.build.gradle.api.TestVariant
import com.facebook.testing.screenshot.build.PullScreenshotsTask
import io.screenshotbot.sdk.Recorder
import io.screenshotbot.sdk.RepoProcessor
import org.gradle.api.DefaultTask
import org.gradle.api.tasks.TaskAction
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.lib.Constants
import java.io.File


open class ScreenshotbotPushTask : DefaultTask() {
    private var extensions: ScreenshotbotExtensions? = null
    private var variant: TestVariant? = null
    private var production: Boolean = false

    companion object {
        fun taskName(variant: TestVariant) = "${variant.name}Screenshotbot"
        fun publishTaskName(variant: TestVariant) = "publish${variant.name.capitalize()}Screenshotbot"
    }

    init {
        description = "Pull screenshots and push it to Sylkworm"
        group = ScreenshotbotPlugin.GROUP
    }

    public fun init(variant: TestVariant, sylkwormExtensions: ScreenshotbotExtensions, production: Boolean) {
        this.variant = variant;
        this.extensions = sylkwormExtensions
        this.production = production
    }

    @TaskAction
    fun pushSylkworm() {
        val dir = PullScreenshotsTask.getReportDir(project, variant!!);
        val recorder = Recorder()
        recorder.setGithubRepo(extensions?.githubRepo)
        val status = RepoProcessor.getCommit(project.projectDir)
        recorder.commit = status?.commit
        recorder.clean = status?.clean
        recorder.production = production
        recorder.branch = extensions?.branch

        val channelName =  extensions?.channelName?:("root-project" + project.path)

        System.out.println("Uploading images to sylkworm.io (run with -i to see progress, this might be slow on first run)")
        recorder.doRecorder(
            channelName,
            dir.absolutePath
        )
    }


}
