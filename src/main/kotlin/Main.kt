package org.example

import org.example.composable.App
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState

fun main() = application {
    Window(
        onCloseRequest = ::exitApplication,
        title = "MUreset",
        icon = painterResource("mu-icon.png"),
        state = rememberWindowState(size = DpSize(1000.dp, 700.dp))
    ) {
        App()
    }
}
