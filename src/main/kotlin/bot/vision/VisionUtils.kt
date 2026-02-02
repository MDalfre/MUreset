package io.github.mdalfre.bot.vision

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

object VisionUtils {
    fun loadTemplate(resource: String): Mat? {
        val stream = javaClass.getResourceAsStream(resource) ?: return null
        val image = ImageIO.read(stream) ?: return null
        return toBgrMat(image)
    }

    fun toBgrMat(image: BufferedImage): Mat {
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

    fun cropRegionRect(
        image: Mat,
        xRatio: Double,
        yRatio: Double,
        wRatio: Double,
        hRatio: Double
    ): Rect {
        val width = image.cols()
        val height = image.rows()
        val x = (width * xRatio).toInt()
        val y = (height * yRatio).toInt()
        val w = (width * wRatio).toInt()
        val h = (height * hRatio).toInt()
        val safeX = max(0, min(x, width - 1))
        val safeY = max(0, min(y, height - 1))
        val safeW = max(1, min(w, width - safeX))
        val safeH = max(1, min(h, height - safeY))
        return Rect(safeX, safeY, safeW, safeH)
    }

    fun matchTemplateScore(region: Mat, template: Mat): Double {
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

    fun matchLocationMultiScale(region: Mat, template: Mat, scales: DoubleArray): TemplateMatch? {
        var bestMatch: TemplateMatch? = null
        for (scale in scales) {
            val scaled = Mat()
            val newWidth = (template.cols() * scale).toInt()
            val newHeight = (template.rows() * scale).toInt()
            if (newWidth < 6 || newHeight < 6) {
                continue
            }
            opencv_imgproc.resize(template, scaled, Size(newWidth, newHeight))
            val score = matchTemplateScore(region, scaled)
            if (score < 0) {
                continue
            }
            val resultCols = region.cols() - scaled.cols() + 1
            val resultRows = region.rows() - scaled.rows() + 1
            if (resultCols <= 0 || resultRows <= 0) {
                continue
            }
            val result = Mat(resultRows, resultCols, opencv_core.CV_32FC1)
            opencv_imgproc.matchTemplate(region, scaled, result, opencv_imgproc.TM_CCOEFF_NORMED)
            val minVal = DoubleArray(1)
            val maxVal = DoubleArray(1)
            val minLoc = Point()
            val maxLoc = Point()
            opencv_core.minMaxLoc(result, minVal, maxVal, minLoc, maxLoc, Mat())
            val candidate = TemplateMatch(maxLoc, scale, maxVal[0])
            if (bestMatch == null || candidate.score > bestMatch.score) {
                bestMatch = candidate
            }
        }
        return bestMatch
    }

    data class TemplateMatch(
        val point: Point,
        val scale: Double,
        val score: Double
    )
}
