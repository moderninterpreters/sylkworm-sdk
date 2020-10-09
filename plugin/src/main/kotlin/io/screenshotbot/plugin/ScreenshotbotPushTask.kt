package io.screenshotbot.plugin

import com.android.build.gradle.api.TestVariant
import com.facebook.testing.screenshot.build.PullScreenshotsTask
import io.screenshotbot.sdk.Recorder
import org.gradle.api.DefaultTask
import org.gradle.api.tasks.TaskAction
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.lib.Constants
import java.io.File

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
        recorder.commit = getCommit()

        val channelName =  sylkwormExtensions?.channelName?:("root-project" + project.path)

        System.out.println("Uploading images to sylkworm.io (run with -i to see progress, this might be slow on first run)")
        recorder.doRecorder(
            channelName,
            dir.absolutePath
        )
    }

    private fun getCommit(): String? {
        val gitDir = getGitDir(project.projectDir)
        if (gitDir == null) {
            return null
        }
        val git = Git.open(gitDir)
        val commit = git.repository.resolve(Constants.HEAD)

        return commit?.name
    }

    private fun getGitDir(projectDir: File): File? {
        val gitDir = File(projectDir, ".git")
        if (gitDir.exists()) {
            return gitDir
        }

        val parent = projectDir.parentFile
        if (parent.equals(gitDir)) {
            return null
        }
        return getGitDir(parent)
    }

}
