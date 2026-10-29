package com.example.geostamp.ui.editor

class ColorPreset(val name: String, val argb: Int)

val ForegroundPresets = listOf(
    ColorPreset("White", 0xFFFFFFFF.toInt()),
    ColorPreset("Yellow", 0xFFFFEB3B.toInt()),
    ColorPreset("Black", 0xFF000000.toInt())
)

val BackgroundPresets = listOf(
    ColorPreset("Dark", 0xB3000000.toInt()),
    ColorPreset("Light", 0xB3FFFFFF.toInt()),
    ColorPreset("Blue", 0xB30D47A1.toInt()),
    ColorPreset("None", 0x00000000)
)
