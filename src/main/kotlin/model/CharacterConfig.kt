package org.example.model

data class CharacterConfig(
    val name: String,
    val str: Int,
    val agi: Int,
    val sta: Int,
    val ene: Int,
    val cmd: Int,
    val warpMap: WarpMap,
    val pointsPerReset: Int,
    val overflowAttribute: AttributeType,
    val soloLevel: Int,
    val active: Boolean = true
)
