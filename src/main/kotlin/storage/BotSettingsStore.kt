package io.github.mdalfre.storage

import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

data class BotSettings(
    val checkIntervalSeconds: Int,
    val teleportWaitSeconds: Int,
    val cpuSavingMode: Boolean
)

object BotSettingsStore {
    private val filePath: Path = Paths.get(
        System.getProperty("user.home"),
        "mureset-settings.cfg"
    )
    private const val DEFAULT_CHECK_INTERVAL = 60
    private const val DEFAULT_TELEPORT_WAIT = 30
    private const val DEFAULT_CPU_SAVING = false

    fun load(): BotSettings {
        if (!Files.exists(filePath)) {
            return BotSettings(DEFAULT_CHECK_INTERVAL, DEFAULT_TELEPORT_WAIT, DEFAULT_CPU_SAVING)
        }
        val line = Files.readAllLines(filePath, StandardCharsets.UTF_8).firstOrNull().orEmpty()
        val parts = line.split("|")
        val check = parts.getOrNull(0)?.toIntOrNull() ?: DEFAULT_CHECK_INTERVAL
        val teleport = parts.getOrNull(1)?.toIntOrNull() ?: DEFAULT_TELEPORT_WAIT
        val cpuSaving = parts.getOrNull(2)?.toBooleanStrictOrNull() ?: DEFAULT_CPU_SAVING
        return BotSettings(check, teleport, cpuSaving)
    }

    fun save(settings: BotSettings) {
        val line = "${settings.checkIntervalSeconds}|${settings.teleportWaitSeconds}|${settings.cpuSavingMode}"
        Files.write(filePath, listOf(line), StandardCharsets.UTF_8)
    }
}
