package io.github.mdalfre.model

enum class CurrentMap(
    val templateResource: String,
) {
    LORENCIA(
        templateResource = "/current_map_lorencia.png",
    ),
    ELBELAND(
        templateResource = "/current_map_elbeland.png",
    )
}
