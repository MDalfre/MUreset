package org.example.composable

import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import org.example.model.AttributeType

fun formatAttribute(attribute: AttributeType): String {
    return when (attribute) {
        AttributeType.STR -> "Str"
        AttributeType.AGI -> "Agi"
        AttributeType.STA -> "Sta"
        AttributeType.ENE -> "Ene"
        AttributeType.CMD -> "Cmd"
    }
}

fun digitsOnly(value: String): String {
    return value.filter { it.isDigit() }
}

fun timestampPrefix(): String {
    return LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"))
}
