package io.github.mdalfre.storage

import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

data class BotSettings(
    val checkIntervalSeconds: Int,
    val teleportWaitSeconds: Int
)

object BotSettingsStore {
    private val filePath: Path = Paths.get(
        System.getProperty("user.home"),
        "mureset-settings.cfg"
    )
    private const val DEFAULT_CHECK_INTERVAL = 60
    private const val DEFAULT_TELEPORT_WAIT = 30

    fun load(): BotSettings {
        if (!Files.exists(filePath)) {
            return BotSettings(DEFAULT_CHECK_INTERVAL, DEFAULT_TELEPORT_WAIT)
        }
        val line = Files.readAllLines(filePath, StandardCharsets.UTF_8).firstOrNull().orEmpty()
        val parts = line.split("|")
        val check = parts.getOrNull(0)?.toIntOrNull() ?: DEFAULT_CHECK_INTERVAL
        val teleport = parts.getOrNull(1)?.toIntOrNull() ?: DEFAULT_TELEPORT_WAIT
        return BotSettings(check, teleport)
    }

    fun save(settings: BotSettings) {
        val line = "${settings.checkIntervalSeconds}|${settings.teleportWaitSeconds}"
        Files.write(filePath, listOf(line), StandardCharsets.UTF_8)
    }
}
