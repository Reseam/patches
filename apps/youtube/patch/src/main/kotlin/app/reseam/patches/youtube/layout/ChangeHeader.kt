// SPDX-FileCopyrightText: 2026 AunAli K. <hello@auna.li>
// SPDX-FileCopyrightText: 2026 ReVanced contributors
// SPDX-License-Identifier: GPL-3.0-or-later

package app.reseam.patches.youtube.layout

import app.reseam.patch.ExtClass
import app.reseam.patch.Type
import app.reseam.patch.after
import app.reseam.patch.methods
import app.reseam.patch.patch
import app.reseam.patch.points
import app.reseam.patch.settings.section
import app.reseam.patches.youtube.core.YOUTUBE
import app.reseam.patches.youtube.core.YouTubeSettings
import app.reseam.patches.youtube.core.YouTubeSettingsPages
import app.reseam.patches.youtube.core.youTubeSettings

private object ChangeHeaderResources
private const val HEADER_ATTRIBUTE = "reseam_header"

private val targetStyles = listOf(
    "Base.Theme.YouTube.Light" to "reseam_header_light",
    "Base.Theme.YouTube.Dark" to "reseam_header_dark",
    "CairoLightThemeRingo2Updates" to "reseam_header_ringo2_light",
    "CairoDarkThemeRingo2Updates" to "reseam_header_ringo2_dark",
)

private fun asset(path: String): ByteArray =
    ChangeHeaderResources::class.java.getResourceAsStream("/reseam-header/$path")?.use { it.readBytes() }
        ?: error("reseam-header resource is missing: $path")

val changeHeader = patch("Change header") {
    description("Adds an option to change the logo in YouTube's top-left header.")
    compatibleWith(YOUTUBE)
    settings(youTubeSettings, section(YouTubeSettingsPages.Appearance, "General", YouTubeSettings.changeHeaderLogo))

    execute {
        // No typed attr helper exists; this reference-format entry backs the style item.
        resources.addRaw("attr", HEADER_ATTRIBUTE, 1u.toUByte(), 0u)

        targetStyles.map { it.second }.distinct().forEach { name ->
            val path = "res/drawable/$name.xml"
            resources.addFile("drawable", name, path, asset("drawable/$name.xml"))
        }

        targetStyles.forEach { (style, drawable) ->
            resources.style(style) {
                this[HEADER_ATTRIBUTE] = "@drawable/$drawable"
            }
        }

        for (attribute in listOf("ytWordmarkHeader", "ytPremiumWordmarkHeader")) {
            val id = resources.id("attr", attribute)?.toLong() ?: error("attr/$attribute is missing")
            methods("$attribute literals") { literals(id) }.points { literal(id) }.forEach {
                this
                    .captureAs("headerAttribute", Type.Int)
                    .after {
                        capture("headerAttribute").assign(
                            call(ChangeHeader.getHeaderAttributeId, capture("headerAttribute")),
                        )
                    }
            }
        }
    }
}

object ChangeHeader : ExtClass("app.reseam.youtube.theme.ChangeHeader") {
    val getHeaderAttributeId = static("getHeaderAttributeId", Type.Int, returns = Type.Int)
}
