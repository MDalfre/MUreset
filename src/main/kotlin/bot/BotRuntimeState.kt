package io.github.mdalfre.bot

import java.awt.image.BufferedImage
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArrayList
import io.github.mdalfre.model.CharacterConfig
import io.github.mdalfre.model.CharacterStats
import io.github.mdalfre.model.LogEntry

object BotRuntimeState {
    @Volatile
    var isRunning: Boolean = false

    @Volatile
    var activeName: String? = null

    private val logs = CopyOnWriteArrayList<LogEntry>()
    private val statsByName = ConcurrentHashMap<String, CharacterStats>()
    private val characters = CopyOnWriteArrayList<CharacterConfig>()
    private val screenshots = ConcurrentHashMap<String, BufferedImage>()

    fun getLogs(limit: Int = 200): List<LogEntry> {
        val size = logs.size
        if (size <= limit) {
            return logs.toList()
        }
        return logs.subList(size - limit, size).toList()
    }

    fun addLog(entry: LogEntry) {
        logs.add(entry)
        if (logs.size > 500) {
            repeat(logs.size - 500) {
                logs.removeAt(0)
            }
        }
    }

    fun setStats(name: String, stats: CharacterStats) {
        statsByName[name] = stats
    }

    fun getStats(): Map<String, CharacterStats> = statsByName.toMap()

    fun setCharacters(list: List<CharacterConfig>) {
        characters.clear()
        characters.addAll(list)
    }

    fun getCharacters(): List<CharacterConfig> = characters.toList()

    fun setScreenshot(name: String, image: BufferedImage) {
        screenshots[name] = image
    }

    fun getScreenshot(name: String): BufferedImage? = screenshots[name]
}
