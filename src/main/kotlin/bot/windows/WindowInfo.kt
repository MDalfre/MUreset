package org.example.bot.windows

import com.sun.jna.platform.win32.WinDef

data class WindowInfo(
    val handle: WinDef.HWND,
    val title: String,
    val rect: WinDef.RECT
)
