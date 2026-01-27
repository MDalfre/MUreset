package org.example.bot.vision

import java.awt.image.BufferedImage
import javax.imageio.ImageIO
import kotlin.math.max
import kotlin.math.min
import org.bytedeco.opencv.global.opencv_core
import org.bytedeco.opencv.global.opencv_imgproc
import org.bytedeco.opencv.opencv_core.Mat
import org.bytedeco.opencv.opencv_core.Point
import org.bytedeco.opencv.opencv_core.Rect
import org.bytedeco.opencv.opencv_core.Size
import org.example.bot.OpenCVBootstrap
import org.example.bot.windows.WindowActions
import org.example.bot.windows.WindowInfo
import org.example.model.LogEntry
import org.example.model.LogType

class HuntModeDetector(
    private val windowActions: WindowActions = WindowActions()
) {
    private val playTemplateColor: Mat? = loadTemplate("/play_button_template.png")
    private val pauseTemplateColor: Mat? = loadTemplate("/pause_button_template.png")
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
        val bgr = toBgrMat(screenshot)
        val roiRect = cropRegionRect(bgr)
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

    private fun cropRegionRect(image: Mat): Rect {
        val width = image.cols()
        val height = image.rows()
        val x = (width * REGION_X).toInt()
        val y = (height * REGION_Y).toInt()
        val w = (width * REGION_W).toInt()
        val h = (height * REGION_H).toInt()
        val safeX = max(0, min(x, width - 1))
        val safeY = max(0, min(y, height - 1))
        val safeW = max(1, min(w, width - safeX))
        val safeH = max(1, min(h, height - safeY))
        return Rect(safeX, safeY, safeW, safeH)
    }

    private fun matchScore(region: Mat, template: Mat): Double {
        val resultCols = region.cols() - template.cols() + 1
        val resultRows = region.rows() - template.rows() + 1
        if (resultCols <= 0 || resultRows <= 0) {
            return -1.0
        }
        val result = Mat(resultRows, resultCols, opencv_core.CV_32FC1)
        opencv_imgproc.matchTemplate(region, template, result, opencv_imgproc.TM_CCOEFF_NORMED)
        val minVal = DoubleArray(1)
        val maxVal = DoubleArray(1)
        val minLoc = Point()
        val maxLoc = Point()
        opencv_core.minMaxLoc(result, minVal, maxVal, minLoc, maxLoc, Mat())
        return maxVal[0]
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

    private fun loadTemplate(resource: String): Mat? {
        val stream = javaClass.getResourceAsStream(resource) ?: return null
        val image = ImageIO.read(stream) ?: return null
        return toBgrMat(image)
    }

    private fun toBgrMat(image: BufferedImage): Mat {
        val converted = BufferedImage(image.width, image.height, BufferedImage.TYPE_3BYTE_BGR)
        val graphics = converted.createGraphics()
        graphics.drawImage(image, 0, 0, null)
        graphics.dispose()
        val data = (converted.raster.dataBuffer as java.awt.image.DataBufferByte).data
        val mat = Mat(image.height, image.width, opencv_core.CV_8UC3)
        val pointer = mat.data()
        pointer.put(data, 0, data.size)
        return mat
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
