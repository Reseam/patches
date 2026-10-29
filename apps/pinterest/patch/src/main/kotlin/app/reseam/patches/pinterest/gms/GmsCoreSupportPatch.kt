// SPDX-FileCopyrightText: 2026 Cossale <hello@auna.li>
// SPDX-License-Identifier: AGPL-3.0-or-later

package app.reseam.patches.pinterest.gms

import app.reseam.patch.alwaysReturn
import app.reseam.patch.klass
import app.reseam.patch.method
import app.reseam.patches.gmscore.gmsCoreSupportFor
import app.reseam.patches.pinterest.core.PINTEREST
import app.reseam.patches.pinterest.core.PINTEREST_SIGNATURE
import app.reseam.patches.pinterest.core.mainActivityOnCreate
import app.reseam.patches.pinterest.core.pinterestSettings

val gmsCoreSupport = gmsCoreSupportFor(
    PINTEREST,
    pinterestSettings,
    signature = PINTEREST_SIGNATURE,
    defaultPackageName = "app.reseam.android.pinterest",
    mainActivityOnCreate,
) {
    // On Android 14+ the system Credential Manager only reaches Google Play services, never GmsCore.
    platformCredentialProvider.method("isAvailableOnDevice").alwaysReturn(false)
}

private val platformCredentialProvider = klass("platformCredentialProvider") {
    strings("androidx.credentials.BUNDLE_KEY_PREFER_IMMEDIATELY_AVAILABLE_CREDENTIALS")
    hasInstanceField("android.credentials.CredentialManager")
}
