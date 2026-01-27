package io.github.mdalfre.bot

import org.bytedeco.javacpp.Loader
import org.bytedeco.opencv.global.opencv_core

object OpenCVBootstrap {
    @Volatile
    private var loaded = false

    fun init() {
        if (loaded) {
            return
        }
        Loader.load(opencv_core::class.java)
        loaded = true
    }
}
