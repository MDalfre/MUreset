package org.example.composable

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.ButtonDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun SettingsBar(
    state: SettingsState,
    onCheckIntervalChange: (String) -> Unit,
    onTeleportWaitChange: (String) -> Unit,
    onToggleForm: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        StyledTextField(
            label = "Check interval (s)",
            value = state.checkIntervalSeconds,
            onValueChange = onCheckIntervalChange,
            modifier = Modifier.weight(1f),
            inputModifier = Modifier.fillMaxWidth(),
            enabled = !state.isRunning
        )
        StyledTextField(
            label = "Teleport wait time (s)",
            value = state.teleportWaitSeconds,
            onValueChange = onTeleportWaitChange,
            modifier = Modifier.weight(1f),
            inputModifier = Modifier.fillMaxWidth(),
            enabled = !state.isRunning
        )
        StyledButton(
            text = if (state.showForm) "Hide form" else "Add character",
            onClick = onToggleForm,
            enabled = !state.isRunning,
            colors = ButtonDefaults.buttonColors()
        )
    }
}
