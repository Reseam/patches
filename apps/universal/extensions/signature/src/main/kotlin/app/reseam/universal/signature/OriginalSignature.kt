// SPDX-FileCopyrightText: 2026 Reseam contributors <https://reseam.app>
// SPDX-License-Identifier: AGPL-3.0-or-later

@file:JvmName("OriginalSignature")

package app.reseam.universal.signature

import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.content.pm.Signature
import android.content.pm.SigningInfo
import app.reseam.runtime.Export
import java.security.MessageDigest
import java.util.Collections
import java.util.WeakHashMap

private val own =
    Collections.synchronizedSet(Collections.newSetFromMap(WeakHashMap<SigningInfo, Boolean>()))

@Export fun ownPackage(): String? = null

@Export fun certificates(): String? = null

@Export
@Throws(PackageManager.NameNotFoundException::class)
fun getPackageInfo(manager: PackageManager, packageName: String, flags: Int): PackageInfo =
    spoofed(manager.getPackageInfo(packageName, flags))

@Export(name = "getPackageInfoWithFlags")
@Throws(PackageManager.NameNotFoundException::class)
fun getPackageInfo(
    manager: PackageManager,
    packageName: String,
    flags: PackageManager.PackageInfoFlags,
): PackageInfo = spoofed(manager.getPackageInfo(packageName, flags))

@Export
fun hasSigningCertificate(
    manager: PackageManager,
    packageName: String,
    certificate: ByteArray,
    type: Int,
): Boolean {
    if (packageName != ownPackage())
        return manager.hasSigningCertificate(packageName, certificate, type)
    return originalSignatures.any { signature ->
        val bytes = signature.toByteArray()
        val candidate = if (type == PackageManager.CERT_INPUT_SHA256) sha256(bytes) else bytes
        candidate.contentEquals(certificate)
    }
}

@Export
fun getApkContentsSigners(info: SigningInfo): Array<Signature>? =
    if (info in own) originalSignatures.clone() else info.apkContentsSigners

@Export
fun getSigningCertificateHistory(info: SigningInfo): Array<Signature>? =
    if (info in own) originalSignatures.clone() else info.signingCertificateHistory

@Export
fun hasMultipleSigners(info: SigningInfo): Boolean =
    if (info in own) originalSignatures.size > 1 else info.hasMultipleSigners()

private fun spoofed(info: PackageInfo): PackageInfo {
    if (checkNotNull(info.packageName) { "Package info has no name" } != ownPackage()) return info
    if (info.signatures != null) info.signatures = originalSignatures.clone()
    info.signingInfo?.let(own::add)
    return info
}

private val originalSignatures by lazy {
    checkNotNull(certificates()) { "Original certificates are missing" }
        .split(',')
        .map(::Signature)
        .toTypedArray()
}

private fun sha256(data: ByteArray): ByteArray = MessageDigest.getInstance("SHA-256").digest(data)
