// SPDX-FileCopyrightText: 2026 Reseam contributors <https://reseam.app>
// SPDX-License-Identifier: AGPL-3.0-or-later

package app.reseam.patches.x.login

import app.reseam.patch.after
import app.reseam.patch.patch
import app.reseam.patch.settings.onAction
import app.reseam.patch.settings.section
import app.reseam.patches.x.core.X
import app.reseam.patches.x.core.XSettingsPages
import app.reseam.patches.x.core.mainActivityOnCreate
import app.reseam.patches.x.core.xSettings
import app.reseam.x.login.bindings.LoginTransfer
import app.reseam.x.settings.XSettings
import app.reseam.x.settings.XStrings

val loginTransfer = patch("Import and export login") {
    description("Copies this account's login to the clipboard, and offers a field on the welcome screen to paste one into, for signing in without the browser.")
    compatibleWith(X)
    enabledByDefault(false)
    settings(xSettings, section(XSettingsPages.Advanced, XStrings.sectionLogin, XSettings.showImportOnLaunch, XSettings.exportLogin))

    execute {
        onAction(XSettings.exportLogin) { LoginTransfer.copyLogin(activity) }
        // Import belongs where you land when signed out; settings need a login.
        mainActivityOnCreate.after { LoginTransfer.offerImport(thisObject) }
    }
}
