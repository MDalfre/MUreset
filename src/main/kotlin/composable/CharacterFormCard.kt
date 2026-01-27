package io.github.mdalfre.composable

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.mdalfre.model.AttributeType
import io.github.mdalfre.model.WarpMap

@Composable
fun CharacterFormCard(
    state: CharacterFormState,
    nameFocus: FocusRequester,
    strFocus: FocusRequester,
    agiFocus: FocusRequester,
    staFocus: FocusRequester,
    eneFocus: FocusRequester,
    cmdFocus: FocusRequester,
    pointsFocus: FocusRequester,
    soloLevelFocus: FocusRequester,
    mapsFocus: FocusRequester,
    addFocus: FocusRequester,
    handleTab: (KeyEvent) -> Boolean,
    onNameChange: (String) -> Unit,
    onStrChange: (String) -> Unit,
    onAgiChange: (String) -> Unit,
    onStaChange: (String) -> Unit,
    onEneChange: (String) -> Unit,
    onCmdChange: (String) -> Unit,
    onPointsPerResetChange: (String) -> Unit,
    onSoloLevelChange: (String) -> Unit,
    onWarpMapChange: (WarpMap) -> Unit,
    onOverflowChange: (AttributeType) -> Unit,
    onSubmit: () -> Unit,
    onCancel: () -> Unit
) {
    val cardShape = RoundedCornerShape(10.dp)
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, MaterialTheme.colorScheme.secondary.copy(alpha = 0.6f), cardShape),
        shape = cardShape,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f)
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                StyledTextField(
                    label = "Name",
                    value = state.name,
                    onValueChange = onNameChange,
                    modifier = Modifier.weight(1f),
                    inputModifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(nameFocus)
                        .focusProperties { next = strFocus }
                        .onPreviewKeyEvent(handleTab),
                    enabled = !state.isRunning
                )
                StyledTextField(
                    label = "Str",
                    value = state.str,
                    onValueChange = onStrChange,
                    modifier = Modifier.weight(1f),
                    inputModifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(strFocus)
                        .focusProperties { next = agiFocus }
                        .onPreviewKeyEvent(handleTab),
                    enabled = !state.isRunning
                )
                StyledTextField(
                    label = "Agi",
                    value = state.agi,
                    onValueChange = onAgiChange,
                    modifier = Modifier.weight(1f),
                    inputModifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(agiFocus)
                        .focusProperties { next = staFocus }
                        .onPreviewKeyEvent(handleTab),
                    enabled = !state.isRunning
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                StyledTextField(
                    label = "Sta",
                    value = state.sta,
                    onValueChange = onStaChange,
                    modifier = Modifier.weight(1f),
                    inputModifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(staFocus)
                        .focusProperties { next = eneFocus }
                        .onPreviewKeyEvent(handleTab),
                    enabled = !state.isRunning
                )
                StyledTextField(
                    label = "Ene",
                    value = state.ene,
                    onValueChange = onEneChange,
                    modifier = Modifier.weight(1f),
                    inputModifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(eneFocus)
                        .focusProperties { next = cmdFocus }
                        .onPreviewKeyEvent(handleTab),
                    enabled = !state.isRunning
                )
                StyledTextField(
                    label = "Cmd",
                    value = state.cmd,
                    onValueChange = onCmdChange,
                    modifier = Modifier.weight(1f),
                    inputModifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(cmdFocus)
                        .focusProperties { next = pointsFocus }
                        .onPreviewKeyEvent(handleTab),
                    enabled = !state.isRunning
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                StyledTextField(
                    label = "Points/Reset",
                    value = state.pointsPerReset,
                    onValueChange = onPointsPerResetChange,
                    modifier = Modifier.weight(1f),
                    inputModifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(pointsFocus)
                        .focusProperties { next = soloLevelFocus }
                        .onPreviewKeyEvent(handleTab),
                    enabled = !state.isRunning
                )
                StyledTextField(
                    label = "Solo level",
                    value = state.soloLevel,
                    onValueChange = onSoloLevelChange,
                    modifier = Modifier.weight(1f),
                    inputModifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(soloLevelFocus)
                        .focusProperties { next = mapsFocus }
                        .onPreviewKeyEvent(handleTab),
                    enabled = !state.isRunning
                )
                Column(modifier = Modifier.weight(2f)) {
                    Text(
                        text = "Overflow attribute",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        AttributeType.values().forEach { attr ->
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                RadioButton(
                                    selected = state.overflowAttribute == attr,
                                    onClick = { onOverflowChange(attr) },
                                    enabled = !state.isRunning
                                )
                                Text(
                                    when (attr) {
                                        AttributeType.STR -> "Str"
                                        AttributeType.AGI -> "Agi"
                                        AttributeType.STA -> "Sta"
                                        AttributeType.ENE -> "Ene"
                                        AttributeType.CMD -> "Cmd"
                                    }
                                )
                            }
                        }
                    }
                }
            }
            val expandedState = remember { mutableStateOf(false) }
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Solo Leveling warp to",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(mapsFocus)
                        .focusProperties { next = addFocus }
                        .onPreviewKeyEvent(handleTab)
                ) {
                    StyledButton(
                        text = state.warpMapLabel,
                        onClick = { expandedState.value = true },
                        enabled = !state.isRunning
                    )
                }
                DropdownMenu(
                    expanded = expandedState.value,
                    onDismissRequest = { expandedState.value = false }
                ) {
                    WarpMap.values().forEach { map ->
                        DropdownMenuItem(
                            text = { Text(map.label) },
                            onClick = {
                                expandedState.value = false
                                onWarpMapChange(map)
                            }
                        )
                    }
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                StyledButton(
                    text = if (state.showCancel) "Update" else "Add",
                    onClick = onSubmit,
                    enabled = state.canSubmit,
                    modifier = Modifier.focusRequester(addFocus)
                )
                if (state.showCancel) {
                    Spacer(modifier = Modifier.width(8.dp))
                    StyledButton(
                        text = "Cancel",
                        onClick = onCancel,
                        enabled = !state.isRunning
                    )
                }
            }
            if (state.errorMessage != null) {
                Text(
                    text = state.errorMessage,
                    fontSize = 12.sp,
                    color = Color(0xFFE06C75)
                )
            }
        }
    }
}
