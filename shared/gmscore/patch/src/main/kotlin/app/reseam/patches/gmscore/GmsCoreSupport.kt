// SPDX-FileCopyrightText: 2026 Cossale <hello@auna.li>
// SPDX-License-Identifier: AGPL-3.0-or-later

package app.reseam.patches.gmscore

import app.reseam.gmscore.bindings.GmsCoreSupport
import app.reseam.patch.CompatiblePackage
import app.reseam.patch.MethodTarget
import app.reseam.patch.PatchRuntime
import app.reseam.patch.ReseamPatch
import app.reseam.patch.after
import app.reseam.patch.alwaysReturn
import app.reseam.patch.patch
import app.reseam.patch.settings.SettingsHost

private const val GMS_CORE_MISSING_ACTIVITY = "app.reseam.gmscore.GmsCoreMissingActivity"

/**
 * [signature] is the SHA-1 of the certificate Google's servers know the app by. [settings] starts
 * the app runtime and ships [GmsCoreStrings], which the startup check's dialogs read.
 */
fun gmsCoreSupportFor(
    app: CompatiblePackage,
    settings: SettingsHost,
    signature: String,
    defaultPackageName: String,
    mainActivityOnCreate: MethodTarget,
    extra: PatchRuntime.() -> Unit = {},
): ReseamPatch = patch("GmsCore support") {
    description(
        """
        Installs alongside the Play Store app under its own package name and signs in through
        GmsCore instead of Google Play Services. GmsCore has to be installed separately.
        """,
    )
    compatibleWith(app)
    dependsOn(settings)
    val vendorGroupId = stringOption(
        "gmsCoreVendorGroupId",
        title = "GmsCore vendor group ID",
        description = "The group ID the installed GmsCore build was published under",
        default = "app.reseam",
        required = true,
    )
    val packageName = stringOption(
        "packageName",
        title = "Package name",
        description = "New package name for the patched app",
        default = defaultPackageName,
        required = true,
    )

    execute {
        val vendor = options[vendorGroupId]
        val newPackage = options[packageName]
        val originalPackage = app.name

        val names = GmsCoreNames(vendor, newPackage, originalPackage)

        // The platform refuses a split set whose APKs disagree on the package name, so this lands
        // in every component's manifest, not the base alone.
        for (component in manifest.components()) {
            manifest.component(component).edit {
                root["package"] = newPackage
                names.rewrite(root)
            }
        }

        // The app looks up its own resources by name under the installed package name, so the
        // resource table is renamed with the manifest; otherwise its icons resolve to nothing.
        resources.components().forEach { resources.component(it).setPackageName(newPackage) }

        manifest.edit {
            val gmsCore = "$vendor.android.gms"
            val queries = findByTag("queries").firstOrNull() ?: createElement("queries").also { root.appendChild(it) }
            queries.appendChild(createElement("package").apply { this["android:name"] = gmsCore })

            // GmsCore signs in as the app Google knows, not as the renamed clone.
            val application = findByTag("application").firstOrNull() ?: error("manifest has no <application> element")
            for ((name, value) in listOf(
                "$gmsCore.SPOOFED_PACKAGE_NAME" to originalPackage,
                "$gmsCore.SPOOFED_PACKAGE_SIGNATURE" to signature,
            )) {
                application.appendChild(
                    createElement("meta-data").apply {
                        this["android:name"] = name
                        this["android:value"] = value
                    },
                )
            }
        }

        val constants = names.constants
        val count = constants.sumOf { old -> names.rename(old)?.let { bytecode.replaceAllStrings(old, it) } ?: 0 }
        log.info("Rewrote $count constants across ${constants.size} package, permission and authority names")

        // An authority also appears inside the content:// URI built around it, which a whole-string
        // replace cannot reach.
        val uris = bytecode.replaceStringsStartingWith("content://") { names.renameUri(it) }
        log.info("Rewrote $uris content:// URIs")

        // Play Services is not installed, so every check for it has to stop reporting that.
        playServicesCheck.alwaysReturn()
        playServicesAvailability.alwaysReturn(0)

        GmsCoreSupport.vendorGroupId.implement { returnValue(string(vendor)) }
        GmsCoreSupport.originalPackageName.implement { returnValue(string(originalPackage)) }

        mainActivityOnCreate.after { GmsCoreSupport.check(thisObject) }
        // An empty affinity puts it in its own task, out of reach of the app closing its own.
        manifest.addActivity(GMS_CORE_MISSING_ACTIVITY) {
            this["android:theme"] = "@android:style/Theme.Translucent.NoTitleBar"
            this["android:taskAffinity"] = ""
            this["android:excludeFromRecents"] = "true"
        }

        extra()
    }
}
