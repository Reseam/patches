// SPDX-FileCopyrightText: 2026 AunAli K. <hello@auna.li>
// SPDX-FileCopyrightText: 2026 ReVanced contributors
// SPDX-License-Identifier: GPL-3.0-or-later

package app.reseam.patches.youtube.player

import app.reseam.patch.Type
import app.reseam.patch.dex.AccessFlags
import app.reseam.patch.method
import app.reseam.patch.patch
import app.reseam.patch.point
import app.reseam.patch.settings.section
import app.reseam.patch.settings.skipWhen
import app.reseam.patches.youtube.core.YOUTUBE
import app.reseam.patches.youtube.core.YouTubeSettings
import app.reseam.patches.youtube.core.YouTubeSettingsPages
import app.reseam.patches.youtube.core.youTubeSettings

val disableRollingNumberAnimations = patch("Disable rolling number animations") {
    description("Stops animated number transitions in the player.")
    compatibleWith(YOUTUBE)
    dependsOn(youTubeSettings)
    settings(youTubeSettings, section(YouTubeSettingsPages.Overlay, "Player overlay", YouTubeSettings.disableRollingNumberAnimations))

    execute {
        val rollingNumber = method("rolling-number image update") {
            flags(AccessFlags.PUBLIC or AccessFlags.FINAL)
            params("android.graphics.Bitmap")
            returns(Type.Void)
            calls {
                owner("Landroid/text/SpannableString;")
                name("setSpan")
                params(Type.Object, Type.Int, Type.Int, Type.Int)
                returns(Type.Void)
            }
            custom {
                classDef.info.superclass == "Landroid/support/v7/widget/AppCompatTextView;" ||
                    classDef.info.superclass ==
                    "Lcom/google/android/libraries/youtube/rendering/ui/spec/typography/YouTubeAppCompatTextView;"
            }
        }
        rollingNumber
            .point("rolling number image span") {
                invokeVirtual {
                    owner("android.text.SpannableString")
                    name("setSpan")
                    params(Type.Object, Type.Int, Type.Int, Type.Int)
                    returns(Type.Void)
                }
            }
            .skipWhen(YouTubeSettings.disableRollingNumberAnimations)
    }
}
