package io.github.mdalfre.storage

import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import io.github.mdalfre.model.AttributeType
import io.github.mdalfre.model.CharacterConfig
import io.github.mdalfre.model.WarpMap

object CharacterConfigStore {
    private val filePath: Path = Paths.get(
        System.getProperty("user.home"),
        ".mureset-characters.cfg"
    )
    private const val DEFAULT_SOLO_LEVEL = 30
    private val DEFAULT_WARP_MAP = WarpMap.ELBELAND_3

    fun load(): List<CharacterConfig> {
        if (!Files.exists(filePath)) {
            return emptyList()
        }
        return Files.readAllLines(filePath, StandardCharsets.UTF_8)
            .mapNotNull { parseLine(it) }
    }

    fun save(characters: List<CharacterConfig>) {
        val lines = characters.map { encodeLine(it) }
        Files.write(filePath, lines, StandardCharsets.UTF_8)
    }

    private fun encodeLine(character: CharacterConfig): String {
        return buildString {
            append(escape(character.name))
            append("|")
            append(character.str)
            append("|")
            append(character.agi)
            append("|")
            append(character.sta)
            append("|")
            append(character.ene)
            append("|")
            append(character.cmd)
            append("|")
            append(character.pointsPerReset)
            append("|")
            append(character.overflowAttribute.name)
            append("|")
            append(character.active)
            append("|")
            append(character.soloLevel)
            append("|")
            append(character.warpMap.name)
        }
    }

    private fun parseLine(line: String): CharacterConfig? {
        if (line.isBlank()) {
            return null
        }
        val parts = splitEscaped(line, '|')
        if (parts.size < 7) {
            return null
        }
        val pointsPerReset = parts.getOrNull(6)?.toIntOrNull() ?: 0
        val overflowAttribute = parts.getOrNull(7)?.let { parseAttribute(it) } ?: AttributeType.STR
        val active = if (parts.size >= 10) {
            parseBoolean(parts[8]) ?: true
        } else {
            true
        }
        val soloLevel = if (parts.size >= 11) {
            parts.getOrNull(9)?.toIntOrNull() ?: DEFAULT_SOLO_LEVEL
        } else {
            DEFAULT_SOLO_LEVEL
        }
        val mapsField = when {
            parts.size >= 11 -> parts[10]
            parts.size == 10 -> parts[9]
            parts.size == 9 -> parts[8]
            parts.size == 8 -> parts[7]
            else -> parts.getOrNull(6).orEmpty()
        }
        val warpMap = parseWarpMap(mapsField) ?: DEFAULT_WARP_MAP
        return CharacterConfig(
            name = unescape(parts[0]),
            str = parts[1].toIntOrNull() ?: 0,
            agi = parts[2].toIntOrNull() ?: 0,
            sta = parts[3].toIntOrNull() ?: 0,
            ene = parts[4].toIntOrNull() ?: 0,
            cmd = parts[5].toIntOrNull() ?: 0,
            warpMap = warpMap,
            pointsPerReset = pointsPerReset,
            overflowAttribute = overflowAttribute,
            soloLevel = soloLevel,
            active = active
        )
    }

    private fun parseAttribute(value: String): AttributeType? {
        val trimmed = unescape(value).trim()
        return runCatching { AttributeType.valueOf(trimmed.uppercase()) }.getOrNull()
    }

    private fun escape(value: String): String {
        return value
            .replace("\\", "\\\\")
            .replace("|", "\\|")
            .replace(";", "\\;")
    }

    private fun unescape(value: String): String {
        val out = StringBuilder()
        var escaped = false
        for (char in value) {
            if (escaped) {
                out.append(char)
                escaped = false
            } else if (char == '\\') {
                escaped = true
            } else {
                out.append(char)
            }
        }
        if (escaped) {
            out.append('\\')
        }
        return out.toString()
    }

    private fun splitEscaped(value: String, delimiter: Char): List<String> {
        val parts = mutableListOf<String>()
        val current = StringBuilder()
        var escaped = false
        for (char in value) {
            if (escaped) {
                current.append(char)
                escaped = false
            } else if (char == '\\') {
                escaped = true
            } else if (char == delimiter) {
                parts.add(current.toString())
                current.setLength(0)
            } else {
                current.append(char)
            }
        }
        parts.add(current.toString())
        return parts
    }

    private fun parseBoolean(value: String): Boolean? {
        return when (unescape(value).trim().lowercase()) {
            "true" -> true
            "false" -> false
            else -> null
        }
    }

    private fun parseWarpMap(value: String): WarpMap? {
        val raw = unescape(value).trim()
        if (raw.isEmpty()) {
            return null
        }
        WarpMap.fromName(raw)?.let { return it }
        WarpMap.fromLabel(raw)?.let { return it }
        val first = raw.split(';').firstOrNull()?.trim().orEmpty()
        return WarpMap.fromLabel(first) ?: WarpMap.fromName(first)
    }
}
