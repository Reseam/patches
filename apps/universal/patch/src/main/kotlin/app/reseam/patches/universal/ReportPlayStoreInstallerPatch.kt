// SPDX-FileCopyrightText: 2026 Reseam contributors <https://reseam.app>
// SPDX-License-Identifier: AGPL-3.0-or-later

package app.reseam.patches.universal

import app.reseam.patch.patch
import app.reseam.universal.installer.bindings.PlayStoreInstaller

private const val PACKAGE_MANAGER = "android.content.pm.PackageManager"
private const val INSTALL_SOURCE_INFO = "android.content.pm.InstallSourceInfo"
val reportPlayStoreInstaller = patch(UniversalPatchStrings.reportPlayStoreInstaller) {
    description(UniversalPatchStrings.reportPlayStoreInstallerDescription)

    execute {
        val packageName = requireNotNull(manifest.packageName) { "The manifest names no package" }
        PlayStoreInstaller.ownPackage.implement { returnValue(string(packageName)) }
        val redirected = with(PlayStoreInstaller) {
            bytecode.redirectInstanceCalls(getInstallerPackageName, getInstallSourceInfo, getInstallingPackageName, getInitiatingPackageName)
        }
        log.info("Redirected $redirected installer lookups")
    }
}
