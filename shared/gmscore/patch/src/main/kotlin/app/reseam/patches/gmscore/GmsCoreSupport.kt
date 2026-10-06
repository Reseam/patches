// SPDX-FileCopyrightText: 2026 AunAli K. <hello@auna.li>
// SPDX-License-Identifier: GPL-3.0-or-later

package app.reseam.patches.gmscore

import app.reseam.patch.CompatiblePackage
import app.reseam.patch.ExtClass
import app.reseam.patch.MethodTarget
import app.reseam.patch.PatchRuntime
import app.reseam.patch.ReseamPatch
import app.reseam.patch.Type
import app.reseam.patch.XmlElement
import app.reseam.patch.after
import app.reseam.patch.alwaysReturn
import app.reseam.patch.before
import app.reseam.patch.method
import app.reseam.patch.patch
import app.reseam.patch.point
import app.reseam.patch.points

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

private const val GMS_CORE_MISSING_ACTIVITY = "app.reseam.gmscore.GmsCoreMissingActivity"

private val PERMISSION_DECLARATIONS = setOf("permission", "uses-permission", "uses-permission-sdk-23")
private val PERMISSION_ATTRIBUTES = listOf("android:permission", "android:readPermission", "android:writePermission")

/** [signature] is the SHA-1 of the certificate Google's servers know the app by. */
fun gmsCoreSupportFor(
    app: CompatiblePackage,
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
    val vendorGroupId = stringOption(
        "gmsCoreVendorGroupId",
        title = "GmsCore vendor group ID",
        description = "The group ID the installed GmsCore build was published under",
        default = "app.revanced",
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

        fun rename(value: String): String? = when {
            value in GMS_PACKAGES || GMS_NAMESPACES.any { value.startsWith("$it.") } ->
                vendor + value.removePrefix("com.google")
            value == originalPackage || value.startsWith("$originalPackage.") ->
                newPackage + value.removePrefix(originalPackage)
            else -> null
        }

        // Code refers to permissions and authorities by literal, so whatever the manifest renames
        // has to be renamed in the bytecode too.
        val renamed = mutableSetOf<String>()
        fun rewrite(element: XmlElement, attribute: String) {
            val value = element[attribute] ?: return
            val new = rename(value) ?: return
            element[attribute] = new
            renamed += value
        }

        // The platform refuses a split set whose APKs disagree on the package name, so this lands
        // in every component's manifest, not the base alone.
        for (component in manifest.components()) {
            manifest.component(component).edit {
                root["package"] = newPackage
                for (element in root.descendants()) {
                    if (element.tag in PERMISSION_DECLARATIONS) rewrite(element, "android:name")
                    PERMISSION_ATTRIBUTES.forEach { rewrite(element, it) }
                    val authorities = element["android:authorities"] ?: continue
                    element["android:authorities"] = authorities.split(";").joinToString(";") { authority ->
                        rename(authority)?.also { renamed += authority } ?: authority
                    }
                }
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

        val constants = renamed + GMS_PACKAGES + GMS_AUTHORITIES
        val count = constants.sumOf { old -> rename(old)?.let { bytecode.replaceAllStrings(old, it) } ?: 0 }
        log.info("Rewrote $count constants across ${constants.size} package, permission and authority names")

        // An authority also appears inside the content:// URI built around it, which a whole-string
        // replace cannot reach.
        val uris = bytecode.replaceStringsContaining("content://") { uri ->
            if (!uri.startsWith("content://")) return@replaceStringsContaining null
            val rest = uri.removePrefix("content://")
            val authority = rest.takeWhile { it != '/' && it != '?' && it != '#' }
            rename(authority)?.let { "content://$it${rest.removePrefix(authority)}" }
        }
        log.info("Rewrote $uris content:// URIs")

        // Play Services is not installed, so every check for it has to stop reporting that.
        playServicesCheck.alwaysReturn()
        castContextFetch.alwaysReturn()
        playServicesAvailability.alwaysReturn(0)

        // Primes only initializes in an app it recognizes as Google's, judged by the installed
        // package, which neither this rename nor a later Change package name keeps on its list.
        primePackages.alwaysReturn(true)

        // Notification registration is keyed on the package Google's servers know.
        gnpPackageName.after { capture("packageName").assign(string(originalPackage)) }

        GmsCoreSupport.vendorGroupId.implement { returnValue(string(vendor)) }
        GmsCoreSupport.originalPackageName.implement { returnValue(string(originalPackage)) }

        // GmsCore can serve a Gservices key with no value, and the cache the rows land in rejects
        // nulls, which crashes the app at launch and on every background refresh.
        gservicesCacheFill.before { call(GmsCoreSupport.dropNullValues, capture("rows")) }

        mainActivityOnCreate.after { call(GmsCoreSupport.check, thisObject) }
        // An empty affinity puts it in its own task, out of reach of the app closing its own.
        manifest.addActivity(GMS_CORE_MISSING_ACTIVITY) {
            this["android:theme"] = "@android:style/Theme.Translucent.NoTitleBar"
            this["android:taskAffinity"] = ""
            this["android:excludeFromRecents"] = "true"
        }

        extra()
    }
}

// GooglePlayServicesUtil.ensurePlayServicesAvailable, which throws when they are not.
private val playServicesCheck = method("playServicesCheck") {
    strings("Google Play Services not available")
    returns(Type.Void)
    params(Type.Context, Type.Int)
}

private val castContextFetch = method("castContextFetch") {
    strings("Error fetching CastContext.")
    returns(Type.Void)
}

// GooglePlayServicesUtilLight.isGooglePlayServicesAvailable: a ConnectionResult status code where
// zero is SUCCESS, not a boolean.
private val playServicesAvailability = method("playServicesAvailability") {
    strings("com.google.android.gms.version")
    params(Type.Context, Type.Int)
    returns(Type.Int)
}

// The "prime" method, which matches the running package against the apps Google ships.
private val primePackages = method("primePackages") {
    strings("com.google.android.GoogleCamera")
    returns(Type.Boolean)
}

private val gnpPackageName = method("gnpRegistration") { strings("Exception reading GServices key.") }
    .points("notificationPackageName") {
        invokeVirtual { owner(Type.Context); name("getPackageName"); params(); returns(Type.String) }
    }.single()
    .next { resultOf(Type.String) }
    .captureAs("packageName", Type.String)

// The Gservices prefix query, which copies the rows it read into the app's cache.
private val gservicesCacheFill = method("gservicesPrefixQuery") {
    strings("ContentProvider query returned null cursor")
    returns(Type.Void)
    params("android.content.ContentResolver", "java.lang.String[]")
}.point("gservicesCacheFill") { invokeInterface { name("putAll"); params(Type.Map) } }
    .captureArgumentAs("rows", 1, Type.Map)

private fun XmlElement.descendants(): Sequence<XmlElement> =
    sequenceOf(this) + children.asSequence().flatMap { it.descendants() }

private object GmsCoreSupport : ExtClass("app.reseam.gmscore.GmsCoreSupport") {
    val check by static(Type.Activity)
    val vendorGroupId by static(returns = Type.String)
    val originalPackageName by static(returns = Type.String)
    val dropNullValues by static(Type.Map)
}
