// SPDX-FileCopyrightText: 2026 Reseam contributors <https://reseam.app>
// SPDX-License-Identifier: AGPL-3.0-or-later

package app.reseam.patches.twitch.core

import app.reseam.patch.Type
import app.reseam.patch.after
import app.reseam.patch.klass
import app.reseam.patch.method
import app.reseam.patch.settings.addReactPackage
import app.reseam.patch.settings.settingsHost
import app.reseam.patches.bundle.reseamAbout
import app.reseam.patches.bundle.reseamBrand

val twitchSettings = settingsHost("twitch") {
    compatibleWith(TWITCH)
    brand(reseamBrand)
    about(reseamAbout)

    install {
        // Hermes gates read their toggles through this React Native package.
        reactPackages.after { addReactPackage(result) }
    }
}

// React Native's autolinking list keeps its name.
private val reactPackages = klass("com.facebook.react.PackageList")
    .method("getPackages") { returns(Type.ArrayList) }
