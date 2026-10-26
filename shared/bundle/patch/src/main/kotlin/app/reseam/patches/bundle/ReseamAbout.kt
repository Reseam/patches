// SPDX-FileCopyrightText: 2026 Cossale <hello@auna.li>
// SPDX-License-Identifier: AGPL-3.0-or-later

package app.reseam.patches.bundle

import app.reseam.bundle.settings.BundleStrings
import app.reseam.patch.settings.About
import app.reseam.patch.settings.Link
import app.reseam.patch.settings.Verbatim

private const val SITE = "https://reseam.app"

val reseamAbout =
    About(
        links = listOf("Discord", "Reddit", "Telegram", "X").map { name ->
            val slug = name.lowercase()
            Link(Verbatim(name), "$SITE/$slug", vector("/socials/$slug.svg"))
        },
        actions = listOf(
            Link(BundleStrings.bundleDonate, "$SITE/donate/", lucide("heart")),
            Link(BundleStrings.bundleRequestPatch, "$SITE/requests/", lucide("plus")),
        ),
    )
