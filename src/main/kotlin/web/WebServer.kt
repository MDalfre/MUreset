package io.github.mdalfre.web

import io.github.mdalfre.bot.BotRuntimeState
import io.github.mdalfre.model.LogType
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.call
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.server.response.respondBytes
import io.ktor.server.response.respondText
import io.ktor.server.routing.get
import io.ktor.server.routing.routing
import java.io.ByteArrayOutputStream
import java.util.concurrent.atomic.AtomicBoolean
import javax.imageio.ImageIO

object WebServer {
    private val started = AtomicBoolean(false)
    const val PORT = 8765

    fun start() {
        if (!started.compareAndSet(false, true)) {
            return
        }
        embeddedServer(Netty, host = "0.0.0.0", port = PORT) {
            routing {
                get("/") {
                    call.respondText(loadHtmlPage(), ContentType.Text.Html)
                }
                get("/api/status") {
                    val status = if (BotRuntimeState.isRunning) "running" else "stopped"
                    val active = BotRuntimeState.activeName ?: ""
                    call.respondText(
                        """{"status":"$status","active":"${escapeJson(active)}"}""",
                        ContentType.Application.Json
                    )
                }
                get("/api/characters") {
                    val characters = BotRuntimeState.getCharacters()
                    val items = characters.joinToString(",") { c ->
                        """{"name":"${escapeJson(c.name)}","active":${c.active},"warpMap":"${escapeJson(c.warpMap.label)}","soloLevel":${c.soloLevel}}"""
                    }
                    call.respondText("[$items]", ContentType.Application.Json)
                }
                get("/api/stats") {
                    val stats = BotRuntimeState.getStats()
                    val items = stats.entries.joinToString(",") { (name, s) ->
                        """{"name":"${escapeJson(name)}","level":${s.level},"masterLevel":${s.masterLevel},"resets":${s.resets}}"""
                    }
                    call.respondText("[$items]", ContentType.Application.Json)
                }
                get("/api/logs") {
                    val logs = BotRuntimeState.getLogs(120)
                    val items = logs.joinToString(",") { l ->
                        val type = when (l.type) {
                            LogType.INFO -> "info"
                            LogType.IMPORTANT -> "important"
                            LogType.ATTENTION -> "attention"
                        }
                        """{"message":"${escapeJson(l.message)}","type":"$type"}"""
                    }
                    call.respondText("[$items]", ContentType.Application.Json)
                }
                get("/api/screenshot/{name}") {
                    val name = call.parameters["name"] ?: return@get call.respondText(
                        "Missing name",
                        status = HttpStatusCode.BadRequest
                    )
                    val image = BotRuntimeState.getScreenshot(name)
                        ?: return@get call.respondText("Not found", status = HttpStatusCode.NotFound)
                    val bytes = ByteArrayOutputStream().use { out ->
                        ImageIO.write(image, "png", out)
                        out.toByteArray()
                    }
                    call.respondBytes(bytes, ContentType.Image.PNG)
                }
            }
        }.start(wait = false)
    }

    private fun escapeJson(value: String): String {
        return value
            .replace("\\", "\\\\")
            .replace("\"", "\\\"")
            .replace("\n", "\\n")
            .replace("\r", "\\r")
    }

    private fun loadHtmlPage(): String {
        val stream = javaClass.getResourceAsStream("/web/index.html") ?: return fallbackHtml()
        return stream.bufferedReader().use { it.readText() }
    }

    private fun fallbackHtml(): String {
        return "<html><body>web/index.html not found</body></html>"
    }
}
