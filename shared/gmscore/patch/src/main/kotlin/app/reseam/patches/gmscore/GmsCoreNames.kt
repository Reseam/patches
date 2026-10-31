// SPDX-FileCopyrightText: 2026 Reseam contributors <https://reseam.app>
// SPDX-License-Identifier: AGPL-3.0-or-later

package app.reseam.patches.gmscore

import app.reseam.patch.XmlElement

// GmsCore is Play Services rebuilt under the vendor's own group id: everything Google publishes
// below com.google.android.* exists there below <vendor>.android.*. Moving the app's references
// from one namespace to the other is what makes it talk to GmsCore.
private val GMS_NAMESPACES = listOf(
    "com.google.android.gms",
    "com.google.android.gsf",
    "com.google.android.c2dm",
    "com.google.android.googleapps",
    "com.google.android.gtalkservice",
    "com.google.android.providers.gsf",
    "com.google.android.providers.settings",
    "com.google.firebase.auth.api.gms",
)

// The Google account type and the package the Play Services client binds to.
private val GMS_PACKAGES = listOf("com.google", "com.google.android.gms")

// Play Services providers the app reads by literal without declaring them, so the manifest pass
// never sees them. The font provider matters most: the app's icons are a downloadable font, and a
// request naming GmsCore's package with Play Services' authority resolves nothing.
private val GMS_AUTHORITIES = listOf(
    "com.google.android.gms.auth.accounts",
    "com.google.android.gms.chimera",
    "com.google.android.gms.fonts",
    "com.google.android.gms.phenotype",
    "com.google.android.gsf.gservices",
)

private val PERMISSION_DECLARATIONS = setOf("permission", "uses-permission", "uses-permission-sdk-23")
private val PERMISSION_ATTRIBUTES = listOf("android:permission", "android:readPermission", "android:writePermission")

internal class GmsCoreNames(private val vendor: String, private val newPackage: String, private val originalPackage: String) {
    private val renamed = mutableSetOf<String>()
    val constants: Set<String> get() = renamed + GMS_PACKAGES + GMS_AUTHORITIES

    fun rename(value: String): String? = when {
        value in GMS_PACKAGES || GMS_NAMESPACES.any { value.startsWith("$it.") } ->
            vendor + value.removePrefix("com.google")
        value == originalPackage || value.startsWith("$originalPackage.") ->
            newPackage + value.removePrefix(originalPackage)
        else -> null
    }

    fun rewrite(root: XmlElement) {
        root.descendants().forEach { element ->
            if (element.tag in PERMISSION_DECLARATIONS) rewriteAttribute(element, "android:name")
            PERMISSION_ATTRIBUTES.forEach { rewriteAttribute(element, it) }
            element["android:authorities"]?.let { authorities ->
                element["android:authorities"] = authorities.split(";").joinToString(";") { authority ->
                    rename(authority)?.also { renamed += authority } ?: authority
                }
            }
        }
    }

    private fun rewriteAttribute(element: XmlElement, attribute: String) {
        val value = element[attribute] ?: return
        val new = rename(value) ?: return
        element[attribute] = new
        renamed += value
    }

    fun renameUri(uri: String): String? {
        if (!uri.startsWith("content://")) return null
        val rest = uri.removePrefix("content://")
        val authority = rest.takeWhile { it != '/' && it != '?' && it != '#' }
        return rename(authority)?.let { "content://$it${rest.removePrefix(authority)}" }
    }
}

private fun XmlElement.descendants(): Sequence<XmlElement> =
    sequenceOf(this) + children.asSequence().flatMap { it.descendants() }
