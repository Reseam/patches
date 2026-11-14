// SPDX-FileCopyrightText: 2026 Reseam contributors <https://reseam.app>
// SPDX-License-Identifier: AGPL-3.0-or-later

package app.reseam.ailabels

import android.content.res.Resources
import app.reseam.bundle.isNightMode
import app.reseam.runtime.ui.Ui

/** The host app's colors, so labels and sheets look like the app they are in. */
class Palette(
    val sheet: Int,
    val card: Int,
    val text: Int,
    val secondaryText: Int,
    val border: Int,
    val accent: Int,
    val button: Int,
    val onButton: Int,
    val light: Boolean,
)

@Volatile private var hostPalette: Palette? = null

/** Apps call this with their current colors whenever they draw a label. */
fun usePalette(palette: Palette) {
    hostPalette = palette
}

internal fun palette(ui: Ui): Palette = hostPalette ?: with(ui.colors) {
    Palette(surfaceContainerLow, surfaceContainer, onSurface, onSurfaceVariant, outlineVariant, primary, primary, onPrimary, !ui.context.resources.isNightMode())
}

/** Pangram's badge colors, so a label reads the same as in its browser extension. */
internal enum class Tone(private val dayFill: Int, private val dayInk: Int, private val nightFill: Int, private val nightInk: Int) {
    Unsure(0x29919EAB, 0xFF637381.toInt(), 0x4D919EAB, 0xFFC4CDD5.toInt()),
    Human(0x2910B981, 0xFF047857.toInt(), 0x4D10B981, 0xFF6EE7B7.toInt()),
    Mixed(0x29FFAB00, 0xFF7A4100.toInt(), 0x4D7A4100, 0xFFD4A574.toInt()),
    Ai(0x29EF4444, 0xFFDC2626.toInt(), 0x4DEF4444, 0xFFFCA5A5.toInt()),
    ;

    fun fill(resources: Resources): Int = if (isLight(resources)) dayFill else nightFill

    fun ink(resources: Resources): Int = if (isLight(resources)) dayInk else nightInk

    private fun isLight(resources: Resources): Boolean = hostPalette?.light ?: !resources.isNightMode()

    companion object {
        fun of(label: Label): Tone = when (label) {
            Label.Human -> Human
            Label.Mixed, Label.AiAssisted -> Mixed
            Label.Ai -> Ai
        }
    }
}

/** The colors of AI, AI-assisted and human in charts, as the detector's extension draws them. */
internal object ShareColors {
    const val AI = 0xFFFF5A1F.toInt()
    const val ASSISTED = 0xFFF5C84C.toInt()
    const val HUMAN = 0xFF5FA88E.toInt()
    const val HUMAN_RING = 0xFFCCDFD9.toInt()

    fun of(label: Label): Int = when (label) {
        Label.Human -> HUMAN
        Label.Mixed, Label.AiAssisted -> ASSISTED
        Label.Ai -> AI
    }
}
