// SPDX-FileCopyrightText: 2026 AunAli K. <hello@auna.li>
// SPDX-FileCopyrightText: 2026 ReVanced contributors
// SPDX-License-Identifier: GPL-3.0-or-later

package app.reseam.patches.youtube.misc

import app.reseam.patch.Type
import app.reseam.patch.dex.AccessFlags
import app.reseam.patch.klass
import app.reseam.patch.method
import app.reseam.patch.patch
import app.reseam.patch.settings.before
import app.reseam.patch.settings.section
import app.reseam.patches.youtube.core.YOUTUBE
import app.reseam.patches.youtube.core.YouTubeSettings
import app.reseam.patches.youtube.core.YouTubeSettingsPages
import app.reseam.patches.youtube.core.youTubeSettings

// The server picks which formats to offer from the screen box the client reports, so a box that
// spans every size it knows about makes it offer all of them.
private const val MIN_HEIGHT_OR_WIDTH = 64
private const val MAX_HEIGHT_OR_WIDTH = 4096

val spoofDeviceDimensions = patch("Spoof device dimensions") {
    description("Reports a screen that spans every size YouTube knows, which unlocks higher video qualities.")
    compatibleWith(YOUTUBE)
    settings(youTubeSettings, section(YouTubeSettingsPages.Advanced, "Device dimensions", YouTubeSettings.spoofDeviceDimensions))

    execute {
        val dimensions = klass(deviceDimensionsToString.owner)
        method("deviceDimensionsConstructor") {
            inClass(dimensions)
            flags(AccessFlags.CONSTRUCTOR)
            params(Type.Int, Type.Int, Type.Int, Type.Int)
        }.before(YouTubeSettings.spoofDeviceDimensions) {
            param(0).assign(int(MIN_HEIGHT_OR_WIDTH))
            param(1).assign(int(MAX_HEIGHT_OR_WIDTH))
            param(2).assign(int(MIN_HEIGHT_OR_WIDTH))
            param(3).assign(int(MAX_HEIGHT_OR_WIDTH))
        }
    }
}

// The dimensions model is a four-int record; only its toString names the fields.
val deviceDimensionsToString = method("deviceDimensionsToString") {
    strings("minh.", ";maxh.")
    returns(Type.String)
    params()
}
