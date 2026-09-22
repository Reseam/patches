// SPDX-FileCopyrightText: 2026 AunAli K. <hello@auna.li>
// SPDX-License-Identifier: GPL-3.0-or-later

package app.reseam.patches.instagram.customization

import app.reseam.patch.patch
import app.reseam.patch.resourceRef
import app.reseam.patches.instagram.core.INSTAGRAM
import app.reseam.patches.instagram.core.INSTAGRAM_PACKAGE
import app.reseam.patches.instagram.core.signatureCheck

val cloneInstagram = patch("Clone Instagram") {
    description("Allows the app to be installed alongside the official Instagram app with a distinct package name")
    compatibleWith(INSTAGRAM)
    dependsOn(signatureCheck)
    val packageName = stringOption(
        "packageName",
        title = "Package name",
        description = "New package name for the cloned app",
        default = "com.instagram.android.clone",
    )
    val appName = stringOption(
        "appName",
        title = "App name",
        description = "Display name for the cloned app",
        default = "Instagram Clone",
    )

    execute {
        val newPackage = options[packageName]
        val newName = options[appName]

        // Every provider authority must be unique on the device, so all of them move under the
        // clone package. The ones code refers to by literal are re-pointed in bytecode below.
        val authorityRenames = mutableMapOf<String, String>()
        var appLabelResId: UInt? = null

        // The platform refuses a split set whose APKs disagree on the package name, so the
        // rename lands in every component's manifest, not the base alone.
        for (component in manifest.components()) {
            manifest.component(component).edit {
                root["package"] = newPackage

                findByTag("application").firstOrNull()?.get("android:label")?.let(::resourceRef)?.let { appLabelResId = it }

                findByTag("provider").forEach { provider ->
                    val authorities = provider["android:authorities"] ?: return@forEach
                    provider["android:authorities"] = authorities.split(";").joinToString(";") { single ->
                        when {
                            single.isEmpty() -> single
                            INSTAGRAM_PACKAGE in single -> single.replace(INSTAGRAM_PACKAGE, newPackage)
                            single.startsWith("com.instagram.") ->
                                ("$newPackage." + single.removePrefix("com.instagram.")).also { authorityRenames[single] = it }
                            else -> "$newPackage.$single".also { authorityRenames[single] = it }
                        }
                    }
                }

                for (tag in listOf("permission", "uses-permission")) {
                    findByTag(tag).forEach { element ->
                        val name = element["android:name"] ?: return@forEach
                        if (INSTAGRAM_PACKAGE in name) element["android:name"] = name.replace(INSTAGRAM_PACKAGE, newPackage)
                    }
                }
            }
        }

        // Instagram strips resource key names, so the label is renamed through the resource id
        // the manifest carries rather than by name.
        val labelId = appLabelResId
        val poolIndex = labelId?.let { resources.poolAdd(newName) }
        when {
            labelId == null -> log.warn("Could not resolve app label resource ID from manifest")
            poolIndex == null -> log.warn("Could not add app name to resource string pool")
            else -> resources.replaceEntry(labelId, poolIndex)
        }

        val replaced = bytecode.replaceAllStrings(INSTAGRAM_PACKAGE, newPackage)
        log.info("Replaced $replaced package name references in bytecode")

        // Runs after the package pass: the new authorities contain the package name and would
        // otherwise be rewritten twice.
        authorityRenames.forEach { (old, new) ->
            val count = bytecode.replaceAllStrings(old, new)
            log.info("Renamed provider authority '$old' -> '$new' ($count bytecode refs)")
        }
    }
}
