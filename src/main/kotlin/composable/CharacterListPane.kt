package io.github.mdalfre.composable

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.Divider
import io.github.mdalfre.model.CharacterConfig

@Composable
fun CharacterListPane(
    state: CharacterListState,
    onClear: () -> Unit,
    onEdit: (CharacterConfig) -> Unit,
    onDelete: (CharacterConfig) -> Unit,
    onToggleActive: (CharacterConfig, Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Config List",
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary
            )
            StyledButton(
                text = "Clear list",
                onClick = onClear,
                enabled = !state.isRunning && state.characters.isNotEmpty(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFE06C75)
                )
            )
        }
        val listShape = RoundedCornerShape(10.dp)
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, MaterialTheme.colorScheme.secondary.copy(alpha = 0.6f), listShape),
            shape = listShape,
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f)
            )
        ) {
            if (state.characters.isEmpty()) {
                Text(
                    text = "No characters registered.",
                    modifier = Modifier.padding(16.dp),
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(420.dp)
                        .padding(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    itemsIndexed(state.characters) { index, character ->
                        val found = state.statusByName[character.name] ?: true
                        val isEditing = state.editingIndex == index
                        val isActive = state.activeName == character.name
                        val isEnabled = character.active
                        val cardColor = when {
                            isActive -> Color(0xFF1E3521)
                            !isEnabled -> Color(0xFF1B120B)
                            !found -> Color(0xFF3A1A18)
                            isEditing -> Color(0xFF2A2215)
                            else -> Color(0xFF1B160F)
                        }
                        val itemShape = RoundedCornerShape(8.dp)
                        Card(
                            shape = itemShape,
                            colors = CardDefaults.cardColors(
                                containerColor = cardColor
                            ),
                            modifier = Modifier.border(
                                0.8.dp,
                                MaterialTheme.colorScheme.secondary.copy(alpha = 0.4f),
                                itemShape
                            )
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = character.name,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            Checkbox(
                                                checked = character.active,
                                                onCheckedChange = { onToggleActive(character, it) },
                                                enabled = !state.isRunning
                                            )
                                            Text(
                                                text = "Active",
                                                fontSize = 12.sp,
                                                color = if (character.active) {
                                                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                                                } else {
                                                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                                                }
                                            )
                                        }
                                        StyledButton(
                                            text = "Edit",
                                            onClick = { onEdit(character) },
                                            enabled = !state.isRunning
                                        )
                                        StyledButton(
                                            text = "Remove",
                                            onClick = { onDelete(character) },
                                            enabled = !state.isRunning,
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = Color(0xFFE06C75)
                                            )
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                if (!found) {
                                    Text(
                                        text = "Not found",
                                        fontSize = 12.sp,
                                        color = Color(0xFFE06C75)
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                }
                                if (!character.active) {
                                    Text(
                                        text = "Inactive",
                                        fontSize = 12.sp,
                                        color = Color(0xFF9C7A4D)
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                }
                                state.statsByName[character.name]?.let { stats ->
                                    Text(
                                        text = "Level ${stats.level} | Master ${stats.masterLevel} | Resets ${stats.resets}",
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                }
                                Text(
                                    text = "Points/Reset ${character.pointsPerReset} | Overflow ${formatAttribute(character.overflowAttribute)}",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.75f)
                                )
                                Text(
                                    text = "Str ${character.str} | Agi ${character.agi} | Sta ${character.sta} | Ene ${character.ene} | Cmd ${character.cmd}",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.75f)
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Divider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.75f))
                                Text(
                                    text = "Solo level until: ${character.soloLevel}",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.75f)
                                )
                                Text(
                                    text = "Warp map: ${character.warpMap.label}",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.75f)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
