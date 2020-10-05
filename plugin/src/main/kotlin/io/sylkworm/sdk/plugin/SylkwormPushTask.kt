package io.sylkworm.sdk.plugin

import com.android.build.gradle.api.TestVariant
import org.gradle.api.DefaultTask

class SylkwormPushTask : DefaultTask(){
    private var variant: TestVariant? = null

    companion object {
        fun taskName(variant: TestVariant) = "pushSilkworm${variant.name.capitalize()}"
    }

    open fun init(variant: TestVariant) {
        this.variant = variant;
    }
}
