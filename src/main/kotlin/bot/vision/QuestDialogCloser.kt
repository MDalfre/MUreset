package io.github.mdalfre.bot.vision

import org.bytedeco.opencv.opencv_core.Mat
import io.github.mdalfre.bot.windows.WindowActions
import io.github.mdalfre.bot.windows.WindowInfo
import io.github.mdalfre.model.LogEntry
import io.github.mdalfre.model.LogType

class QuestDialogCloser(
    private val windowActions: WindowActions = WindowActions()
) {
    private val questTemplate: Mat? = VisionUtils.loadTemplate("/quest_dialog_template.png")

    fun closeIfPresent(window: WindowInfo, onLog: (LogEntry) -> Unit = {}) {
        val template = questTemplate ?: return
        val screenshot = windowActions.captureWindow(window)
        val bgr = VisionUtils.toBgrMat(screenshot)
        val match = VisionUtils.matchLocationMultiScale(bgr, template, TEMPLATE_SCALES) ?: return
        if (match.score < TEMPLATE_THRESHOLD) {
            return
        }
        val scaledWidth = (template.cols() * match.scale).toInt()
        val scaledHeight = (template.rows() * match.scale).toInt()
        val clickX = match.point.x() + (scaledWidth * CLOSE_X_RATIO).toInt()
        val clickY = match.point.y() + (scaledHeight * CLOSE_Y_RATIO).toInt()
        windowActions.clickAt(window.rect.left + clickX, window.rect.top + clickY)
        onLog(LogEntry("Closed quest dialog.", LogType.INFO))
    }

    private companion object {
        private const val TEMPLATE_THRESHOLD = 0.9
        private val TEMPLATE_SCALES = doubleArrayOf(0.9, 0.95, 1.0, 1.05, 1.1)
        private const val CLOSE_X_RATIO = 0.94
        private const val CLOSE_Y_RATIO = 0.5
    }
}
