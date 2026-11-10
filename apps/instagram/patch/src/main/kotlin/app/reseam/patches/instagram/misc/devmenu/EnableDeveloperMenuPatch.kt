// SPDX-FileCopyrightText: 2026 Reseam contributors <https://reseam.app>
// SPDX-License-Identifier: AGPL-3.0-or-later

package app.reseam.patches.instagram.misc.devmenu

import app.reseam.instagram.settings.InstagramStrings
import app.reseam.patch.Type
import app.reseam.patch.klass
import app.reseam.patch.method
import app.reseam.patch.patch
import app.reseam.patch.point
import app.reseam.patch.settings.gate
import app.reseam.patch.settings.section
import app.reseam.instagram.settings.DeveloperSettings
import app.reseam.patches.instagram.InstagramPatchStrings
import app.reseam.patches.instagram.core.INSTAGRAM
import app.reseam.patches.instagram.core.USER_SESSION
import app.reseam.patches.instagram.core.instagramSettings
import app.reseam.patches.instagram.core.signatureCheck

val enableDeveloperMenu = patch(InstagramPatchStrings.enableDeveloperMenu) {
    description(InstagramPatchStrings.enableDeveloperMenuDescription)
    compatibleWith(INSTAGRAM)
    enabledByDefault(false)
    dependsOn(signatureCheck)
    settings(instagramSettings, section(InstagramStrings.sectionDeveloper, DeveloperSettings.unlockDeveloperOptions))

    execute {
        gate(DeveloperSettings.unlockDeveloperOptions) {
            developerMenuGate.alwaysReturn(true)
        }
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
