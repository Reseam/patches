// SPDX-FileCopyrightText: 2026 Cossale <hello@auna.li>
// SPDX-License-Identifier: AGPL-3.0-or-later

package app.reseam.patches.discord.update

import app.reseam.patch.Type
import app.reseam.patch.alwaysReturnNull
import app.reseam.patch.field
import app.reseam.patch.klass
import app.reseam.patch.method
import app.reseam.patch.patch
import app.reseam.patch.replace
import app.reseam.patches.discord.core.DISCORD

private const val BUNDLE_UPDATER = "com.discord.bundle_updater.BundleUpdater"
private const val FUNCTION0 = "kotlin.jvm.functions.Function0"

val keepPatchedBundle = patch("Keep patched bundle") {
    description("Stop in-app code updates from replacing your patches.")
    compatibleWith(DISCORD)

    execute {
        cachedBundle.alwaysReturnNull()
        updateWorker.replace {
            param(1).set(checking, bool(false))
            whenNotNull(param(2)) {
                param(2).callInterface(FUNCTION0, "invoke", "()Ljava/lang/Object;")
            }
            returnVoid()
        }
    }
}

private val bundleUpdater = klass(BUNDLE_UPDATER)
private val checking = bundleUpdater.field("otaUpdateChecking")

private val cachedBundle = bundleUpdater.method("getBundle") {
    params()
    returns("com.discord.bundle_updater.BundleUpdater\$OtaBundle")
}

private val updateWorker = bundleUpdater.method("checkForUpdate\$lambda\$11") {
    params(Type.Int, BUNDLE_UPDATER, FUNCTION0)
    returns(Type.Void)
}
