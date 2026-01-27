package io.github.mdalfre.composable

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import io.github.mdalfre.model.CharacterConfig

@Composable
fun ConfirmDialogs(
    pendingDelete: CharacterConfig?,
    pendingClear: Boolean,
    pendingUpdate: CharacterConfig?,
    pendingUpdateIndex: Int?,
    onDismissDelete: () -> Unit,
    onConfirmDelete: () -> Unit,
    onDismissClear: () -> Unit,
    onConfirmClear: () -> Unit,
    onDismissUpdate: () -> Unit,
    onConfirmUpdate: () -> Unit
) {
    if (pendingDelete != null) {
        AlertDialog(
            onDismissRequest = onDismissDelete,
            title = { Text("Confirm removal") },
            text = { Text("Remove ${pendingDelete.name} from the run list?") },
            confirmButton = {
                StyledButton(
                    text = "Remove",
                    onClick = onConfirmDelete
                )
            },
            dismissButton = {
                StyledButton(
                    text = "Cancel",
                    onClick = onDismissDelete
                )
            }
        )
    }

    if (pendingClear) {
        AlertDialog(
            onDismissRequest = onDismissClear,
            title = { Text("Confirm clear") },
            text = { Text("Remove all characters from the run list?") },
            confirmButton = {
                StyledButton(
                    text = "Clear",
                    onClick = onConfirmClear
                )
            },
            dismissButton = {
                StyledButton(
                    text = "Cancel",
                    onClick = onDismissClear
                )
            }
        )
    }

    if (pendingUpdate != null && pendingUpdateIndex != null) {
        AlertDialog(
            onDismissRequest = onDismissUpdate,
            title = { Text("Confirm update") },
            text = { Text("Overwrite this character's data?") },
            confirmButton = {
                StyledButton(
                    text = "Update",
                    onClick = onConfirmUpdate
                )
            },
            dismissButton = {
                StyledButton(
                    text = "Cancel",
                    onClick = onDismissUpdate
                )
            }
        )
    }
}
