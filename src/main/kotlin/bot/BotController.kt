package io.github.mdalfre.bot

import io.github.mdalfre.bot.windows.WindowActions
import io.github.mdalfre.bot.windows.WindowFinder
import io.github.mdalfre.bot.windows.UserInputIdleMonitor
import io.github.mdalfre.bot.windows.BotInputTracker
import io.github.mdalfre.model.AttributeType
import io.github.mdalfre.model.CharacterConfig
import io.github.mdalfre.model.CharacterStats
import io.github.mdalfre.model.LogEntry
import io.github.mdalfre.model.LogType
import io.github.mdalfre.bot.BotRuntimeState
import io.github.mdalfre.bot.vision.CurrentMapDetector
import io.github.mdalfre.bot.vision.PartyInteractor
import io.github.mdalfre.bot.vision.HuntModeDetector
import io.github.mdalfre.bot.vision.MapWarpInteractor
import io.github.mdalfre.bot.vision.QuestDialogCloser
import io.github.mdalfre.bot.vision.SwitchModeDetector
import io.github.mdalfre.bot.windows.DebugHotkeyMonitor
import io.github.mdalfre.bot.windows.WindowInfo
import java.awt.event.KeyEvent

class BotController(
    private val windowFinder: WindowFinder = WindowFinder(),
    private val windowActions: WindowActions = WindowActions(),
    private val idleMonitor: UserInputIdleMonitor = UserInputIdleMonitor()
) {
    @Volatile
    private var running = false
    private val partyInteractor = PartyInteractor(windowActions)
    private val huntModeDetector = HuntModeDetector(windowActions)
    private val mapWarpInteractor = MapWarpInteractor(windowActions)
    private val currentMapDetector = CurrentMapDetector(windowActions)
    private val questDialogCloser = QuestDialogCloser(windowActions)
    private val switchModeDetector = SwitchModeDetector(windowActions)
    private val debugHotkeyMonitor = DebugHotkeyMonitor()
    @Volatile
    private var debugCursorRunning = false

    fun start(
        characters: List<CharacterConfig>,
        onLog: (LogEntry) -> Unit = {},
        onStats: (String, CharacterStats) -> Unit = { _, _ -> },
        onStatus: (String, Boolean) -> Unit = { _, _ -> },
        onActive: (String?) -> Unit = {},
        checkIntervalSeconds: Int = 60,
        teleportWaitSeconds: Int = 30,
        cpuSavingMode: Boolean = false,
        onComplete: () -> Unit = {}
    ) {
        if (running) {
            return
        }
        running = true
        Thread {
            OpenCVBootstrap.init()
            try {
                runBot(
                    characters,
                    onLog,
                    onStats,
                    onStatus,
                    onActive,
                    checkIntervalSeconds,
                    teleportWaitSeconds,
                    cpuSavingMode
                )
            } finally {
                running = false
                onComplete()
            }
        }.apply {
            isDaemon = true
            start()
        }
    }

    fun stop() {
        running = false
    }

    private fun runBot(
        characters: List<CharacterConfig>,
        onLog: (LogEntry) -> Unit,
        onStats: (String, CharacterStats) -> Unit,
        onStatus: (String, Boolean) -> Unit,
        onActive: (String?) -> Unit,
        checkIntervalSeconds: Int,
        teleportWaitSeconds: Int,
        cpuSavingMode: Boolean
    ) {
        var cycle = 0
        val intervalSeconds = normalizeSeconds(checkIntervalSeconds, DEFAULT_CHECK_INTERVAL_SECONDS)
        val huntWaitTimeoutMs = normalizeSeconds(teleportWaitSeconds, DEFAULT_TELEPORT_WAIT_SECONDS) * 1000L
        while (running) {
            if (!waitForUserIdle(onLog)) {
                return
            }
            cycle += 1
            onLog(infoLog("Cycle started #$cycle"))
            val resetThisCycle = mutableSetOf<String>()
            for (character in characters) {
                if (!running) {
                    break
                }
                if (!waitForUserIdle(onLog)) {
                    return
                }
                val continueRun = processCharacter(
                    character = character,
                    resetThisCycle = resetThisCycle,
                    huntWaitTimeoutMs = huntWaitTimeoutMs,
                    onLog = onLog,
                    onStats = onStats,
                    onStatus = onStatus,
                    onActive = onActive,
                    cpuSavingMode = cpuSavingMode
                )
                if (!continueRun) {
                    return
                }
            }
            onActive(null)
            onLog(infoLog("Cycle finished #$cycle"))
            repeat(intervalSeconds) {
                if (!running) {
                    return
                }
                Thread.sleep(1000)
            }
        }
    }

    private fun parseStats(title: String): Pair<String, CharacterStats>? {
        val match = TITLE_REGEX.find(title) ?: return null
        val name = match.groupValues[1]
        val level = match.groupValues[2].toIntOrNull() ?: return null
        val master = match.groupValues[3].toIntOrNull() ?: return null
        val resets = match.groupValues[4].toIntOrNull() ?: return null
        return name to CharacterStats(level = level, masterLevel = master, resets = resets)
    }

    private fun processCharacter(
        character: CharacterConfig,
        resetThisCycle: MutableSet<String>,
        huntWaitTimeoutMs: Long,
        onLog: (LogEntry) -> Unit,
        onStats: (String, CharacterStats) -> Unit,
        onStatus: (String, Boolean) -> Unit,
        onActive: (String?) -> Unit,
        cpuSavingMode: Boolean
    ): Boolean {
        onActive(character.name)
        try {
            val windows = windowFinder.findWindowsByPrefix(windowPrefix(character.name))
            updateWindowStatus(character, windows.isNotEmpty(), onLog, onStatus)
            for (window in windows) {
                if (!running) {
                    return false
                }
                val shouldContinue = handleWindow(
                    window = window,
                    character = character,
                    resetThisCycle = resetThisCycle,
                    huntWaitTimeoutMs = huntWaitTimeoutMs,
                    onLog = onLog,
                    onStats = onStats,
                    cpuSavingMode = cpuSavingMode
                )
                if (!shouldContinue) {
                    return false
                }
            }
            return true
        } finally {
            onActive(null)
        }
    }

    private fun handleWindow(
        window: WindowInfo,
        character: CharacterConfig,
        resetThisCycle: MutableSet<String>,
        huntWaitTimeoutMs: Long,
        onLog: (LogEntry) -> Unit,
        onStats: (String, CharacterStats) -> Unit,
        cpuSavingMode: Boolean
    ): Boolean {
        onLog(infoLog("Selecting: ${character.name}"))
        val parsed = parseStats(window.title) ?: return true
        val (name, stats) = parsed
        onStats(name, stats)
        logStats(character, stats, onLog)
        if (cpuSavingMode) {
            ensureSwitchMode(window, character, desiredActive = false, onLog = onLog)
        }
        if (stats.level != RESET_LEVEL) {
            onLog(infoLog("${character.name} waiting for level ${RESET_LEVEL}..."))
            focusAndCapture(character, window)
            if (cpuSavingMode) {
                ensureSwitchMode(window, character, desiredActive = true, onLog = onLog)
            }
            return true
        }
        if (!resetThisCycle.add(character.name)) {
            onLog(infoLog("Reset already executed this cycle for ${character.name}"))
            focusAndCapture(character, window)
            if (cpuSavingMode) {
                ensureSwitchMode(window, character, desiredActive = true, onLog = onLog)
            }
            return true
        }
        val completed = handleResetFlow(window, character, stats, huntWaitTimeoutMs, onLog, onStats)
        if (completed && cpuSavingMode) {
            ensureSwitchMode(window, character, desiredActive = true, onLog = onLog)
        }
        return completed
    }

    private fun handleResetFlow(
        window: WindowInfo,
        character: CharacterConfig,
        stats: CharacterStats,
        huntWaitTimeoutMs: Long,
        onLog: (LogEntry) -> Unit,
        onStats: (String, CharacterStats) -> Unit
    ): Boolean {
        questDialogCloser.closeIfPresent(window, onLog)
        performResetRoutine(window, character, stats, onLog)
        if (!runSoloLeveling(window, character, onLog, onStats)) {
            return false
        }
        Thread.sleep(REJOIN_AFTER_SOLO_DELAY_MS)
        rejoinPartyWithRetry(window, character, onLog)
        logHuntReturn(window, character, stats, huntWaitTimeoutMs, onLog)
        captureScreenshot(character, window)
        return true
    }

    private fun logHuntReturn(
        window: WindowInfo,
        character: CharacterConfig,
        stats: CharacterStats,
        huntWaitTimeoutMs: Long,
        onLog: (LogEntry) -> Unit
    ) {
        val huntOk = huntModeDetector.waitForHuntMode(window, huntWaitTimeoutMs, onLog = onLog)
        if (!huntOk) {
            onLog(attentionLog("Hunt mode not detected for ${character.name}."))
            return
        }
        onLog(importantLog("${character.name} returned to hunt mode."))
        onLog(importantLog("${character.name} reset complete -> Resets: ${stats.resets + 1}"))
    }

    private fun logStats(character: CharacterConfig, stats: CharacterStats, onLog: (LogEntry) -> Unit) {
        onLog(infoLog("${character.name} Level: ${stats.level} Master Level: ${stats.masterLevel} Resets: ${stats.resets}"))
    }

    private fun focusAndCapture(character: CharacterConfig, window: WindowInfo) {
        windowActions.focus(window)
        captureScreenshot(character, window)
    }

    private fun captureScreenshot(character: CharacterConfig, window: WindowInfo) {
        if (!character.active) {
            return
        }
        val image = windowActions.captureClientArea(window)
        BotRuntimeState.setScreenshot(character.name, image)
    }

    fun logFocusedCursor(onLog: (LogEntry) -> Unit) {
        val window = windowFinder.getForegroundWindowInfo()
        if (window == null) {
            onLog(attentionLog("Debug: no foreground window detected."))
            return
        }
        val prefix = window.title.take(10)
        onLog(infoLog("Debug: focused window title prefix: \"$prefix\""))
        startFocusedCursorLogging(onLog)
    }

    fun stopFocusedCursorLogging(onLog: (LogEntry) -> Unit) {
        if (!debugCursorRunning) {
            return
        }
        debugCursorRunning = false
        onLog(infoLog("Debug: stopped cursor logging."))
    }

    private fun startFocusedCursorLogging(onLog: (LogEntry) -> Unit) {
        if (debugCursorRunning) {
            return
        }
        debugCursorRunning = true
        Thread {
            while (debugCursorRunning) {
                val window = windowFinder.getForegroundWindowInfo()
                if (window == null) {
                    onLog(attentionLog("Debug: no foreground window detected."))
                } else {
                    debugHotkeyMonitor.logCursor(window, onLog)
                }
                Thread.sleep(DEBUG_CURSOR_POLL_MS)
            }
        }.apply {
            isDaemon = true
            start()
        }
    }

    private fun performResetRoutine(
        window: WindowInfo,
        character: CharacterConfig,
        stats: CharacterStats,
        onLog: (LogEntry) -> Unit
    ) {
        val totalPoints = (stats.resets + 1) * character.pointsPerReset
        if (totalPoints <= 0) {
            onLog(attentionLog("Invalid total points for ${character.name}"))
            return
        }
        val basePoints = basePoints(character)
        val overflowAttr = character.overflowAttribute
        val usedPoints = basePoints.entries
            .filter { it.key != overflowAttr }
            .sumOf { it.value }
        var overflowPoints = totalPoints - usedPoints
        if (overflowPoints < 0) {
            onLog(attentionLog("Configured points exceed available total for ${character.name}"))
            return
        }
        if (overflowPoints > OVERFLOW_CAP) {
            val remaining = overflowPoints - OVERFLOW_CAP
            overflowPoints = OVERFLOW_CAP
            onLog(attentionLog("${character.name} overflow capped at $OVERFLOW_CAP. Remaining points: $remaining"))
        }

        windowActions.focus(window)
        windowActions.sendCommand("/reset")
        Thread.sleep(2000)
        onLog(importantLog("Reset executed for ${character.name}."))

        val commands = ATTRIBUTE_COMMANDS.map { (attr, prefix) ->
            val value = if (attr == overflowAttr) overflowPoints else basePoints[attr] ?: 0
            "$prefix $value" to value
        }

        for ((command, value) in commands) {
            if (!running) {
                break
            }
            if (value <= 0) {
                continue
            }
            windowActions.sendCommand(command)
            Thread.sleep(COMMAND_DELAY_MS)
        }
    }

    private fun runSoloLeveling(
        window: WindowInfo,
        character: CharacterConfig,
        onLog: (LogEntry) -> Unit,
        onStats: (String, CharacterStats) -> Unit
    ): Boolean {
        val targetLevel = character.soloLevel
        if (targetLevel <= 0) {
            return true
        }
        onLog(infoLog("Starting solo leveling for ${character.name} (target level $targetLevel)."))
        if (!mapWarpInteractor.warpToMap(window, character.warpMap, onLog)) {
            onLog(attentionLog("Warp to ${character.warpMap.label} failed for ${character.name}."))
            return true
        }
        ensureHuntModeActive(window, character, onLog)
        return waitForSoloLevel(window, character, targetLevel, onLog, onStats)
    }

    private fun ensureHuntModeActive(
        window: WindowInfo,
        character: CharacterConfig,
        onLog: (LogEntry) -> Unit
    ) {
        repeat(HUNT_TOGGLE_MAX_ATTEMPTS) { attempt ->
            if (!running) {
                return
            }
            if (huntModeDetector.isHuntActive(window)) {
                return
            }
            windowActions.sendKey(KeyEvent.VK_HOME)
            Thread.sleep(HUNT_TOGGLE_DELAY_MS)
            if (huntModeDetector.isHuntActive(window)) {
                return
            }
            if (attempt == HUNT_TOGGLE_MAX_ATTEMPTS - 1) {
                onLog(attentionLog("Hunt mode not active for ${character.name} after ${HUNT_TOGGLE_MAX_ATTEMPTS} attempts."))
            }
        }
    }

    private fun ensureSwitchMode(
        window: WindowInfo,
        character: CharacterConfig,
        desiredActive: Boolean,
        onLog: (LogEntry) -> Unit
    ) {
        val currentActive = switchModeDetector.isSwitchActive(window)
        if (currentActive == desiredActive) {
            return
        }
        windowActions.focus(window)
        windowActions.sendCtrlKey(KeyEvent.VK_F)
        Thread.sleep(SWITCH_TOGGLE_DELAY_MS)
        val updated = switchModeDetector.isSwitchActive(window)
        if (updated != desiredActive) {
            val target = if (desiredActive) "enable" else "disable"
            onLog(attentionLog("Failed to $target CPU Saving Mode for ${character.name}."))
        }
    }

    private fun rejoinPartyWithRetry(
        window: WindowInfo,
        character: CharacterConfig,
        onLog: (LogEntry) -> Unit
    ) {
        var stillElbeland = true
        repeat(REJOIN_MAX_ATTEMPTS) {
            if (!running) {
                return
            }
            partyInteractor.rejoinParty(window)
            Thread.sleep(REJOIN_CHECK_DELAY_MS)
            stillElbeland = currentMapDetector.isElbeland(window)
            if (!stillElbeland) {
                onLog(importantLog("${character.name} left Elbeland after rejoin."))
                return
            }
        }
        if (stillElbeland) {
            onLog(attentionLog("${character.name} still in Elbeland after $REJOIN_MAX_ATTEMPTS attempts."))
        }
    }

    private fun waitForSoloLevel(
        window: WindowInfo,
        character: CharacterConfig,
        targetLevel: Int,
        onLog: (LogEntry) -> Unit,
        onStats: (String, CharacterStats) -> Unit
    ): Boolean {
        onLog(infoLog("Waiting ${character.name} reach level $targetLevel..."))
        while (running) {
            val title = windowFinder.getWindowTitle(window.handle)
            val parsed = parseStats(title)
            if (parsed != null) {
                val (name, stats) = parsed
                onStats(name, stats)
                if (stats.level >= targetLevel) {
                    onLog(importantLog("${character.name} reached level ${stats.level} (target $targetLevel)."))
                    return true
                }
            }
            Thread.sleep(SOLO_LEVEL_POLL_MS)
        }
        return false
    }

    private fun infoLog(message: String) = LogEntry(message, LogType.INFO)

    private fun importantLog(message: String) = LogEntry(message, LogType.IMPORTANT)

    private fun attentionLog(message: String) = LogEntry(message, LogType.ATTENTION)

    private fun basePoints(character: CharacterConfig): Map<AttributeType, Int> {
        return mapOf(
            AttributeType.STR to character.str,
            AttributeType.AGI to character.agi,
            AttributeType.STA to character.sta,
            AttributeType.ENE to character.ene,
            AttributeType.CMD to character.cmd
        )
    }

    private fun windowPrefix(name: String): String {
        return "GlobalMuOnline - Powered by IGCN - Name: [$name]"
    }

    private fun waitForUserIdle(onLog: (LogEntry) -> Unit): Boolean {
        var notified = false
        while (running) {
            if (idleMonitor.idleMillis() >= USER_IDLE_MS || BotInputTracker.isRecent(BOT_INPUT_IGNORE_MS)) {
                return true
            }
            if (!notified) {
                onLog(infoLog("User activity detected, waiting for ${USER_IDLE_MS / 1000}s idle."))
                notified = true
            }
            Thread.sleep(IDLE_POLL_MS)
        }
        return false
    }

    private fun updateWindowStatus(
        character: CharacterConfig,
        hasWindows: Boolean,
        onLog: (LogEntry) -> Unit,
        onStatus: (String, Boolean) -> Unit
    ) {
        onStatus(character.name, hasWindows)
        if (!hasWindows) {
            onLog(attentionLog("No window found for ${character.name}"))
        }
    }

    private fun normalizeSeconds(value: Int, defaultValue: Int): Int {
        return if (value > 0) value else defaultValue
    }

    private companion object {
        private const val RESET_LEVEL = 400
        private const val DEFAULT_CHECK_INTERVAL_SECONDS = 60
        private const val DEFAULT_TELEPORT_WAIT_SECONDS = 30
        private const val USER_IDLE_MS = 30_000L
        private const val IDLE_POLL_MS = 1_000L
        private const val COMMAND_DELAY_MS = 500L
        private const val BOT_INPUT_IGNORE_MS = 2_500L
        private const val SOLO_LEVEL_POLL_MS = 3_000L
        private const val REJOIN_MAX_ATTEMPTS = 3
        private const val REJOIN_CHECK_DELAY_MS = 2_000L
        private const val REJOIN_AFTER_SOLO_DELAY_MS = 2_000L
        private const val HUNT_TOGGLE_MAX_ATTEMPTS = 3
        private const val HUNT_TOGGLE_DELAY_MS = 800L
        private const val SWITCH_TOGGLE_DELAY_MS = 900L
        private const val DEBUG_CURSOR_POLL_MS = 5_000L
        private const val OVERFLOW_CAP = 32_600
        private val TITLE_REGEX = Regex(
            """Name:\s*\[(.+?)]\s*Level:\s*\[(\d+)]\s*Master Level:\s*\[(\d+)]\s*Resets:\s*\[(\d+)]"""
        )
        private val ATTRIBUTE_COMMANDS = listOf(
            AttributeType.STR to "/addstr",
            AttributeType.AGI to "/addagi",
            AttributeType.STA to "/addvit",
            AttributeType.ENE to "/addene",
            AttributeType.CMD to "/addcmd"
        )
    }
}
