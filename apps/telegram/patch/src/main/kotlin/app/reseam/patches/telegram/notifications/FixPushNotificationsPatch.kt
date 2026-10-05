// SPDX-FileCopyrightText: 2026 AunAli K. <hello@auna.li>
// SPDX-License-Identifier: GPL-3.0-or-later

package app.reseam.patches.telegram.notifications

import app.reseam.patch.patch
import app.reseam.patches.telegram.core.TELEGRAM
import app.reseam.patches.universal.spoofSignature

// Firebase registers for push with the signing certificate's hash, and Google refuses Telegram's API
// key for any certificate but Telegram's own. Without a token, notifications only arrive while the
// app's own connection happens to be alive.
val fixPushNotifications = patch("Fix push notifications") {
    description("Shows Firebase the signature Telegram was published with, so push notifications work after re-signing.")
    compatibleWith(TELEGRAM)
    dependsOn(spoofSignature)
}
