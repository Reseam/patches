// SPDX-FileCopyrightText: 2026 Reseam contributors <https://reseam.app>
// SPDX-License-Identifier: AGPL-3.0-or-later

@file:JvmName("PlayStoreInstaller")

package app.reseam.universal.installer

import android.content.pm.InstallSourceInfo
import android.content.pm.PackageManager
import app.reseam.runtime.Export
import java.util.Collections
import java.util.WeakHashMap

private const val PLAY_STORE = "com.android.vending"
private val own =
    Collections.synchronizedSet(
        Collections.newSetFromMap(WeakHashMap<InstallSourceInfo, Boolean>())
    )

@Export fun ownPackage(): String? = null

@Export
fun getInstallerPackageName(manager: PackageManager, packageName: String): String? {
    val installer = manager.getInstallerPackageName(packageName)
    return if (packageName == ownPackage()) PLAY_STORE else installer
}

@Export
@Throws(PackageManager.NameNotFoundException::class)
fun getInstallSourceInfo(manager: PackageManager, packageName: String): InstallSourceInfo =
    manager.getInstallSourceInfo(packageName).also { if (packageName == ownPackage()) own.add(it) }

@Export
fun getInstallingPackageName(info: InstallSourceInfo): String? =
    if (info in own) PLAY_STORE else info.installingPackageName

@Export
fun getInitiatingPackageName(info: InstallSourceInfo): String? =
    if (info in own) PLAY_STORE else info.initiatingPackageName
