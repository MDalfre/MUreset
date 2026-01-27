package io.github.mdalfre.composable

import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme

@Composable
fun RunningBanner(isRunning: Boolean) {
    if (!isRunning) {
        return
    }
    val bannerShape = RoundedCornerShape(10.dp)
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, MaterialTheme.colorScheme.secondary.copy(alpha = 0.7f), bannerShape),
        shape = bannerShape,
        colors = CardDefaults.cardColors(containerColor = Color(0xFF2C1412))
    ) {
        Text(
            text = "Bot running",
            modifier = Modifier.padding(12.dp),
            fontWeight = FontWeight.SemiBold,
            color = Color(0xFFF2C2C7)
        )
    }
}
