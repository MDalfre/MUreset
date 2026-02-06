package io.github.mdalfre.bot.windows

import com.sun.jna.platform.win32.Kernel32
import com.sun.jna.platform.win32.User32
import com.sun.jna.platform.win32.WinDef
import com.sun.jna.platform.win32.WinUser
import java.awt.Rectangle
import java.awt.Robot
import java.awt.event.KeyEvent
import java.awt.image.BufferedImage

class WindowActions {
    private val user32 = User32.INSTANCE
    private val robot = Robot()

    init {
        robot.autoDelay = 40
    }

    fun focus(window: WindowInfo) {
        bringToFront(window.handle)
        Thread.sleep(250)
        moveMouseToWindowCenter(window)
        BotInputTracker.markBotInput()
        Thread.sleep(150)
    }

    fun sendCommand(command: String) {
        sendEnter()
        Thread.sleep(80)
        sendText(command)
        Thread.sleep(80)
        sendEnter()
        BotInputTracker.markBotInput()
    }

    fun sendKey(keyCode: Int) {
        robot.keyPress(keyCode)
        robot.keyRelease(keyCode)
        BotInputTracker.markBotInput()
    }

    fun clickAt(x: Int, y: Int) {
        robot.mouseMove(x, y)
        robot.mousePress(java.awt.event.InputEvent.BUTTON1_DOWN_MASK)
        robot.mouseRelease(java.awt.event.InputEvent.BUTTON1_DOWN_MASK)
        BotInputTracker.markBotInput()
    }

    fun captureWindow(window: WindowInfo): BufferedImage {
        val rect = window.rect
        val width = rect.right - rect.left
        val height = rect.bottom - rect.top
        val captureRect = Rectangle(rect.left, rect.top, width, height)
        return robot.createScreenCapture(captureRect)
    }

    fun captureClientArea(window: WindowInfo): BufferedImage {
        val clientRect = WinDef.RECT()
        user32.GetClientRect(window.handle, clientRect)
        val width = clientRect.right - clientRect.left
        val height = clientRect.bottom - clientRect.top
        val windowRect = window.rect
        val windowWidth = windowRect.right - windowRect.left
        val windowHeight = windowRect.bottom - windowRect.top
        val border = ((windowWidth - width) / 2).coerceAtLeast(0)
        val titleBar = (windowHeight - height - border).coerceAtLeast(0)
        val captureRect = Rectangle(
            windowRect.left + border,
            windowRect.top + titleBar,
            width,
            height
        )
        return robot.createScreenCapture(captureRect)
    }

    fun clientOrigin(window: WindowInfo): Pair<Int, Int> {
        val clientRect = WinDef.RECT()
        user32.GetClientRect(window.handle, clientRect)
        val width = clientRect.right - clientRect.left
        val height = clientRect.bottom - clientRect.top
        val windowRect = window.rect
        val windowWidth = windowRect.right - windowRect.left
        val windowHeight = windowRect.bottom - windowRect.top
        val border = ((windowWidth - width) / 2).coerceAtLeast(0)
        val titleBar = (windowHeight - height - border).coerceAtLeast(0)
        return (windowRect.left + border) to (windowRect.top + titleBar)
    }

    private fun bringToFront(handle: WinDef.HWND) {
        val foreground = user32.GetForegroundWindow()
        if (foreground == handle) {
            return
        }

        val currentThreadId = user32.GetWindowThreadProcessId(foreground, null)
        val targetThreadId = user32.GetWindowThreadProcessId(handle, null)
        val thisThreadId = Kernel32.INSTANCE.GetCurrentThreadId()
        val currentThread = WinDef.DWORD(currentThreadId.toLong())
        val targetThread = WinDef.DWORD(targetThreadId.toLong())
        val thisThread = WinDef.DWORD(thisThreadId.toLong())

        user32.ShowWindow(handle, WinUser.SW_RESTORE)

        if (currentThreadId != thisThreadId) {
            user32.AttachThreadInput(thisThread, currentThread, true)
        }
        if (targetThreadId != thisThreadId) {
            user32.AttachThreadInput(thisThread, targetThread, true)
        }

        user32.SetForegroundWindow(handle)
        user32.BringWindowToTop(handle)
        user32.SetFocus(handle)

        if (targetThreadId != thisThreadId) {
            user32.AttachThreadInput(thisThread, targetThread, false)
        }
        if (currentThreadId != thisThreadId) {
            user32.AttachThreadInput(thisThread, currentThread, false)
        }
    }

    private fun moveMouseToWindowCenter(window: WindowInfo) {
        val rect = window.rect
        val centerX = rect.left + (rect.right - rect.left) / 2
        val centerY = rect.top + (rect.bottom - rect.top) / 2
        robot.mouseMove(centerX, centerY)
    }

    private fun sendEnter() {
        robot.keyPress(KeyEvent.VK_ENTER)
        robot.keyRelease(KeyEvent.VK_ENTER)
    }

    private fun sendText(text: String) {
        for (char in text) {
            val keyCode = resolveKeyCode(char)
            if (keyCode <= 0) {
                continue
            }
            val upper = char.isUpperCase()
            if (upper) {
                robot.keyPress(KeyEvent.VK_SHIFT)
            }
            robot.keyPress(keyCode)
            robot.keyRelease(keyCode)
            if (upper) {
                robot.keyRelease(KeyEvent.VK_SHIFT)
            }
        }
    }

    private fun resolveKeyCode(char: Char): Int {
        return when (char) {
            '/' -> KeyEvent.VK_SLASH
            ' ' -> KeyEvent.VK_SPACE
            '-' -> KeyEvent.VK_MINUS
            '_' -> KeyEvent.VK_MINUS
            '.' -> KeyEvent.VK_PERIOD
            ',' -> KeyEvent.VK_COMMA
            ':' -> KeyEvent.VK_SEMICOLON
            '+' -> KeyEvent.VK_EQUALS
            '=' -> KeyEvent.VK_EQUALS
            else -> KeyEvent.getExtendedKeyCodeForChar(char.code)
        }
    }
}
