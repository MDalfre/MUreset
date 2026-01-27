package io.github.mdalfre.composable

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Surface
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isShiftPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import io.github.mdalfre.bot.BotController
import io.github.mdalfre.model.AttributeType
import io.github.mdalfre.model.CharacterConfig
import io.github.mdalfre.model.CharacterStats
import io.github.mdalfre.model.LogEntry
import io.github.mdalfre.model.WarpMap
import io.github.mdalfre.storage.CharacterConfigStore
import java.awt.EventQueue

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun App() {
    val scheme = darkColorScheme(
        background = Color(0xFF0C0A08),
        surface = Color(0xFF15110C),
        primary = Color(0xFFC8A24A),
        onPrimary = Color(0xFF1A1307),
        secondary = Color(0xFF7E6533),
        tertiary = Color(0xFF3F5A3B),
        onSurface = Color(0xFFF2EAD3),
        onBackground = Color(0xFFEFE6CF)
    )
    val typography = Typography().run {
        copy(
            displayLarge = displayLarge.copy(fontFamily = FontFamily.Cursive),
            displayMedium = displayMedium.copy(fontFamily = FontFamily.Cursive),
            displaySmall = displaySmall.copy(fontFamily = FontFamily.Cursive),
            headlineLarge = headlineLarge.copy(fontFamily = FontFamily.Serif),
            headlineMedium = headlineMedium.copy(fontFamily = FontFamily.Serif),
            headlineSmall = headlineSmall.copy(fontFamily = FontFamily.Serif),
            titleLarge = titleLarge.copy(fontFamily = FontFamily.Serif),
            titleMedium = titleMedium.copy(fontFamily = FontFamily.Serif),
            titleSmall = titleSmall.copy(fontFamily = FontFamily.Serif),
            bodyLarge = bodyLarge.copy(fontFamily = FontFamily.Monospace),
            bodyMedium = bodyMedium.copy(fontFamily = FontFamily.Monospace),
            bodySmall = bodySmall.copy(fontFamily = FontFamily.Monospace),
            labelLarge = labelLarge.copy(fontFamily = FontFamily.Monospace),
            labelMedium = labelMedium.copy(fontFamily = FontFamily.Monospace),
            labelSmall = labelSmall.copy(fontFamily = FontFamily.Monospace)
        )
    }
    val shapes = Shapes(
        extraSmall = RoundedCornerShape(6.dp),
        small = RoundedCornerShape(8.dp),
        medium = RoundedCornerShape(10.dp),
        large = RoundedCornerShape(12.dp)
    )
    val backgroundBrush = Brush.linearGradient(
        colors = listOf(
            Color(0xFF080705),
            Color(0xFF15100B),
            Color(0xFF0D1411)
        ),
        start = Offset(0f, 0f),
        end = Offset(1200f, 900f)
    )

    MaterialTheme(colorScheme = scheme, typography = typography, shapes = shapes) {
        Surface(modifier = Modifier.fillMaxSize()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(backgroundBrush)
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                val botController = remember { BotController() }
                val characters = remember {
                    mutableStateListOf<CharacterConfig>().apply {
                        addAll(CharacterConfigStore.load())
                    }
                }
                val statsByName = remember { mutableStateMapOf<String, CharacterStats>() }
                val statusByName = remember { mutableStateMapOf<String, Boolean>() }
                val logs = remember { mutableStateListOf<LogEntry>() }
                val logListState = rememberLazyListState()
                var activeName by remember { mutableStateOf<String?>(null) }

                var isRunning by remember { mutableStateOf(false) }
                var name by remember { mutableStateOf("") }
                var str by remember { mutableStateOf("0") }
                var agi by remember { mutableStateOf("0") }
                var sta by remember { mutableStateOf("0") }
                var ene by remember { mutableStateOf("0") }
                var cmd by remember { mutableStateOf("0") }
                var warpMap by remember { mutableStateOf(WarpMap.ELBELAND_3) }
                var pointsPerReset by remember { mutableStateOf("") }
                var soloLevel by remember { mutableStateOf("30") }
                var overflowAttribute by remember { mutableStateOf<AttributeType?>(null) }
                var editingIndex by remember { mutableStateOf<Int?>(null) }
                var pendingUpdate by remember { mutableStateOf<CharacterConfig?>(null) }
                var pendingUpdateIndex by remember { mutableStateOf<Int?>(null) }
                var pendingDelete by remember { mutableStateOf<CharacterConfig?>(null) }
                var pendingClear by remember { mutableStateOf(false) }
                var errorMessage by remember { mutableStateOf<String?>(null) }
                var checkIntervalSeconds by remember { mutableStateOf("60") }
                var teleportWaitSeconds by remember { mutableStateOf("30") }
                var showForm by remember { mutableStateOf(false) }

                fun resetForm(clearError: Boolean = true) {
                    if (clearError) {
                        errorMessage = null
                    }
                    name = ""
                    str = "0"
                    agi = "0"
                    sta = "0"
                    ene = "0"
                    cmd = "0"
                    warpMap = WarpMap.ELBELAND_3
                    pointsPerReset = ""
                    soloLevel = "30"
                    overflowAttribute = null
                    editingIndex = null
                }

                fun fillForm(character: CharacterConfig, index: Int) {
                    errorMessage = null
                    name = character.name
                    str = character.str.toString()
                    agi = character.agi.toString()
                    sta = character.sta.toString()
                    ene = character.ene.toString()
                    cmd = character.cmd.toString()
                    warpMap = character.warpMap
                    pointsPerReset = character.pointsPerReset.toString()
                    soloLevel = character.soloLevel.toString()
                    overflowAttribute = character.overflowAttribute
                    editingIndex = index
                    showForm = true
                }

                val pointsValue = pointsPerReset.toIntOrNull() ?: 0
                val soloLevelValue = soloLevel.toIntOrNull() ?: 0
                val canAdd = !isRunning && name.isNotBlank() && pointsValue > 0 && overflowAttribute != null
                val focusManager = LocalFocusManager.current
                val handleTab: (KeyEvent) -> Boolean = { event: KeyEvent ->
                    if (event.type == KeyEventType.KeyDown && event.key == Key.Tab) {
                        if (event.isShiftPressed) {
                            focusManager.moveFocus(FocusDirection.Previous)
                        } else {
                            focusManager.moveFocus(FocusDirection.Next)
                        }
                        true
                    } else {
                        false
                    }
                }
                val nameFocus = remember { FocusRequester() }
                val strFocus = remember { FocusRequester() }
                val agiFocus = remember { FocusRequester() }
                val staFocus = remember { FocusRequester() }
                val eneFocus = remember { FocusRequester() }
                val cmdFocus = remember { FocusRequester() }
                val pointsFocus = remember { FocusRequester() }
                val soloLevelFocus = remember { FocusRequester() }
                val mapsFocus = remember { FocusRequester() }
                val addFocus = remember { FocusRequester() }

                Column(
                    modifier = Modifier.width(1100.dp),
                    verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(16.dp)
                ) {
                    RunningBanner(isRunning = isRunning)

                    HeaderBar(
                        isRunning = isRunning,
                        onStartStop = {
                            if (isRunning) {
                                botController.stop()
                                isRunning = false
                            } else {
                                isRunning = true
                                botController.start(
                                    characters = characters.filter { it.active },
                                    onLog = { entry ->
                                        EventQueue.invokeLater {
                                            logs.add(
                                                LogEntry(
                                                    message = "${timestampPrefix()} ${entry.message}",
                                                    type = entry.type
                                                )
                                            )
                                            if (logs.size > 200) {
                                                logs.removeRange(0, logs.size - 200)
                                            }
                                        }
                                    },
                                    onStats = { characterName, stats ->
                                        EventQueue.invokeLater { statsByName[characterName] = stats }
                                    },
                                    onStatus = { characterName, found ->
                                        EventQueue.invokeLater { statusByName[characterName] = found }
                                    },
                                    onActive = { name ->
                                        EventQueue.invokeLater { activeName = name }
                                    },
                                    checkIntervalSeconds = checkIntervalSeconds.toIntOrNull() ?: 60,
                                    teleportWaitSeconds = teleportWaitSeconds.toIntOrNull() ?: 30,
                                    onComplete = {
                                        EventQueue.invokeLater {
                                            isRunning = false
                                            activeName = null
                                        }
                                    }
                                )
                            }
                        }
                    )

                    SettingsBar(
                        state = SettingsState(
                            checkIntervalSeconds = checkIntervalSeconds,
                            teleportWaitSeconds = teleportWaitSeconds,
                            showForm = showForm,
                            isRunning = isRunning
                        ),
                        onCheckIntervalChange = { value -> checkIntervalSeconds = digitsOnly(value) },
                        onTeleportWaitChange = { value -> teleportWaitSeconds = digitsOnly(value) },
                        onToggleForm = { showForm = !showForm }
                    )

                    if (showForm) {
                        CharacterFormCard(
                            state = CharacterFormState(
                                name = name,
                                str = str,
                                agi = agi,
                                sta = sta,
                                ene = ene,
                                cmd = cmd,
                                pointsPerReset = pointsPerReset,
                                soloLevel = soloLevel,
                                warpMapLabel = warpMap.label,
                                overflowAttribute = overflowAttribute,
                                errorMessage = errorMessage,
                                isRunning = isRunning,
                                canSubmit = canAdd,
                                showCancel = editingIndex != null
                            ),
                            nameFocus = nameFocus,
                            strFocus = strFocus,
                            agiFocus = agiFocus,
                            staFocus = staFocus,
                            eneFocus = eneFocus,
                            cmdFocus = cmdFocus,
                            pointsFocus = pointsFocus,
                            soloLevelFocus = soloLevelFocus,
                            mapsFocus = mapsFocus,
                            addFocus = addFocus,
                            handleTab = handleTab,
                            onNameChange = { value -> name = value.filter { c -> !c.isWhitespace() } },
                            onStrChange = { value -> str = digitsOnly(value) },
                            onAgiChange = { value -> agi = digitsOnly(value) },
                            onStaChange = { value -> sta = digitsOnly(value) },
                            onEneChange = { value -> ene = digitsOnly(value) },
                            onCmdChange = { value -> cmd = digitsOnly(value) },
                            onPointsPerResetChange = { value -> pointsPerReset = digitsOnly(value) },
                            onSoloLevelChange = { value -> soloLevel = digitsOnly(value) },
                            onWarpMapChange = { warpMap = it },
                            onOverflowChange = { overflowAttribute = it },
                            onSubmit = {
                                errorMessage = null
                                val overflow = overflowAttribute
                                if (canAdd && overflow != null) {
                                    val existingActive = editingIndex?.let { index ->
                                        characters.getOrNull(index)?.active
                                    } ?: true
                                    val updated = CharacterConfig(
                                        name = name.trim(),
                                        str = str.toIntOrNull() ?: 0,
                                        agi = agi.toIntOrNull() ?: 0,
                                        sta = sta.toIntOrNull() ?: 0,
                                        ene = ene.toIntOrNull() ?: 0,
                                        cmd = cmd.toIntOrNull() ?: 0,
                                        warpMap = warpMap,
                                        pointsPerReset = pointsValue,
                                        overflowAttribute = overflow,
                                        soloLevel = soloLevelValue,
                                        active = existingActive
                                    )
                                    val normalized = updated.name.lowercase()
                                    val duplicateIndex = characters.indexOfFirst {
                                        it.name.trim().lowercase() == normalized
                                    }
                                    val index = editingIndex
                                    val hasDuplicate = duplicateIndex >= 0 && duplicateIndex != index
                                    if (hasDuplicate) {
                                        errorMessage = "A character with this name already exists."
                                        return@CharacterFormCard
                                    }
                                    if (index == null) {
                                        characters.add(updated)
                                        CharacterConfigStore.save(characters)
                                        resetForm(clearError = false)
                                    } else if (index in characters.indices) {
                                        pendingUpdate = updated
                                        pendingUpdateIndex = index
                                        return@CharacterFormCard
                                    }
                                }
                            },
                            onCancel = {
                                resetForm()
                            }
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(16.dp)
                    ) {
                        CharacterListPane(
                            state = CharacterListState(
                                characters = characters,
                                statsByName = statsByName,
                                statusByName = statusByName,
                                activeName = activeName,
                                editingIndex = editingIndex,
                                isRunning = isRunning
                            ),
                            onClear = { pendingClear = true },
                            onDelete = { pendingDelete = it },
                            onEdit = { character ->
                                val index = characters.indexOf(character)
                                if (index >= 0) {
                                    fillForm(character, index)
                                }
                            },
                            onToggleActive = { character, active ->
                                val index = characters.indexOf(character)
                                if (index in characters.indices) {
                                    characters[index] = character.copy(active = active)
                                    CharacterConfigStore.save(characters)
                                }
                            },
                            modifier = Modifier.weight(1.1f)
                        )

                        LogsPane(
                            state = LogsState(logs = logs),
                            logListState = logListState,
                            onClear = { logs.clear() },
                            modifier = Modifier.weight(0.9f)
                        )
                    }

                    ConfirmDialogs(
                        pendingDelete = pendingDelete,
                        pendingClear = pendingClear,
                        pendingUpdate = pendingUpdate,
                        pendingUpdateIndex = pendingUpdateIndex,
                        onDismissDelete = { pendingDelete = null },
                        onConfirmDelete = {
                            pendingDelete?.let { characters.remove(it) }
                            pendingDelete?.let { statsByName.remove(it.name) }
                            pendingDelete?.let { statusByName.remove(it.name) }
                            CharacterConfigStore.save(characters)
                            pendingDelete = null
                        },
                        onDismissClear = { pendingClear = false },
                        onConfirmClear = {
                            characters.clear()
                            statsByName.clear()
                            statusByName.clear()
                            CharacterConfigStore.save(characters)
                            pendingClear = false
                        },
                        onDismissUpdate = {
                            pendingUpdate = null
                            pendingUpdateIndex = null
                        },
                        onConfirmUpdate = {
                            val index = pendingUpdateIndex ?: -1
                            if (index in characters.indices) {
                                characters[index] = pendingUpdate ?: characters[index]
                                CharacterConfigStore.save(characters)
                                resetForm(clearError = false)
                            }
                            pendingUpdate = null
                            pendingUpdateIndex = null
                        }
                    )
                }
            }
        }
    }
}
