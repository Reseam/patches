// SPDX-FileCopyrightText: 2026 AunAli K. <hello@auna.li>
// SPDX-License-Identifier: GPL-3.0-or-later

package app.reseam.patches.universal

import app.reseam.patch.ExtClass
import app.reseam.patch.Type
import app.reseam.patch.patch

private const val PACKAGE_MANAGER = "android.content.pm.PackageManager"
private const val PACKAGE_INFO = "android.content.pm.PackageInfo"
private const val SIGNING_INFO = "android.content.pm.SigningInfo"
private const val SIGNATURES = "android.content.pm.Signature[]"

private object OriginalSignature : ExtClass("app.reseam.universal.signature.OriginalSignature") {
    val ownPackage = static("ownPackage", returns = Type.String)
    val certificates = static("certificates", returns = Type.String)
    val getPackageInfo = static("getPackageInfo", PACKAGE_MANAGER, Type.String, Type.Int, returns = PACKAGE_INFO)
    val getPackageInfoWithFlags = static("getPackageInfo", PACKAGE_MANAGER, Type.String, "$PACKAGE_MANAGER\$PackageInfoFlags", returns = PACKAGE_INFO)
    val hasSigningCertificate = static("hasSigningCertificate", PACKAGE_MANAGER, Type.String, "[B", Type.Int, returns = Type.Boolean)
    val getApkContentsSigners = static("getApkContentsSigners", SIGNING_INFO, returns = SIGNATURES)
    val getSigningCertificateHistory = static("getSigningCertificateHistory", SIGNING_INFO, returns = SIGNATURES)
    val hasMultipleSigners = static("hasMultipleSigners", SIGNING_INFO, returns = Type.Boolean)
}

val spoofSignature = patch("Spoof signature") {
    description("Shows the app the signature it was published with, so checks for a modified app pass.")

    execute {
        val packageName = requireNotNull(manifest.packageName) { "The manifest names no package" }
        val certificates = files.signers()
        require(certificates.isNotEmpty()) { "The APK has no v2 or v3 signature to keep" }
        val encoded = certificates.joinToString(",") { certificate -> certificate.joinToString("") { "%02x".format(it) } }

        OriginalSignature.ownPackage.implement { returnValue(string(packageName)) }
        OriginalSignature.certificates.implement { returnValue(string(encoded)) }
        val redirected = with(OriginalSignature) {
            bytecode.redirectInstanceCalls(
                getPackageInfo, getPackageInfoWithFlags, hasSigningCertificate,
                getApkContentsSigners, getSigningCertificateHistory, hasMultipleSigners,
            )
        }
        log.info("Redirected $redirected signature lookups")
    }
}
