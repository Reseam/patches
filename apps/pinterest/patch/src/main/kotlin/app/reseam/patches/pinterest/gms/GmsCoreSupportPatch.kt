// SPDX-FileCopyrightText: 2026 Cossale <hello@auna.li>
// SPDX-License-Identifier: AGPL-3.0-or-later

package app.reseam.patches.pinterest.gms

import app.reseam.patch.Type
import app.reseam.patch.after
import app.reseam.patch.alwaysReturn
import app.reseam.patch.dex.methodRef
import app.reseam.patch.klass
import app.reseam.patch.method
import app.reseam.patch.methods
import app.reseam.patch.points
import app.reseam.patches.gmscore.gmsCoreSupportFor
import app.reseam.patches.pinterest.core.PINTEREST
import app.reseam.patches.pinterest.core.PINTEREST_PACKAGE
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
    // Pinterest's reCAPTCHA key allows only the original package name. The SDK's own PackageManager
    // lookups keep the installed one.
    val reported = recaptchaPackageReads.points { invokeVirtual { name("getPackageName") } }.all.filter { read ->
        val consumer = read.next { invoke() }.instruction.methodRef!!.definingClass
        consumer.startsWith(RECAPTCHA) || read.method.owner.startsWith(RECAPTCHA) && !consumer.startsWith("Landroid/content/pm/")
    }
    reported.forEach {
        it.next { resultOf(Type.String) }
            .captureAs("packageName", Type.String)
            .after { capture("packageName").assign(string(PINTEREST_PACKAGE)) }
    }
    log.info("reCAPTCHA reports $PINTEREST_PACKAGE at ${reported.size} calls")
}

private const val RECAPTCHA = "Lcom/google/android/recaptcha/"

private val recaptchaPackageReads = methods("recaptchaPackageReads") {
    calls { name("getPackageName") }
    custom { instructions.any { it.methodRef?.definingClass?.startsWith(RECAPTCHA) == true } }
}

private val platformCredentialProvider = klass("platformCredentialProvider") {
    strings("androidx.credentials.BUNDLE_KEY_PREFER_IMMEDIATELY_AVAILABLE_CREDENTIALS")
    hasInstanceField("android.credentials.CredentialManager")
}
