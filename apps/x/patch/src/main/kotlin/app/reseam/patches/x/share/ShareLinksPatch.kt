// SPDX-FileCopyrightText: 2026 Reseam contributors <https://reseam.app>
// SPDX-License-Identifier: AGPL-3.0-or-later

package app.reseam.patches.x.share

import app.reseam.patch.Type
import app.reseam.patch.before
import app.reseam.patch.method
import app.reseam.patch.patch
import app.reseam.patch.settings.section
import app.reseam.patches.x.XPatchStrings
import app.reseam.patches.x.core.X
import app.reseam.patches.x.core.XSettingsPages
import app.reseam.patches.x.core.xSettings
import app.reseam.x.settings.XSettings
import app.reseam.x.settings.XStrings
import app.reseam.x.share.bindings.ShareLinks

val shareLinks = patch(XPatchStrings.shareLinks) {
    description(XPatchStrings.shareLinksDescription)
    compatibleWith(X)
    settings(xSettings, section(XSettingsPages.Links, XStrings.sectionSharing, XSettings.removeShareTracking, XSettings.shareLinkProvider))

    execute {
        // Every share intent, whether from the action bar, the share sheet's app row or its
        // "Share via" chooser, is built from the link in the first parameter.
        shareIntent.before { param(0).assign(ShareLinks.rewrite(param(0))) }

        // The share sheet's "Copy link" puts the link on the clipboard without building an intent.
        copyShareLink.before { param(0).assign(ShareLinks.rewrite(param(0))) }
    }
}

private val shareIntent = method("shareIntent") {
    strings("android.intent.action.SEND", "text/plain", "android.intent.extra.TEXT")
    params(Type.String, Type.String)
    returns("android.content.Intent")
}

private val copyShareLink = method("copyShareLink") {
    strings("link", "copy_link")
    params(Type.String)
    returns(Type.Void)
}
