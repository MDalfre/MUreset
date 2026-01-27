package io.github.mdalfre.composable

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.mdalfre.model.LogType

@Composable
fun LogsPane(
    state: LogsState,
    logListState: LazyListState,
    onClear: () -> Unit,
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
                text = "Logs",
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary
            )
            StyledButton(
                text = "Clear logs",
                onClick = onClear,
                enabled = state.logs.isNotEmpty(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFE06C75)
                )
            )
        }
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
            if (state.logs.isEmpty()) {
                Text(
                    text = "No logs yet.",
                    modifier = Modifier.padding(16.dp),
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            } else {
                LaunchedEffect(state.logs.size) {
                    logListState.animateScrollToItem(state.logs.size - 1)
                }
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(420.dp)
                        .padding(8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                    state = logListState
                ) {
                    items(state.logs) { entry ->
                        val color = when (entry.type) {
                            LogType.ATTENTION -> Color(0xFFE06C75)
                            LogType.IMPORTANT -> Color(0xFFF2C94C)
                            LogType.INFO -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.85f)
                        }
                        Text(
                            text = entry.message,
                            fontSize = 12.sp,
                            color = color
                        )
                    }
                }
            }
        }
    }
}
