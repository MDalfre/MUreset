package org.example.composable

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonColors
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp

@Composable
fun StyledTextField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    inputModifier: Modifier = Modifier,
    enabled: Boolean = true,
    singleLine: Boolean = true
) {
    Column(modifier = modifier) {
        Text(
            text = label,
            style = TextStyle(fontFamily = FontFamily.Monospace),
            color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.85f),
            modifier = Modifier.padding(bottom = 5.dp)
        )
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            enabled = enabled,
            singleLine = singleLine,
            cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
            textStyle = TextStyle(
                color = MaterialTheme.colorScheme.onSurface,
                fontFamily = FontFamily.Monospace
            ),
            modifier = inputModifier,
            decorationBox = { innerTextField ->
                Column(
                    Modifier
                        .fillMaxWidth()
                        .background(
                            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.75f),
                            shape = RoundedCornerShape(5.dp)
                        )
                        .border(
                            width = 0.5.dp,
                            color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.7f),
                            shape = RoundedCornerShape(5.dp)
                        )
                        .height(30.dp)
                        .padding(horizontal = 8.dp, vertical = 6.dp)
                ) {
                    innerTextField()
                }
            }
        )
    }
}

@Composable
fun StyledButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    emphasis: Boolean = false,
    colors: ButtonColors = ButtonDefaults.buttonColors(
        containerColor = MaterialTheme.colorScheme.primary,
        contentColor = MaterialTheme.colorScheme.onPrimary
    )
) {
    val buttonShape = RoundedCornerShape(4.dp)
    val innerBorder = when {
        !enabled -> Color(0xFF3A2714)
        emphasis -> Color(0xFF8A5E28)
        else -> Color(0xFF6A4A1F)
    }
    val bezel = Brush.verticalGradient(
        colors = listOf(
            Color(0xFF2C1B0E),
            Color(0xFF1F140A),
            Color(0xFF151009)
        )
    )
    val innerFill = if (emphasis) {
        Brush.verticalGradient(
            colors = listOf(
                Color(0xFF3A2A15),
                Color(0xFF24170C),
                Color(0xFF1A1008)
            )
        )
    } else {
        Brush.verticalGradient(
            colors = listOf(
                Color(0xFF2F2112),
                Color(0xFF20150B),
                Color(0xFF171008)
            )
        )
    }
    val textColor = when {
        !enabled -> Color(0xFF7A6440)
        emphasis -> Color(0xFFF0E0B0)
        else -> Color(0xFFE7D7A4)
    }
    val minHeight = if (emphasis) 30.dp else 26.dp
    val horizontalPadding = if (emphasis) 18.dp else 14.dp
    Button(
        onClick = onClick,
        enabled = enabled,
        colors = ButtonDefaults.buttonColors(
            containerColor = Color.Transparent,
            contentColor = MaterialTheme.colorScheme.onPrimary
        ),
        elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp, pressedElevation = 0.dp),
        shape = buttonShape,
        modifier = modifier
            .defaultMinSize(minHeight = minHeight)
            .clip(buttonShape),
        contentPadding = PaddingValues(0.dp)
    ) {
        Box(
            modifier = Modifier
                .background(innerFill, buttonShape)
                .border(0.6.dp, innerBorder, buttonShape)
                .padding(horizontal = horizontalPadding, vertical = 4.dp)
        ) {
            Text(
                text = text,
                fontFamily = FontFamily.Monospace,
                color = textColor
            )
        }
    }
}
