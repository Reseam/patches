// SPDX-FileCopyrightText: 2026 AunAli K. <hello@auna.li>
// SPDX-FileCopyrightText: 2026 ReVanced contributors
// SPDX-License-Identifier: GPL-3.0-or-later

package app.reseam.patches.youtube.layout

import app.reseam.patch.Type
import app.reseam.patch.after
import app.reseam.patch.dex.AccessFlags
import app.reseam.patch.dex.Opcode
import app.reseam.patch.method
import app.reseam.patch.point
import app.reseam.patches.youtube.core.MAIN_ACTIVITY
import app.reseam.patches.youtube.core.YOUTUBE
import app.reseam.patches.youtube.core.YouTubeSettings
import app.reseam.patches.youtube.core.YouTubeSettingsPages
import app.reseam.patches.youtube.core.mainActivityOnCreate
import app.reseam.patches.youtube.core.youTubeSettings
import app.reseam.patches.youtubecommon.CustomBranding
import app.reseam.patches.youtubecommon.Launcher
import app.reseam.patches.youtubecommon.customBrandingFor

val customBranding = customBrandingFor(
    YOUTUBE,
    youTubeSettings,
    YouTubeSettingsPages.Appearance,
    "General",
    YouTubeSettings.customBrandingName,
    YouTubeSettings.customBrandingIcon,
    Launcher(
        activity = MAIN_ACTIVITY,
        entry = "com.google.android.youtube.app.honeycomb.Shell\$HomeActivity",
        originalIcon = "@mipmap/ringo2_ic_launcher",
    ),
    mainActivityOnCreate,
) {
    val builderField = notificationMethod
        .point("notificationBuilderCast") {
            opcode(Opcode.IGET_OBJECT)
            field { type(Type.Object) }
            then(within = 4) { checkCast("android.app.Notification\$Builder") }
        }
        .previous { opcode(Opcode.IGET_BOOLEAN) }
        .previous { opcode(Opcode.IGET_OBJECT); field { type(Type.Object) } }
        .field()
    notificationMethod.after {
        call(
            CustomBranding.setNotificationIcon,
            thisObject.field(builderField).cast("android.app.Notification\$Builder"),
            string(YouTubeSettings.customBrandingIcon.key),
        )
    }
}

private val notificationMethod = method("notificationMethod") {
    strings("key_action_priority")
    returns(Type.Void)
    paramCount(1)
    flags(AccessFlags.CONSTRUCTOR)
}
