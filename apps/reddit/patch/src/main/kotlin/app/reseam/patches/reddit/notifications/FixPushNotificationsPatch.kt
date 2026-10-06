// SPDX-FileCopyrightText: 2026 Cossale <hello@auna.li>
// SPDX-License-Identifier: AGPL-3.0-or-later

package app.reseam.patches.reddit.notifications

import app.reseam.patch.patch
import app.reseam.patches.reddit.core.REDDIT
import app.reseam.patches.universal.spoofSignature

// Firebase registers for push with the signing certificate's hash, and Google refuses Reddit's API
// key for any certificate but Reddit's own.
val fixPushNotifications = patch("Fix push notifications") {
    description("Shows Firebase the signature Reddit was published with, so push notifications work after re-signing.")
    compatibleWith(REDDIT)
    dependsOn(spoofSignature)
}
