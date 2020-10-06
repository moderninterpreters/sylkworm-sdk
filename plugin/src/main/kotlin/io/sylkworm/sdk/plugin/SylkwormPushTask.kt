package io.sylkworm.sdk.plugin

import com.android.build.gradle.api.TestVariant
import com.facebook.testing.screenshot.build.PullScreenshotsTask
import io.sylkworm.sdk.Recorder
import org.gradle.api.DefaultTask
import org.gradle.api.tasks.TaskAction

open class SylkwormPushTask : DefaultTask() {
    private var sylkwormExtensions: SylkwormPluginExtension? = null
    private var variant: TestVariant? = null

    companion object {
        fun taskName(variant: TestVariant) = "pushSylkworm${variant.name.capitalize()}"
    }

    init {
        description = "Pull screenshots and push it to Sylkworm"
        group = SylkwormPlugin.GROUP
    }

    public fun init(variant: TestVariant, sylkwormExtensions: SylkwormPluginExtension) {
        this.variant = variant;
        this.sylkwormExtensions = sylkwormExtensions
    }

    @TaskAction
    fun pushSylkworm() {
        val dir = PullScreenshotsTask.getReportDir(project, variant!!);
        val recorder = Recorder()
        val channelName =  sylkwormExtensions?.channelName?:("root-project" + project.path)

        System.out.println("Uploading images to sylkworm.io (run with -i to see progress, this might be slow on first run)")
        recorder.doRecorder(
            channelName,
            dir.absolutePath
        )
    }
}
