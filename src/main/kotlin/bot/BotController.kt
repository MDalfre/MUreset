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
import io.github.mdalfre.bot.vision.PartyInteractor
import io.github.mdalfre.bot.vision.HuntModeDetector
import io.github.mdalfre.bot.vision.MapWarpInteractor
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

    fun start(
        characters: List<CharacterConfig>,
        onLog: (LogEntry) -> Unit = {},
        onStats: (String, CharacterStats) -> Unit = { _, _ -> },
        onStatus: (String, Boolean) -> Unit = { _, _ -> },
        onActive: (String?) -> Unit = {},
        checkIntervalSeconds: Int = 60,
        teleportWaitSeconds: Int = 30,
        onComplete: () -> Unit = {}
    ) {
        if (running) {
            return
        }
        running = true
        Thread {
            OpenCVBootstrap.init()
            try {
                runBot(characters, onLog, onStats, onStatus, onActive, checkIntervalSeconds, teleportWaitSeconds)
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
        teleportWaitSeconds: Int
    ) {
        var cycle = 0
        val intervalSeconds = if (checkIntervalSeconds > 0) checkIntervalSeconds else 60
        val huntWaitTimeoutMs = (if (teleportWaitSeconds > 0) teleportWaitSeconds else 30) * 1000L
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
                    onActive = onActive
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
        onActive: (String?) -> Unit
    ): Boolean {
        onActive(character.name)
        try {
            val windows = windowFinder.findWindowsByPrefix(windowPrefix(character.name))
            if (windows.isEmpty()) {
                onLog(attentionLog("No window found for ${character.name}"))
                onStatus(character.name, false)
            } else {
                onStatus(character.name, true)
            }
            for (window in windows) {
                if (!running) {
                    return false
                }
                onLog(infoLog("Selecting: ${character.name}"))
                val (name, stats) = parseStats(window.title) ?: continue
                onStats(name, stats)
                onLog(infoLog("${character.name} Level: ${stats.level} Master Level: ${stats.masterLevel} Resets: ${stats.resets}"))
                if (stats.level == RESET_LEVEL) {
                    if (resetThisCycle.add(character.name)) {
                        performResetRoutine(window, character, stats, onLog)
                        val soloOk = runSoloLeveling(window, character, onLog, onStats)
                        if (!soloOk) {
                            return false
                        }
                        partyInteractor.clickOtherPartyMember(window)
                        Thread.sleep(500)
                        val huntOk = huntModeDetector.waitForHuntMode(window, huntWaitTimeoutMs, onLog = onLog)
                        if (!huntOk) {
                            onLog(attentionLog("Hunt mode not detected for ${character.name}."))
                        } else {
                            onLog(importantLog("${character.name} returned to hunt mode."))
                            onLog(importantLog("${character.name} reset complete -> Resets: ${stats.resets + 1}"))
                        }
                        continue
                    } else {
                        onLog(infoLog("Reset already executed this cycle for ${character.name}"))
                    }
                } else {
                    onLog(infoLog("${character.name} waiting for level ${RESET_LEVEL}..."))
                }
                windowActions.focus(window)
            }
            return true
        } finally {
            onActive(null)
        }
    }

    private fun performResetRoutine(
        window: WindowInfo,
        character: CharacterConfig,
        stats: CharacterStats,
        onLog: (LogEntry) -> Unit
    ) {
        val totalPoints = stats.resets * character.pointsPerReset
        if (totalPoints <= 0) {
            onLog(attentionLog("Invalid total points for ${character.name}"))
            return
        }
        val basePoints = basePoints(character)
        val overflowAttr = character.overflowAttribute
        val usedPoints = basePoints.entries
            .filter { it.key != overflowAttr }
            .sumOf { it.value }
        val overflowPoints = totalPoints - usedPoints
        if (overflowPoints < 0) {
            onLog(attentionLog("Configured points exceed available total for ${character.name}"))
            return
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
        val warpOk = mapWarpInteractor.warpToMap(window, character.warpMap, onLog)
        if (!warpOk) {
            onLog(attentionLog("Warp to Elbeland 3 failed for ${character.name}."))
            return true
        }
        windowActions.sendKey(KeyEvent.VK_HOME)
        Thread.sleep(300)
        return waitForSoloLevel(window, character, targetLevel, onLog, onStats)
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
            if (idleMonitor.idleMillis() >= USER_IDLE_MS) {
                return true
            }
            if (BotInputTracker.isRecent(BOT_INPUT_IGNORE_MS)) {
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

    private companion object {
        private const val RESET_LEVEL = 400
        private const val USER_IDLE_MS = 30_000L
        private const val IDLE_POLL_MS = 1_000L
        private const val COMMAND_DELAY_MS = 500L
        private const val BOT_INPUT_IGNORE_MS = 2_500L
        private const val SOLO_LEVEL_POLL_MS = 3_000L
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
