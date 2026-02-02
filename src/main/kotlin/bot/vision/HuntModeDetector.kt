package io.github.mdalfre.bot.vision

import kotlin.math.max
import org.bytedeco.opencv.global.opencv_imgproc
import org.bytedeco.opencv.opencv_core.Mat
import org.bytedeco.opencv.opencv_core.Size
import io.github.mdalfre.bot.OpenCVBootstrap
import io.github.mdalfre.bot.windows.WindowActions
import io.github.mdalfre.bot.windows.WindowInfo
import io.github.mdalfre.model.LogEntry
import io.github.mdalfre.model.LogType

class HuntModeDetector(
    private val windowActions: WindowActions = WindowActions()
) {
    private val playTemplateColor: Mat? = VisionUtils.loadTemplate("/play_button_template.png")
    private val pauseTemplateColor: Mat? = VisionUtils.loadTemplate("/pause_button_template.png")
    private val playTemplateEdges: Mat? = playTemplateColor?.let { toEdges(it) }
    private val pauseTemplateEdges: Mat? = pauseTemplateColor?.let { toEdges(it) }

    fun waitForHuntMode(
        window: WindowInfo,
        timeoutMs: Long,
        pollMs: Long = 600L,
        onLog: (LogEntry) -> Unit = {}
    ): Boolean {
        val deadline = System.currentTimeMillis() + timeoutMs
        while (System.currentTimeMillis() < deadline) {
            if (!Thread.currentThread().isInterrupted && isHuntActive(window)) {
                return true
            }
            Thread.sleep(pollMs)
        }
        onLog(LogEntry("Timeout waiting for hunt mode.", LogType.ATTENTION))
        return false
    }

    fun isHuntActive(window: WindowInfo): Boolean {
        if (playTemplateColor == null || pauseTemplateColor == null) {
            return false
        }
        OpenCVBootstrap.init()
        val screenshot = windowActions.captureClientArea(window)
        val bgr = VisionUtils.toBgrMat(screenshot)
        val roiRect = VisionUtils.cropRegionRect(bgr, REGION_X, REGION_Y, REGION_W, REGION_H)
        val roi = Mat(bgr, roiRect)
        val (pauseScoreColor, _) = matchScoreMultiScale(roi, pauseTemplateColor)
        val (playScoreColor, _) = matchScoreMultiScale(roi, playTemplateColor)
        val roiEdges = toEdges(roi)
        val (pauseScoreEdges, _) = pauseTemplateEdges?.let { matchScoreMultiScale(roiEdges, it) }
            ?: (-1.0 to 1.0)
        val (playScoreEdges, _) = playTemplateEdges?.let { matchScoreMultiScale(roiEdges, it) }
            ?: (-1.0 to 1.0)
        val colorDiff = pauseScoreColor - playScoreColor
        val edgeDiff = pauseScoreEdges - playScoreEdges
        val decision = when {
            max(pauseScoreColor, playScoreColor) >= COLOR_ACTIVE_MIN -> {
                when {
                    colorDiff >= COLOR_ACTIVE_GAP -> true
                    -colorDiff >= COLOR_ACTIVE_GAP -> false
                    else -> false
                }
            }
            max(pauseScoreEdges, playScoreEdges) >= EDGE_ACTIVE_MIN -> {
                when {
                    edgeDiff >= EDGE_ACTIVE_GAP -> true
                    -edgeDiff >= EDGE_ACTIVE_GAP -> false
                    else -> false
                }
            }
            else -> false
        }
        return decision
    }

    private fun matchScore(region: Mat, template: Mat): Double {
        return VisionUtils.matchTemplateScore(region, template)
    }

    private fun matchScoreMultiScale(region: Mat, template: Mat): Pair<Double, Double> {
        var bestScore = -1.0
        var bestScale = 1.0
        for (scale in TEMPLATE_SCALES) {
            val scaled = Mat()
            val newWidth = (template.cols() * scale).toInt()
            val newHeight = (template.rows() * scale).toInt()
            if (newWidth < 6 || newHeight < 6) {
                continue
            }
            opencv_imgproc.resize(template, scaled, Size(newWidth, newHeight))
            val score = matchScore(region, scaled)
            if (score > bestScore) {
                bestScore = score
                bestScale = scale
            }
        }
        return bestScore to bestScale
    }

    private fun toEdges(image: Mat): Mat {
        val gray = Mat()
        opencv_imgproc.cvtColor(image, gray, opencv_imgproc.COLOR_BGR2GRAY)
        val edges = Mat()
        opencv_imgproc.Canny(gray, edges, 50.0, 150.0)
        return edges
    }

    private companion object {
        private const val REGION_X = 0.15
        private const val REGION_Y = 0.0
        private const val REGION_W = 0.25
        private const val REGION_H = 0.12
        private const val COLOR_ACTIVE_MIN = 0.7
        private const val COLOR_ACTIVE_GAP = 0.02
        private const val EDGE_ACTIVE_MIN = 0.3
        private const val EDGE_ACTIVE_GAP = 0.03
        private val TEMPLATE_SCALES = doubleArrayOf(0.9, 0.95, 1.0, 1.05, 1.1)
    }
}
