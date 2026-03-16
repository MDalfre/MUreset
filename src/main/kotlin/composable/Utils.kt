package io.github.mdalfre.composable

import io.github.mdalfre.model.AttributeType
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

fun formatAttribute(attribute: AttributeType): String =
    when (attribute) {
        AttributeType.STR -> "Str"
        AttributeType.AGI -> "Agi"
        AttributeType.STA -> "Sta"
        AttributeType.ENE -> "Ene"
        AttributeType.CMD -> "Cmd"
    }

fun digitsOnly(value: String): String = value.filter { it.isDigit() }

fun timestampPrefix(): String = LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"))
