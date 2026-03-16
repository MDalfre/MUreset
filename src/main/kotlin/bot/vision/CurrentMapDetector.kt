package io.github.mdalfre.bot.vision

import io.github.mdalfre.bot.OpenCVBootstrap
import io.github.mdalfre.bot.windows.WindowActions
import io.github.mdalfre.bot.windows.WindowInfo
import io.github.mdalfre.model.CurrentMap
import org.bytedeco.opencv.opencv_core.Mat

class CurrentMapDetector(
    private val windowActions: WindowActions = WindowActions(),
) {
    fun isLorencia(window: WindowInfo): Boolean {
        return isCurrentMap(window, CurrentMap.LORENCIA)
    }

    fun isElbeland(window: WindowInfo): Boolean {
        return isCurrentMap(window, CurrentMap.ELBELAND)
    }

    private fun isCurrentMap(window: WindowInfo, currentMap: CurrentMap): Boolean {
        return VisionUtils.loadTemplate(currentMap.templateResource)?.let { template ->
            OpenCVBootstrap.init()
            val screenshot = windowActions.captureClientArea(window)
            val bgr = VisionUtils.toBgrMat(screenshot)
            val roiRect = VisionUtils.cropRegionRect(bgr, REGION_X, REGION_Y, REGION_W, REGION_H)
            val roi = Mat(bgr, roiRect)
            val score = VisionUtils.matchTemplateScore(roi, template)
            score >= TEMPLATE_THRESHOLD
        } ?: false
    }

    private companion object {
        private const val REGION_X = 0.0
        private const val REGION_Y = 0.0
        private const val REGION_W = 0.35
        private const val REGION_H = 0.12
        private const val TEMPLATE_THRESHOLD = 0.8
    }
}
