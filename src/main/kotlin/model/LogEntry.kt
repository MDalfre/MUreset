package org.example.model

enum class LogType {
    INFO,
    IMPORTANT,
    ATTENTION
}

data class LogEntry(
    val message: String,
    val type: LogType
)
