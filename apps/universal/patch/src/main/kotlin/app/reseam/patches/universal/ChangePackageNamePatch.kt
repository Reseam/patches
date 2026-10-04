// SPDX-FileCopyrightText: 2026 AunAli K. <hello@auna.li>
// SPDX-FileCopyrightText: 2026 ReVanced contributors
// SPDX-License-Identifier: GPL-3.0-or-later

package app.reseam.patches.universal

import app.reseam.patch.XmlElement
import app.reseam.patch.patch

private val PACKAGE_NAME = Regex("^[a-z]\\w*(\\.[a-z]\\w*)+$")

// A declared permission or authority prefixed by the package follows the package when it moves, so
// the app still grants itself what it declared and still binds to its own providers.
private val PERMISSION_DECLARATIONS = setOf("permission", "uses-permission", "uses-permission-sdk-23")
private val PERMISSION_ATTRIBUTES = listOf("android:permission", "android:readPermission", "android:writePermission")

val changePackageName = patch("Change package name") {
    description(
        "Installs the app under a new package name, so a clone runs alongside the original. " +
            "Appends \".reseam\" by default. Some apps misbehave under a changed package name.",
    )

    val packageNameOption = stringOption(
        "packageName",
        title = "Package name",
        description = "The new package name, or empty to append \".reseam\" to the original.",
    )

    // After every other patch, so they read and edit the app under its original package name.
    afterDependents {
        val original = requireNotNull(manifest.packageName) { "The manifest names no package" }
        val chosen = options.getOrNull(packageNameOption)?.takeIf { it.isNotBlank() }?.trim()
        if (chosen != null) require(chosen.matches(PACKAGE_NAME)) { "'$chosen' is not a valid package name" }
        val renamed = chosen ?: "$original.reseam"
        if (renamed == original) return@afterDependents

        fun rename(value: String): String? =
            if (value == original || value.startsWith("$original.")) renamed + value.removePrefix(original) else null

        // Code refers to permissions and authorities by literal, so whatever the manifest renames
        // has to be renamed in the bytecode too.
        val literals = mutableSetOf<String>()
        fun rewrite(element: XmlElement, attribute: String) {
            val value = element[attribute] ?: return
            val new = rename(value) ?: return
            element[attribute] = new
            literals += value
        }

        // The platform refuses a split set whose APKs disagree on the package name, so this lands
        // in every component's manifest, not the base alone.
        for (component in manifest.components()) {
            manifest.component(component).edit {
                root["package"] = renamed
                for (element in root.descendants()) {
                    if (element.tag in PERMISSION_DECLARATIONS) rewrite(element, "android:name")
                    PERMISSION_ATTRIBUTES.forEach { rewrite(element, it) }
                    val authorities = element["android:authorities"] ?: continue
                    element["android:authorities"] = authorities.split(";").joinToString(";") { authority ->
                        rename(authority)?.also { literals += authority } ?: authority
                    }
                }
            }
        }

        // The app looks up its own resources by name under the installed package name, so the
        // resource table is renamed with the manifest; otherwise its resources resolve to nothing.
        resources.components().forEach { resources.component(it).setPackageName(renamed) }

        val count = literals.sumOf { old -> rename(old)?.let { bytecode.replaceAllStrings(old, it) } ?: 0 }

        // An authority also appears inside the content:// URI built around it, which a whole-string
        // replace cannot reach.
        val uris = bytecode.replaceStringsContaining("content://") { uri ->
            if (!uri.startsWith("content://")) return@replaceStringsContaining null
            val rest = uri.removePrefix("content://")
            val authority = rest.takeWhile { it != '/' && it != '?' && it != '#' }
            rename(authority)?.let { "content://$it${rest.removePrefix(authority)}" }
        }

        log.info("Renamed $original to $renamed across ${literals.size} names, $count literals and $uris content:// URIs")
    }
}

private fun XmlElement.descendants(): Sequence<XmlElement> =
    sequenceOf(this) + children.asSequence().flatMap { it.descendants() }
