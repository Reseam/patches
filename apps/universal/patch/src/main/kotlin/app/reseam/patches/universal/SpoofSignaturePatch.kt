// SPDX-FileCopyrightText: 2026 Reseam contributors <https://reseam.app>
// SPDX-License-Identifier: AGPL-3.0-or-later

package app.reseam.patches.universal

import app.reseam.patch.patch
import app.reseam.universal.signature.bindings.OriginalSignature

private const val PACKAGE_MANAGER = "android.content.pm.PackageManager"
private const val PACKAGE_INFO = "android.content.pm.PackageInfo"
private const val SIGNING_INFO = "android.content.pm.SigningInfo"
private const val SIGNATURES = "android.content.pm.Signature[]"
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
