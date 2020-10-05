package io.sylkworm.sdk.plugin

import com.android.build.gradle.api.TestVariant
import com.facebook.testing.screenshot.build.PullScreenshotsTask
import io.sylkworm.sdk.Credential
import io.sylkworm.sdk.Recorder
import org.gradle.api.DefaultTask
import org.gradle.api.tasks.TaskAction

open class SylkwormPushTask : DefaultTask() {
    private var variant: TestVariant? = null

    companion object {
        fun taskName(variant: TestVariant) = "pushSylkworm${variant.name.capitalize()}"
    }

    init {
        description = "Pull screenshots and push it to Sylkworm"
        group = SylkwormPlugin.GROUP
    }

    public fun init(variant: TestVariant) {
        this.variant = variant;
    }

    @TaskAction
    fun pushSylkworm() {
        val dir = PullScreenshotsTask.getReportDir(project, variant!!);
        val recorder = Recorder()
        recorder.doRecorder(
            "dummy-channel",
            dir.absolutePath
        )
    }
}
