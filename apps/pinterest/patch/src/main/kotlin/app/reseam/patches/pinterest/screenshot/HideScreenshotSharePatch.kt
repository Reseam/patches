// SPDX-FileCopyrightText: 2026 Cossale <hello@auna.li>
// SPDX-License-Identifier: AGPL-3.0-or-later

package app.reseam.patches.pinterest.screenshot

import app.reseam.patch.Type
import app.reseam.patch.before
import app.reseam.patch.method
import app.reseam.patch.patch
import app.reseam.patch.settings.gate
import app.reseam.patch.settings.section
import app.reseam.patches.pinterest.core.PINTEREST
import app.reseam.patches.pinterest.core.pinterestSettings
import app.reseam.pinterest.screenshot.bindings.ScreenshotShares
import app.reseam.pinterest.settings.PinterestStrings
import app.reseam.pinterest.settings.ShareSettings

val hideScreenshotShare = patch("Hide screenshot share menu") {
    description("Hides the share menu that appears after a screenshot.")
    compatibleWith(PINTEREST)
    settings(pinterestSettings, section(PinterestStrings.sectionSharing, ShareSettings.hideScreenshotShare))

    execute {
        gate(ShareSettings.hideScreenshotShare) {
            screenshotDetection.before { returnVoid() }
        }
        // Pin closeups listen for screenshots on their own.
        bytecode.redirectCalls("android.app.Activity", "registerScreenCaptureCallback", ScreenshotShares.registerScreenCaptureCallback)
    }
}

private val screenshotDetection = method("screenshotDetection") {
    strings("sg_android_new_screenshot_api_14", "Failed to registerScreenCaptureAPICallback")
    returns(Type.Void)
}
