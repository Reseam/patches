// SPDX-FileCopyrightText: 2026 AunAli K. <hello@auna.li>
// SPDX-License-Identifier: GPL-3.0-or-later

package app.reseam.patches.x.core

import app.reseam.patch.Type
import app.reseam.patch.after
import app.reseam.patch.descriptor
import app.reseam.patch.dex.ref
import app.reseam.patch.fieldTarget
import app.reseam.patch.klass
import app.reseam.patch.method
import app.reseam.patch.patch
import app.reseam.patch.point

private const val FUNCTION0 = "kotlin.jvm.functions.Function0"
private const val LOGO_PATH = "res/drawable/reseam_logo.png"

// Defaulted constructor parameters, as Kotlin's synthetic constructors read them: bit i means "use
// the default for parameter i".
private const val ITEM_DEFAULTS_ACTION_AND_TRAILING = 0b1110000
private const val SECTION_DEFAULTS_HEADER_AND_SUBTITLE = 0b11

val settingsEntry = patch("Reseam entry in X settings") {
    description("Adds a Reseam Settings row to X's Settings and privacy screen.")
    compatibleWith(X)
    dependsOn(xSettings)

    execute {
        val logo = Resources::class.java.getResourceAsStream("/reseam-logo.png")?.use { it.readBytes() }
            ?: error("reseam-logo.png missing from patch jar resources")
        val logoId = resources.addFile("drawable", "reseam_logo", LOGO_PATH, logo)
        val iconType = settingsItemCtor.parameterTypes[2]

        // R8 renames kotlin.Unit.INSTANCE, so the Function0 body is emitted here.
        OpenReseamSettings.invoke.implement {
            call(XSettingsEntry.open)
            returnValue(staticField(unitInstance))
        }

        settingsRootPageSections.after {
            val title = newInstance(literalTextCtor.owner, literalTextCtor.proto, string("Reseam Settings"))
            val subtitle = newInstance(literalTextCtor.owner, literalTextCtor.proto, string("Ads, downloads, Premium, Grok, and privacy toggles."))
            val item = newInstance(
                settingsItemCtor.owner, settingsItemCtor.proto,
                title, subtitle, newInstance(iconType, "(I)V", int(logoId.toInt())), newInstance(OpenReseamSettings.descriptor), nullObject, nullObject,
                int(ITEM_DEFAULTS_ACTION_AND_TRAILING),
            )
            val section = newInstance(
                settingsSectionCtor.owner, settingsSectionCtor.proto,
                nullObject, nullObject, call(SettingsRows.single, item), int(SECTION_DEFAULTS_HEADER_AND_SUBTITLE),
            )
            capture("sections").assign(call(SettingsRows.withSection, capture("sections"), section))
        }
    }
}

// Three classes check these constructor arguments; the settings root also has a List getter.
val settingsRootComponent = klass("settingsRootComponent") {
    strings("settingsListComponentFactory", "subscriptionsFeatures", "inAppUpdateManager")
    rankBy("pageSections") { zeroArgListGetters() }
}
// The page factory reads the lazily built section list; the row is appended where it is read.
val settingsRootPage = method("settingsRootPage") {
    inClass(settingsRootComponent)
    strings("stackNavigator", "screenNavigator", "screenName")
}
val settingsRootPageSections = settingsRootPage.point { checkCast(Type.List) }.captureAs("sections", Type.List)

// The Additional resources page builds plain rows around stable URLs; its constructors give the model shapes.
val additionalResourcesRows = method("additionalResourcesRows") {
    strings("https://x.com/privacy", "https://business.x.com/help/troubleshooting/how-twitter-ads-work.html")
}
val settingsItemCtor = additionalResourcesRows.point { invokeDirect { name("<init>"); hasParam(FUNCTION0) } }.callee("settingsItemCtor")
val settingsSectionCtor = additionalResourcesRows.point { invokeDirect { name("<init>"); hasParam(Type.List); paramCount(4) } }.callee("settingsSectionCtor")
val literalTextCtor = additionalResourcesRows.point { invokeDirect { name("<init>"); params(Type.String) } }.callee("literalTextCtor")

private object Resources

val unitInstance = fieldTarget("unitInstance") {
    val unit = bytecode.findClass("kotlin.Unit") ?: error("kotlin.Unit missing")
    unit.staticFields.single { it.fieldType == descriptor("kotlin.Unit") }.ref
}
