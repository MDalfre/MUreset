package io.github.mdalfre.bot.vision

import java.awt.event.KeyEvent
import java.awt.image.BufferedImage
import org.bytedeco.opencv.opencv_core.Mat
import org.bytedeco.opencv.opencv_core.Rect
import io.github.mdalfre.bot.OpenCVBootstrap
import io.github.mdalfre.bot.windows.WindowActions
import io.github.mdalfre.bot.windows.WindowInfo
import io.github.mdalfre.model.LogEntry
import io.github.mdalfre.model.LogType
import io.github.mdalfre.model.WarpMap

class MapWarpInteractor(
    private val windowActions: WindowActions = WindowActions()
) {
    private val elbeland3Template: Mat? = VisionUtils.loadTemplate("/elbeland3_template.png")
    private val elbeland2Template: Mat? = VisionUtils.loadTemplate("/elbeland2_template.png")

    fun warpToMap(window: WindowInfo, map: WarpMap, onLog: (LogEntry) -> Unit = {}): Boolean {
        val template = getTemplate(map)
        if (template == null) {
            onLog(LogEntry("Template not found for ${map.label}.", LogType.ATTENTION))
            return false
        }
        OpenCVBootstrap.init()
        windowActions.focus(window)
        windowActions.sendKey(KeyEvent.VK_M)
        Thread.sleep(MENU_OPEN_MS)

        repeat(MAX_ATTEMPTS) { attempt ->
            val screenshot = windowActions.captureClientArea(window)
            val click = findWarpClick(screenshot, map, template)
            if (click != null) {
                onLog(LogEntry("Warping to ${map.label} at (${click.first},${click.second})...", LogType.INFO))
                val (originX, originY) = windowActions.clientOrigin(window)
                windowActions.clickAt(originX + click.first, originY + click.second)
                Thread.sleep(WARP_WAIT_MS)
                return true
            }
            if (attempt < MAX_ATTEMPTS - 1) {
                Thread.sleep(RETRY_DELAY_MS)
                windowActions.sendKey(KeyEvent.VK_M)
                Thread.sleep(MENU_OPEN_MS)
            }
        }

        onLog(LogEntry("Failed to locate ${map.label} in warp list.", LogType.ATTENTION))
        return false
    }

    private fun findWarpClick(
        image: BufferedImage,
        map: WarpMap,
        template: Mat
    ): Pair<Int, Int>? {
        val bgr = VisionUtils.toBgrMat(image)
        val roiRect = VisionUtils.cropRegionRect(bgr, REGION_X, REGION_Y, REGION_W, REGION_H)
        val baseMatch = findTemplateClickInRegion(
            bgr = bgr,
            regionRect = roiRect,
            template = template,
            threshold = TEMPLATE_THRESHOLD
        )
        val anchorTemplate = when (map) {
            WarpMap.ELBELAND_3 -> elbeland2Template
            WarpMap.ELBELAND_2 -> elbeland3Template
        }
        if (anchorTemplate == null) {
            return baseMatch
        }
        val anchorMatch = findTemplateClickInRegion(
            bgr,
            roiRect,
            anchorTemplate,
            ELBELAND2_THRESHOLD
        )
        val targetHeight = (template.rows() * 2)
        val anchorHeight = anchorTemplate.rows()
        val subRect = when {
            anchorMatch == null -> null
            map == WarpMap.ELBELAND_3 -> {
                val subTop = (anchorMatch.second - roiRect.y()) + (anchorHeight / 2) + ROW_GAP_PX
                val safeTop = subTop.coerceAtLeast(0)
                val subHeight = targetHeight.coerceAtMost(roiRect.height() - safeTop)
                if (subHeight > 0) Rect(roiRect.x(), roiRect.y() + safeTop, roiRect.width(), subHeight) else null
            }
            else -> {
                val subBottom = (anchorMatch.second - roiRect.y()) - (anchorHeight / 2) - ROW_GAP_PX
                val safeBottom = subBottom.coerceAtLeast(0)
                val subHeight = targetHeight.coerceAtMost(safeBottom)
                val subTop = (safeBottom - subHeight).coerceAtLeast(0)
                if (subHeight > 0) Rect(roiRect.x(), roiRect.y() + subTop, roiRect.width(), subHeight) else null
            }
        }
        if (subRect != null) {
            val refinedMatch = findTemplateClickInRegion(
                bgr,
                subRect,
                template,
                TEMPLATE_THRESHOLD
            )
            if (refinedMatch != null) {
                return refinedMatch
            }
        }
        return baseMatch
    }

    private fun findTemplateClickInRegion(
        bgr: Mat,
        regionRect: Rect,
        template: Mat,
        threshold: Double
    ): Pair<Int, Int>? {
        val roi = Mat(bgr, regionRect)
        val match = VisionUtils.matchLocationMultiScale(roi, template, TEMPLATE_SCALES) ?: return null
        if (match.score < threshold) {
            return null
        }
        val scaledWidth = (template.cols() * match.scale).toInt()
        val scaledHeight = (template.rows() * match.scale).toInt()
        val clickX = regionRect.x() + match.point.x() + (scaledWidth / 2)
        val clickY = regionRect.y() + match.point.y() + (scaledHeight / 2)
        return clickX to clickY
    }


    private fun getTemplate(map: WarpMap): Mat? {
        return when (map) {
            WarpMap.ELBELAND_2 -> elbeland2Template
            WarpMap.ELBELAND_3 -> elbeland3Template
        }
    }

    private companion object {
        private const val REGION_X = 0.02
        private const val REGION_Y = 0.12
        private const val REGION_W = 0.5
        private const val REGION_H = 0.6
        private const val TEMPLATE_THRESHOLD = 0.92
        private const val ELBELAND2_THRESHOLD = 0.9
        private const val ROW_GAP_PX = 6
        private const val MENU_OPEN_MS = 500L
        private const val WARP_WAIT_MS = 5_000L
        private const val RETRY_DELAY_MS = 400L
        private const val MAX_ATTEMPTS = 3
        private val TEMPLATE_SCALES = doubleArrayOf(0.9, 0.95, 1.0, 1.05, 1.1)
    }

}
