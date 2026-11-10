// SPDX-FileCopyrightText: 2026 Reseam contributors <https://reseam.app>
// SPDX-License-Identifier: AGPL-3.0-or-later

package app.reseam.patches.youtube.misc

import app.reseam.patch.Type
import app.reseam.patch.dex.AccessFlags
import app.reseam.patch.klass
import app.reseam.patch.method
import app.reseam.patch.patch
import app.reseam.patch.settings.gate
import app.reseam.patch.settings.section
import app.reseam.patches.youtube.YouTubePatchStrings
import app.reseam.patches.youtube.core.YOUTUBE
import app.reseam.patches.youtube.core.YouTubeSettingsPages
import app.reseam.patches.youtube.core.youTubeSettings
import app.reseam.youtube.settings.AdvancedSettings
import app.reseam.youtube.settings.YouTubeStrings

// The server picks which formats to offer from the screen box the client reports, so a box that
// spans every size it knows about makes it offer all of them.
private const val SMALLEST_SIDE = 64
private const val LARGEST_SIDE = 4096

val spoofDeviceDimensions = patch(YouTubePatchStrings.spoofDeviceDimensions) {
    description(YouTubePatchStrings.spoofDeviceDimensionsDescription)
    compatibleWith(YOUTUBE)
    settings(youTubeSettings, section(YouTubeSettingsPages.Advanced, YouTubeStrings.sectionDeviceDimensions, AdvancedSettings.spoofDeviceDimensions))

    execute {
        val dimensions = klass(deviceDimensionsToString.owner)
        gate(AdvancedSettings.spoofDeviceDimensions) {
            method("deviceDimensionsConstructor") {
                inClass(dimensions)
                flags(AccessFlags.CONSTRUCTOR)
                params(Type.Int, Type.Int, Type.Int, Type.Int)
            }.before {
                param(0).assign(int(SMALLEST_SIDE))
                param(1).assign(int(LARGEST_SIDE))
                param(2).assign(int(SMALLEST_SIDE))
                param(3).assign(int(LARGEST_SIDE))
            }
        }
    }
}

// The dimensions model is a four-int record; only its toString names the fields.
val deviceDimensionsToString = method("deviceDimensionsToString") {
    strings("minh.", ";maxh.")
    returns(Type.String)
    params()
}
