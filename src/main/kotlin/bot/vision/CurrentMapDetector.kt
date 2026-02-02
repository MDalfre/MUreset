package io.github.mdalfre.bot.vision

import org.bytedeco.opencv.opencv_core.Mat
import io.github.mdalfre.bot.OpenCVBootstrap
import io.github.mdalfre.bot.windows.WindowActions
import io.github.mdalfre.bot.windows.WindowInfo

class CurrentMapDetector(
    private val windowActions: WindowActions = WindowActions()
) {
    private val elbelandTemplate: Mat? = VisionUtils.loadTemplate("/current_map_elbeland.png")

    fun isElbeland(window: WindowInfo): Boolean {
        val template = elbelandTemplate ?: return false
        OpenCVBootstrap.init()
        val screenshot = windowActions.captureClientArea(window)
        val bgr = VisionUtils.toBgrMat(screenshot)
        val roiRect = VisionUtils.cropRegionRect(bgr, REGION_X, REGION_Y, REGION_W, REGION_H)
        val roi = Mat(bgr, roiRect)
        val score = VisionUtils.matchTemplateScore(roi, template)
        return score >= TEMPLATE_THRESHOLD
    }

    private companion object {
        private const val REGION_X = 0.0
        private const val REGION_Y = 0.0
        private const val REGION_W = 0.35
        private const val REGION_H = 0.12
        private const val TEMPLATE_THRESHOLD = 0.8
    }
}
