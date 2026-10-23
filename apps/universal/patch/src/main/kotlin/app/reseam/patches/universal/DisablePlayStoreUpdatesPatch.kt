// SPDX-FileCopyrightText: 2026 MadKarma <me@madkarma.top>
// SPDX-License-Identifier: AGPL-3.0-or-later

package app.reseam.patches.universal

import app.reseam.patch.patch

val disablePlayStoreUpdates = patch("Disable Play Store updates") {
    description("Hides the app from Google Play's pending updates.")

    execute {
        // Google Play accepts version codes up to 2100000000, below Android's signed Int maximum.
        val versionCode = Int.MAX_VALUE.toUInt()

        for (name in manifest.components()) {
            val component = manifest.component(name)

            val previousVersionCode =
                component.versionCode ?: error("Play Store updates: $name has no versionCode")

            component.setVersionCode(versionCode)
            log.info("Play Store updates: $name versionCode $previousVersionCode -> $versionCode.")
        }
    }
}
