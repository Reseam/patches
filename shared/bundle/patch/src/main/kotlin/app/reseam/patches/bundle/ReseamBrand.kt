// SPDX-FileCopyrightText: 2026 Reseam contributors <https://reseam.app>
// SPDX-License-Identifier: AGPL-3.0-or-later

package app.reseam.patches.bundle

import app.reseam.patch.settings.Brand
import app.reseam.patch.settings.Color
import app.reseam.patch.settings.ColorScheme
import app.reseam.patch.settings.Font
import app.reseam.patch.settings.Logo
import app.reseam.patch.settings.TextStyle
import app.reseam.patch.settings.Typography
import app.reseam.patch.settings.Vector

/** The `mode/light`, `mode/dark` and type tokens of the "Reseam" Penpot file. */
val reseamBrand =
    Brand(
        name = "Reseam",
        light =
            ColorScheme(
                primary = Color.hex("#296A48"),
                onPrimary = Color.hex("#FFFFFF"),
                primaryContainer = Color.hex("#A2EBBD"),
                onPrimaryContainer = Color.hex("#003920"),
                secondaryContainer = Color.hex("#D4E7D7"),
                onSecondaryContainer = Color.hex("#394B3F"),
                surface = Color.hex("#F9F9F9"),
                surfaceContainerLow = Color.hex("#F3F3F3"),
                surfaceContainer = Color.hex("#EEEEEE"),
                surfaceContainerHigh = Color.hex("#E8E8E8"),
                surfaceContainerHighest = Color.hex("#E2E2E2"),
                onSurface = Color.hex("#1B1B1B"),
                onSurfaceVariant = Color.hex("#474747"),
                outline = Color.hex("#777777"),
                outlineVariant = Color.hex("#C6C6C6"),
                error = Color.hex("#A73735"),
                onError = Color.hex("#FFFFFF"),
                errorContainer = Color.hex("#FFDAD7"),
                onErrorContainer = Color.hex("#871F20"),
                scrim = Color.hex("#000000"),
            ),
        dark =
            ColorScheme(
                primary = Color.hex("#A2EBBD"),
                onPrimary = Color.hex("#000000"),
                primaryContainer = Color.hex("#075232"),
                onPrimaryContainer = Color.hex("#AEF2C6"),
                secondaryContainer = Color.hex("#394B3F"),
                onSecondaryContainer = Color.hex("#D4E7D7"),
                surface = Color.hex("#0A0A0A"),
                surfaceContainerLow = Color.hex("#111111"),
                surfaceContainer = Color.hex("#171717"),
                surfaceContainerHigh = Color.hex("#1F1F1F"),
                surfaceContainerHighest = Color.hex("#262626"),
                onSurface = Color.hex("#EEEEEE"),
                onSurfaceVariant = Color.hex("#A1A1A1"),
                outline = Color.hex("#919191"),
                outlineVariant = Color.hex("#393939"),
                error = Color.hex("#FD736D"),
                onError = Color.hex("#000000"),
                errorContainer = Color.hex("#871F20"),
                onErrorContainer = Color.hex("#FFDAD7"),
                scrim = Color.hex("#000000"),
            ),
        logo = Logo(light = vector("/brand/logo-light.svg"), dark = vector("/brand/logo-dark.svg")),
        font = Font(resource("/brand/InterVariable.ttf").readBytes()),
        typography =
            Typography(
                headlineSmall = TextStyle(size = 24f, lineHeight = 32f, weight = 600, tracking = -0.48f),
                titleLarge = TextStyle(size = 22f, lineHeight = 28f, weight = 600, tracking = -0.22f),
                titleMedium = TextStyle(size = 16f, lineHeight = 24f, weight = 600),
                bodyLarge = TextStyle(size = 16f, lineHeight = 24f, weight = 400),
                bodyMedium = TextStyle(size = 14f, lineHeight = 20f, weight = 400),
                labelLarge = TextStyle(size = 14f, lineHeight = 20f, weight = 500),
                labelMedium = TextStyle(size = 12f, lineHeight = 16f, weight = 500),
            ),
    )

private object Files

internal fun resource(path: String) =
    Files::class.java.getResource(path) ?: error("$path is missing from the bundle's patch jar")

internal fun vector(path: String) = Vector.svg(resource(path).readText())

fun lucide(name: String) = vector("/icons/$name.svg")
