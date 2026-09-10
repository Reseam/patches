// SPDX-FileCopyrightText: 2026 AunAli K. <hello@auna.li>
// SPDX-License-Identifier: GPL-3.0-or-later

package app.reseam.patches.instagram.misc.devmenu

import app.reseam.patch.Type
import app.reseam.patch.klass
import app.reseam.patch.method
import app.reseam.patch.patch
import app.reseam.patch.point
import app.reseam.patch.settings.returnTrueWhen
import app.reseam.patch.settings.section
import app.reseam.patches.instagram.core.DeveloperSettings
import app.reseam.patches.instagram.core.INSTAGRAM
import app.reseam.patches.instagram.core.USER_SESSION
import app.reseam.patches.instagram.core.instagramSettings
import app.reseam.patches.instagram.core.signatureCheck

val enableDeveloperMenu = patch("Enable developer menu") {
    description(
        """
        Surfaces Instagram's hidden developer menu as 'Internal Settings' at the bottom
        of the settings screen. Recommended on alpha/beta builds. On stable builds the
        developer flags appear as numeric IDs without descriptions.
        """,
    )
    compatibleWith(INSTAGRAM)
    enabledByDefault(false)
    dependsOn(signatureCheck)
    settings(instagramSettings, section("Developer", DeveloperSettings.unlockDeveloperOptions))

    execute {
        developerMenuGate.returnTrueWhen(DeveloperSettings.unlockDeveloperOptions)
    }
}

// ClearNotificationReceiver is registered in the manifest, so its name survives obfuscation.
// The developer-menu gate is the static (UserSession)Z call nearest above the
// "NOTIFICATION_DISMISSED" string in its onReceive; that signature is unique on the path.
val clearNotificationReceiver = klass("com.instagram.notifications.push.ClearNotificationReceiver")

val developerMenuGate = clearNotificationReceiver
    .method("onReceive") { strings("NOTIFICATION_DISMISSED") }
    .point { string("NOTIFICATION_DISMISSED") }
    .previous { invokeStatic { params(USER_SESSION); returns(Type.Boolean) } }
    .callee("developerMenuGate")
