package io.github.mdalfre.bot.windows

import com.sun.jna.platform.win32.User32
import com.sun.jna.platform.win32.WinDef
import io.github.mdalfre.model.LogEntry
import io.github.mdalfre.model.LogType

class DebugHotkeyMonitor {
    fun logCursor(window: WindowInfo, onLog: (LogEntry) -> Unit) {
        val point = WinDef.POINT()
        if (!User32.INSTANCE.GetCursorPos(point)) {
            return
        }
        val rect = window.rect
        val inside = point.x >= rect.left && point.x <= rect.right &&
            point.y >= rect.top && point.y <= rect.bottom
        if (!inside) {
            onLog(LogEntry("Debug: cursor outside window.", LogType.INFO))
            return
        }
        val relX = point.x - rect.left
        val relY = point.y - rect.top
        onLog(LogEntry("Debug: cursor in window at ($relX,$relY).", LogType.INFO))
    }
}
