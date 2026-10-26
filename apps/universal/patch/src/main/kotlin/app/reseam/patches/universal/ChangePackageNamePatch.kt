// SPDX-FileCopyrightText: 2026 Cossale <hello@auna.li>
// SPDX-License-Identifier: AGPL-3.0-or-later

package app.reseam.patches.universal

import app.reseam.patch.XmlElement
import app.reseam.patch.methods
import app.reseam.patch.patch
import app.reseam.patch.resourceRef

private val PACKAGE_NAME = Regex("^[a-z]\\w*(\\.[a-z]\\w*)+$")

// A declared permission or authority prefixed by the package follows the package when it moves, so
// the app still grants itself what it declared and still binds to its own providers.
private val PERMISSION_DECLARATIONS = setOf("permission", "uses-permission", "uses-permission-sdk-23")
private val PERMISSION_ATTRIBUTES = listOf("android:permission", "android:readPermission", "android:writePermission")

val changePackageName = patch("Change package name") {
    description(
        "Installs the app under a new package name, so a clone runs alongside the original. " +
            "Without a chosen name, \".reseam\" is added to the original one. Not every app works under a different name.",
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
        fun renameName(value: String) = rename(value)?.also { literals += value } ?: value

        // A renamed value still starts with the original package, so a shared resource is renamed once.
        val renamedResources = mutableSetOf<UInt>()
        fun renameResource(id: UInt, transform: (String) -> String) {
            if (renamedResources.add(id)) resources.getString(id)?.let { resources.setString(id, transform(it)) }
        }
        fun rewrite(element: XmlElement, attribute: String, transform: (String) -> String = ::renameName) {
            val value = element[attribute] ?: return
            when (val id = resourceRef(value)) {
                null -> transform(value).takeIf { it != value }?.let { element[attribute] = it }
                else -> renameResource(id, transform)
            }
        }

        // The platform refuses a split set whose APKs disagree on the package name, so this lands
        // in every component's manifest, not the base alone.
        for (component in manifest.components()) {
            manifest.component(component).edit {
                root["package"] = renamed
                for (element in root.descendants()) {
                    if (element.tag in PERMISSION_DECLARATIONS) rewrite(element, "android:name")
                    PERMISSION_ATTRIBUTES.forEach { rewrite(element, it) }
                    rewrite(element, "android:authorities") { it.split(";").joinToString(";", transform = ::renameName) }
                }
            }
        }

        // The app looks up its own resources by name under the installed package name, so the
        // resource table is renamed with the manifest; otherwise its resources resolve to nothing.
        resources.components().forEach { resources.component(it).setPackageName(renamed) }

        // Only the first app installed with an account type can use it, so the clone needs its own.
        val accountTypes = mutableMapOf<String, String>()
        fun accountType(type: String) = accountTypes.getOrPut(type) { rename(type) ?: "$renamed.$type" }

        // An explicit intent in compiled XML, such as a preference's <intent>, names the package
        // to launch. Android 14 refuses implicit intents to unexported activities, so apps bake it.
        var intents = 0
        for (component in files.components()) {
            val apk = files.component(component)
            for (path in apk.xmlFilesWith(TARGET_PACKAGE, ACCOUNT_TYPE)) {
                if (!path.startsWith("res/")) continue
                apk.editXml(path) {
                    findByAttribute(TARGET_PACKAGE, original).forEach {
                        it[TARGET_PACKAGE] = renamed
                        intents++
                    }
                    for (element in ACCOUNT_TYPE_DECLARATIONS.flatMap(::findByTag)) {
                        val type = element[ACCOUNT_TYPE] ?: continue
                        when (val id = resourceRef(type)) {
                            null -> if (rename(type) != null) element[ACCOUNT_TYPE] = accountType(type)
                            else -> renameResource(id, ::accountType)
                        }
                    }
                }
            }
        }

        val count = literals.sumOf { old -> rename(old)?.let { bytecode.replaceAllStrings(old, it) } ?: 0 }

        // The package name has other uses, so only account API callers get the new type.
        val typeLiterals = accountTypes.entries.sumOf { (old, new) ->
            if (old != original) return@sumOf bytecode.replaceAllStrings(old, new)
            ACCOUNT_APIS.flatMap { api -> methods { strings(old); calls { owner(api) } }.all }
                .distinctBy { it.descriptor }
                .sumOf { it.method.replaceAllStrings(old, new) }
        }

        // An authority also appears inside the content:// URI built around it, which a whole-string
        // replace cannot reach.
        val uris = bytecode.replaceStringsStartingWith("content://") { uri ->
            val rest = uri.removePrefix("content://")
            val authority = rest.takeWhile { it != '/' && it != '?' && it != '#' }
            rename(authority)?.let { "content://$it${rest.removePrefix(authority)}" }
        }

        log.info(
            "Renamed $original to $renamed across ${literals.size} names, $count literals, " +
                "$uris content:// URIs, $intents intents and ${accountTypes.size} account types " +
                "($typeLiterals literals)",
        )
    }
}

private const val TARGET_PACKAGE = "android:targetPackage"

private const val ACCOUNT_TYPE = "android:accountType"
private val ACCOUNT_TYPE_DECLARATIONS = listOf("account-authenticator", "sync-adapter")
private val ACCOUNT_APIS = listOf("android.accounts.AccountManager", "android.accounts.Account")

private fun XmlElement.descendants(): Sequence<XmlElement> =
    sequenceOf(this) + children.asSequence().flatMap { it.descendants() }
