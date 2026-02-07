package io.github.mdalfre.bot.vision

import io.github.mdalfre.bot.OpenCVBootstrap
import io.github.mdalfre.bot.windows.WindowActions
import io.github.mdalfre.bot.windows.WindowInfo
import org.bytedeco.opencv.opencv_core.Mat

class SwitchModeDetector(
    private val windowActions: WindowActions = WindowActions()
) {
    private val switchTemplate: Mat? = VisionUtils.loadTemplate("/switch_mode_template.png")

    fun isSwitchActive(window: WindowInfo): Boolean {
        if (switchTemplate == null) {
            return false
        }
        OpenCVBootstrap.init()
        val screenshot = windowActions.captureClientArea(window)
        val bgr = VisionUtils.toBgrMat(screenshot)
        val match = VisionUtils.matchLocationMultiScale(bgr, switchTemplate, TEMPLATE_SCALES)
            ?: return false
        return match.score >= SWITCH_THRESHOLD
    }

    private companion object {
        private const val SWITCH_THRESHOLD = 0.92
        private val TEMPLATE_SCALES = doubleArrayOf(0.9, 0.95, 1.0, 1.05, 1.1)
    }
}
