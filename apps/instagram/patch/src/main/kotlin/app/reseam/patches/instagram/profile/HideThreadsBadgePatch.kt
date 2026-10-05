// SPDX-FileCopyrightText: 2026 AunAli K. <hello@auna.li>
// SPDX-License-Identifier: GPL-3.0-or-later

package app.reseam.patches.instagram.profile

import app.reseam.patch.Type
import app.reseam.patch.field
import app.reseam.patch.klass
import app.reseam.patch.method
import app.reseam.patch.patch
import app.reseam.patch.settings.gate
import app.reseam.patch.settings.section
import app.reseam.patches.instagram.core.AppearanceSettings
import app.reseam.patches.instagram.core.INSTAGRAM
import app.reseam.patches.instagram.core.instagramSettings
import app.reseam.patches.instagram.core.signatureCheck

private const val BOXED_BOOLEAN = "java.lang.Boolean"

val hideThreadsBadge = patch("Hide Threads badge") {
    description("Removes the Threads badge, label and profile link chip from profiles.")
    compatibleWith(INSTAGRAM)
    dependsOn(signatureCheck)
    settings(instagramSettings, section("Appearance", AppearanceSettings.hideThreadsBadge))

    execute {
        gate(AppearanceSettings.hideThreadsBadge) {
            showThreadsBadge.before {
                returnValue(staticField(field(BOXED_BOOLEAN, "FALSE", BOXED_BOOLEAN)))
            }
            threadsBadgeLabel.alwaysReturnNull()
            threadsProfileBanner.alwaysReturn()
        }
    }
}

private val user = klass("com.instagram.user.model.User")

private val showThreadsBadge = method("showThreadsBadge") {
    inClass(user)
    strings("show_text_post_app_badge")
    returns(BOXED_BOOLEAN)
}

private val threadsBadgeLabel = method("threadsBadgeLabel") {
    inClass(user)
    strings("text_post_app_badge_label")
    returns(Type.String)
}

// The profile header's banner row reads these fields from the user model directly rather than through
// the getters above. Its Threads adder returns without adding a chip for users without Threads.
private val threadsBannerEligible = method("threadsBannerEligible") {
    literals("show_text_post_app_badge".hashCode().toLong(), "text_post_app_badge_label".hashCode().toLong())
    returns(Type.Boolean)
}

private val threadsProfileBanner = method("threadsProfileBanner") {
    calls(threadsBannerEligible)
    literals("text_post_new_post_count".hashCode().toLong())
}
