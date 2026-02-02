package io.github.mdalfre.bot.vision

import java.awt.image.BufferedImage
import org.bytedeco.opencv.global.opencv_core
import org.bytedeco.opencv.global.opencv_imgproc
import org.bytedeco.opencv.opencv_core.Mat
import org.bytedeco.opencv.opencv_core.Point
import org.bytedeco.opencv.opencv_core.Rect
import io.github.mdalfre.bot.OpenCVBootstrap
import io.github.mdalfre.bot.windows.WindowActions
import io.github.mdalfre.bot.windows.WindowInfo

class PartyInteractor(
    private val windowActions: WindowActions = WindowActions()
) {
    private val okTemplate: Mat? = VisionUtils.loadTemplate("/ok_dialog_template.png")

    fun rejoinParty(window: WindowInfo): Boolean {
        OpenCVBootstrap.init()
        windowActions.focus(window)
        Thread.sleep(300)
        val triedSlots = mutableSetOf<Int>()
        repeat(MAX_OK_ATTEMPTS) {
            val initial = windowActions.captureWindow(window)
            val (clickX, clickY, slotIndex) = pickRandomPartySlotClick(initial, triedSlots) ?: return false
            triedSlots.add(slotIndex)
            windowActions.clickAt(window.rect.left + clickX, window.rect.top + clickY)
            Thread.sleep(450)
            val afterClick = windowActions.captureWindow(window)
            val okClick = findOkButtonClick(afterClick)
            if (okClick != null) {
                println("PartyInteractor: clicando OK em (${okClick.first},${okClick.second})")
                windowActions.clickAt(window.rect.left + okClick.first, window.rect.top + okClick.second)
                return true
            }
            Thread.sleep(300)
        }
        return false
    }

    private fun pickRandomPartySlotClick(
        image: BufferedImage,
        triedSlots: Set<Int>
    ): Triple<Int, Int, Int>? {
        val width = image.width
        val height = image.height
        val outerPartyRect = Rect(
            (width * PARTY_X_START).toInt(),
            (height * PARTY_Y_START).toInt(),
            (width * PARTY_WIDTH).toInt(),
            (height * PARTY_HEIGHT).toInt()
        )
        val partyRect = Rect(
            outerPartyRect.x() + (outerPartyRect.width() * PARTY_INNER_X_START).toInt(),
            outerPartyRect.y(),
            (outerPartyRect.width() * PARTY_INNER_WIDTH).toInt(),
            outerPartyRect.height()
        )
        val slotHeight = partyRect.height() / PARTY_SLOTS
        if (slotHeight <= 0) {
            return null
        }
        val availableSlots = (0 until PARTY_SLOTS).filterNot { triedSlots.contains(it) }
        if (availableSlots.isEmpty()) {
            return null
        }
        val slotIndex = availableSlots.random()
        val clickX = partyRect.x() + (partyRect.width() * NAME_CLICK_X).toInt()
        val clickY = partyRect.y() +
            ((slotIndex + PARTY_SLOT_OFFSET) * slotHeight).toInt() +
            (slotHeight * SLOT_CLICK_Y).toInt() +
            SLOT_CLICK_Y_OFFSET_PX
        return Triple(clickX, clickY, slotIndex)
    }

    private fun findOkButtonClick(image: BufferedImage): Pair<Int, Int>? {
        val width = image.width
        val height = image.height
        val region = Rect(
            (width * OK_REGION_X).toInt(),
            (height * OK_REGION_Y).toInt(),
            (width * OK_REGION_W).toInt(),
            (height * OK_REGION_H).toInt()
        )
        val template = okTemplate ?: return null
        val bgr = VisionUtils.toBgrMat(image)
        val roi = Mat(bgr, region)
        val resultCols = roi.cols() - template.cols() + 1
        val resultRows = roi.rows() - template.rows() + 1
        if (resultCols <= 0 || resultRows <= 0) {
            return null
        }
        val result = Mat(resultRows, resultCols, opencv_core.CV_32FC1)
        opencv_imgproc.matchTemplate(roi, template, result, opencv_imgproc.TM_CCOEFF_NORMED)
        val minVal = DoubleArray(1)
        val maxVal = DoubleArray(1)
        val minLoc = Point()
        val maxLoc = Point()
        opencv_core.minMaxLoc(result, minVal, maxVal, minLoc, maxLoc, Mat())
        if (maxVal[0] < OK_TEMPLATE_THRESHOLD) {
            return null
        }
        val clickX = region.x() + maxLoc.x() + (template.cols() * OK_BUTTON_CENTER_X).toInt()
        val clickY = region.y() + maxLoc.y() + (template.rows() * OK_BUTTON_CENTER_Y).toInt()
        return clickX to clickY
    }


    private companion object {
        private const val PARTY_X_START = 0.74
        private const val PARTY_Y_START = 0.12
        private const val PARTY_WIDTH = 0.24
        private const val PARTY_HEIGHT = 0.32
        private const val PARTY_INNER_X_START = 0.5
        private const val PARTY_INNER_WIDTH = 0.5
        private const val PARTY_SLOTS = 5
        private const val NAME_CLICK_X = 0.6
        private const val SLOT_CLICK_Y = 0.5
        private const val PARTY_SLOT_OFFSET = 1.0
        private const val SLOT_CLICK_Y_OFFSET_PX = -10
        private const val OK_REGION_X = 0.35
        private const val OK_REGION_Y = 0.45
        private const val OK_REGION_W = 0.3
        private const val OK_REGION_H = 0.2
        private const val OK_TEMPLATE_THRESHOLD = 0.6
        private const val OK_BUTTON_CENTER_X = 0.36
        private const val OK_BUTTON_CENTER_Y = 0.73
        private const val MAX_OK_ATTEMPTS = 3
    }
}
