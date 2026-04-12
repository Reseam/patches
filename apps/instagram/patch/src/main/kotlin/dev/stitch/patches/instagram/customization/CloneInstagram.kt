/*
 * Stitch — part of the Stitch Android patcher.
 * Copyright (C) 2026 Aunali321 <accounts@auna.li>
 *
 * This file is part of Stitch.
 *
 * Stitch is free software: you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Stitch is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 *
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package dev.stitch.patches.instagram.customization

import dev.stitch.patches.instagram.core.signatureCheckPatch

import dev.stitch.patch.compatibleWith
import dev.stitch.patch.optionsOf
import dev.stitch.patch.patch
import dev.stitch.patch.stringOption

private const val INSTAGRAM_PACKAGE = "com.instagram.android"

val cloneInstagram = patch(
    name = "Clone Instagram",
    description = "Allows the app to be installed alongside the official Instagram app with a distinct package name",
    compatibleWith = listOf(compatibleWith(INSTAGRAM_PACKAGE)),
    dependsOn = listOf(signatureCheckPatch),
    options = optionsOf(
        stringOption(
            "packageName",
            default = "com.instagram.android.clone",
            title = "Package name",
            description = "New package name for the cloned app",
        ),
        stringOption(
            "appName",
            default = "Instagram Clone",
            title = "App name",
            description = "Display name for the cloned app",
        ),
    ),
) {
    execute { ctx ->
        val newPackage = ctx.options.string("packageName")!!
        val newName = ctx.options.string("appName")!!

        // Rewrite manifest: package attribute, provider authorities, app-specific permissions.
        // While open, also capture the app label resource ID so we can rename the app in
        // the resource table without relying on a key name (Instagram strips resource keys).
        var appLabelResId: UInt? = null
        ctx.manifest.document().use { doc ->
            doc.root["package"] = newPackage

            val appEl = doc.findByTag("application").firstOrNull()
            val labelRef = appEl?.get("android:label")
            if (labelRef != null && labelRef.startsWith("@0x")) {
                appLabelResId = labelRef.removePrefix("@0x").toUIntOrNull(16)
            }

            doc.findByTag("provider").forEach { provider ->
                val auth = provider["android:authorities"] ?: return@forEach
                if (INSTAGRAM_PACKAGE in auth) {
                    provider["android:authorities"] = auth.replace(INSTAGRAM_PACKAGE, newPackage)
                }
            }

            for (tag in listOf("permission", "uses-permission")) {
                doc.findByTag(tag).forEach { el ->
                    val name = el["android:name"] ?: return@forEach
                    if (INSTAGRAM_PACKAGE in name) {
                        el["android:name"] = name.replace(INSTAGRAM_PACKAGE, newPackage)
                    }
                }
            }
        }

        // Update the app display name. Instagram strips resource key names, so we resolve
        // the label resource ID captured from the manifest and redirect it to a new pool entry.
        val resId = appLabelResId
        if (resId != null) {
            val poolIdx = ctx.resources.poolAdd(newName)
            if (poolIdx != null) {
                ctx.resources.replaceEntry(resId, poolIdx)
                ctx.log.info("App name set via resource 0x${resId.toString(16)}")
            } else {
                ctx.log.warn("Could not add app name to resource string pool")
            }
        } else {
            ctx.log.warn("Could not resolve app label resource ID from manifest")
        }

        // Replace the package name string constant in all bytecode methods.
        // Instagram has 14 occurrences across 17 dex files.
        var replacedCount = 0
        for (cls in ctx.bytecode.classes) {
            for (method in cls.methods) {
                replacedCount += method.replaceAllStrings(INSTAGRAM_PACKAGE, newPackage)
            }
        }
        ctx.log.info("Replaced $replacedCount package name references in bytecode")
    }
}
