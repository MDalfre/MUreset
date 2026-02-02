package io.github.mdalfre.bot.windows

import com.sun.jna.platform.win32.User32
import com.sun.jna.platform.win32.WinDef
import com.sun.jna.platform.win32.WinUser

class WindowFinder {
    fun findWindowsByPrefix(prefix: String): List<WindowInfo> {
        val results = mutableListOf<WindowInfo>()
        val callback = WinUser.WNDENUMPROC { hWnd, _ ->
            val title = getWindowTitle(hWnd)
            if (title.startsWith(prefix)) {
                val rect = WinDef.RECT()
                User32.INSTANCE.GetWindowRect(hWnd, rect)
                results.add(WindowInfo(hWnd, title, rect))
            }
            true
        }
        User32.INSTANCE.EnumWindows(callback, null)
        return results
    }

    fun getWindowTitle(handle: WinDef.HWND): String {
        val buffer = CharArray(512)
        User32.INSTANCE.GetWindowText(handle, buffer, buffer.size)
        return String(buffer).trimEnd('\u0000')
    }

    fun getWindowInfo(handle: WinDef.HWND): WindowInfo? {
        val rect = WinDef.RECT()
        if (!User32.INSTANCE.GetWindowRect(handle, rect)) {
            return null
        }
        val title = getWindowTitle(handle)
        return WindowInfo(handle, title, rect)
    }

    fun getForegroundWindowInfo(): WindowInfo? {
        val handle = User32.INSTANCE.GetForegroundWindow() ?: return null
        return getWindowInfo(handle)
    }
}
