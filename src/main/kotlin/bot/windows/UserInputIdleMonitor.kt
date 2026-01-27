package io.github.mdalfre.bot.windows

import com.sun.jna.platform.win32.Kernel32
import com.sun.jna.platform.win32.User32
import com.sun.jna.platform.win32.WinUser

class UserInputIdleMonitor {
    fun idleMillis(): Long {
        val info = WinUser.LASTINPUTINFO()
        if (!User32.INSTANCE.GetLastInputInfo(info)) {
            return 0L
        }
        val currentTick = Kernel32.INSTANCE.GetTickCount().toLong() and 0xffffffffL
        val lastInputTick = info.dwTime.toLong() and 0xffffffffL
        return if (currentTick >= lastInputTick) {
            currentTick - lastInputTick
        } else {
            (0xffffffffL - lastInputTick) + currentTick + 1L
        }
    }
}
