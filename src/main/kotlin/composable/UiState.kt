package org.example.composable

import org.example.model.AttributeType
import org.example.model.CharacterConfig
import org.example.model.CharacterStats
import org.example.model.LogEntry

data class SettingsState(
    val checkIntervalSeconds: String,
    val teleportWaitSeconds: String,
    val showForm: Boolean,
    val isRunning: Boolean
)

data class CharacterFormState(
    val name: String,
    val str: String,
    val agi: String,
    val sta: String,
    val ene: String,
    val cmd: String,
    val pointsPerReset: String,
    val soloLevel: String,
    val warpMapLabel: String,
    val overflowAttribute: AttributeType?,
    val errorMessage: String?,
    val isRunning: Boolean,
    val canSubmit: Boolean,
    val showCancel: Boolean
)

data class CharacterListState(
    val characters: List<CharacterConfig>,
    val statsByName: Map<String, CharacterStats>,
    val statusByName: Map<String, Boolean>,
    val activeName: String?,
    val editingIndex: Int?,
    val isRunning: Boolean
)

data class LogsState(
    val logs: List<LogEntry>
)
