package org.example.model

enum class WarpMap(val label: String, val templateResource: String) {
    ELBELAND_2("Elbeland 2", "/elbeland2_template.png"),
    ELBELAND_3("Elbeland 3", "/elbeland3_template.png");

    companion object {
        fun fromLabel(label: String?): WarpMap? {
            val trimmed = label?.trim() ?: return null
            return values().firstOrNull { it.label.equals(trimmed, ignoreCase = true) }
        }

        fun fromName(name: String?): WarpMap? {
            val trimmed = name?.trim() ?: return null
            return runCatching { valueOf(trimmed.uppercase()) }.getOrNull()
        }
    }
}
