// SPDX-FileCopyrightText: 2026 Reseam contributors <https://reseam.app>
// SPDX-License-Identifier: AGPL-3.0-or-later

package app.reseam.patches.reddit.notifications

import app.reseam.patch.patch
import app.reseam.patches.reddit.RedditPatchStrings
import app.reseam.patches.reddit.core.REDDIT
import app.reseam.patches.universal.spoofSignature

// Firebase registers for push with the signing certificate's hash, and Google refuses Reddit's API
// key for any certificate but Reddit's own.
val fixPushNotifications = patch(RedditPatchStrings.fixPushNotifications) {
    description(RedditPatchStrings.fixPushNotificationsDescription)
    compatibleWith(REDDIT)
    dependsOn(spoofSignature)
}
