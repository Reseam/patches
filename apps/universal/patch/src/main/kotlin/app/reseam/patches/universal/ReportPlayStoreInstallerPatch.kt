// SPDX-FileCopyrightText: 2026 AunAli K. <hello@auna.li>
// SPDX-License-Identifier: GPL-3.0-or-later

package app.reseam.patches.universal

import app.reseam.patch.ExtClass
import app.reseam.patch.Type
import app.reseam.patch.patch

private const val PACKAGE_MANAGER = "android.content.pm.PackageManager"
private const val INSTALL_SOURCE_INFO = "android.content.pm.InstallSourceInfo"

private object PlayStoreInstaller : ExtClass("app.reseam.universal.installer.PlayStoreInstaller") {
    val ownPackage = static("ownPackage", returns = Type.String)
    val getInstallerPackageName = static("getInstallerPackageName", PACKAGE_MANAGER, Type.String, returns = Type.String)
    val getInstallSourceInfo = static("getInstallSourceInfo", PACKAGE_MANAGER, Type.String, returns = INSTALL_SOURCE_INFO)
    val getInstallingPackageName = static("getInstallingPackageName", INSTALL_SOURCE_INFO, returns = Type.String)
    val getInitiatingPackageName = static("getInitiatingPackageName", INSTALL_SOURCE_INFO, returns = Type.String)
}

val reportPlayStoreInstaller = patch("Report Play Store as installer") {
    description("Makes the app see itself as installed from Google Play.")

    execute {
        val packageName = requireNotNull(manifest.packageName) { "The manifest names no package" }
        PlayStoreInstaller.ownPackage.implement { returnValue(string(packageName)) }
        val redirected = with(PlayStoreInstaller) {
            bytecode.redirectInstanceCalls(getInstallerPackageName, getInstallSourceInfo, getInstallingPackageName, getInitiatingPackageName)
        }
        log.info("Redirected $redirected installer lookups")
    }
}
