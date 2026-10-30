// SPDX-FileCopyrightText: 2026 Cossale <hello@auna.li>
// SPDX-License-Identifier: AGPL-3.0-or-later

package app.reseam.patches.x.login

import app.reseam.patch.after
import app.reseam.patch.methodTarget
import app.reseam.patch.patch
import app.reseam.patch.settings.section
import app.reseam.patches.x.core.X
import app.reseam.patches.x.core.addSettingsSection
import app.reseam.patches.x.core.reseamLogoId
import app.reseam.patches.x.core.settingsItem
import app.reseam.patches.x.core.settingsSection
import app.reseam.patches.x.core.unitInstance
import app.reseam.patches.x.core.xSettings
import app.reseam.x.login.bindings.LoginTransfer
import app.reseam.x.settings.XSettings
import app.reseam.x.settings.XStrings
import app.reseam.x.settings.bindings.ExportLogin
import app.reseam.x.settings.bindings.SettingsRows

private const val MAIN_ACTIVITY = "com.x.android.main.MainActivity"

val loginTransfer = patch("Import and export login") {
    description("Copies this account's login to the clipboard, and offers to import one on the welcome screen, for signing in without the browser.")
    compatibleWith(X)
    enabledByDefault(false)
    settings(xSettings, section(XStrings.sectionLogin, XSettings.showImportOnLaunch))

    execute {
        val logoId = reseamLogoId()
        ExportLogin.invoke.implement {
            LoginTransfer.copyLogin()
            returnValue(staticField(unitInstance))
        }
        addSettingsSection("login") { design ->
            settingsSection(
                design,
                SettingsRows.single(
                    settingsItem(design, "Export login", "Copies this account's login to the clipboard.", logoId, ExportLogin.descriptor)
                ),
            )
        }
        // Import belongs where you land when signed out; settings need a login.
        mainActivityOnCreate.after { LoginTransfer.offerImport(thisObject) }
    }
}

private val mainActivityOnCreate = methodTarget("mainActivityOnCreate") {
    val activity = bytecode.findClass(MAIN_ACTIVITY) ?: error("$MAIN_ACTIVITY missing")
    activity.virtualMethods.single { it.name == "onCreate" && it.parameterTypes == listOf("Landroid/os/Bundle;") }
}
