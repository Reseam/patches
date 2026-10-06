// SPDX-FileCopyrightText: 2026 Cossale <hello@auna.li>
// SPDX-License-Identifier: AGPL-3.0-or-later

package app.reseam.patches.x.core

import app.reseam.patch.Type
import app.reseam.patch.before
import app.reseam.patch.descriptor
import app.reseam.patch.dex.Opcode
import app.reseam.patch.dex.ref
import app.reseam.patch.fieldTarget
import app.reseam.patch.klass
import app.reseam.patch.method
import app.reseam.patch.patch
import app.reseam.patch.point
import app.reseam.patch.settings.ReseamSettingsScreen

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
            call(ReseamSettingsScreen.open)
            returnValue(staticField(unitInstance))
        }

        SettingsRows.section.implement {
            val title = newInstance(literalTextCtor.owner, literalTextCtor.proto, string("Reseam Settings"))
            val subtitle = newInstance(literalTextCtor.owner, literalTextCtor.proto, string("Ads, downloads, Premium, Grok, and privacy toggles."))
            val item = newInstance(
                settingsItemCtor.owner, settingsItemCtor.proto,
                title, subtitle, newInstance(iconType, "(I)V", int(logoId.toInt())), newInstance(OpenReseamSettings.descriptor), nullObject, nullObject,
                int(ITEM_DEFAULTS_ACTION_AND_TRAILING),
            )
            returnValue(
                newInstance(
                    settingsSectionCtor.owner, settingsSectionCtor.proto,
                    nullObject, nullObject, call(SettingsRows.single, item), int(SECTION_DEFAULTS_HEADER_AND_SUBTITLE),
                ),
            )
        }

        val settingsTitle = resources.id("string", "settings") ?: error("string/settings missing")
        // Every settings page is a SettingsListState; the root page is the one titled Settings.
        settingsListState.method("<init>").before {
            whenEqual(paramOfType(resourceText.descriptor).field(resourceTextId), int(settingsTitle.toInt())) {
                val sections = paramOfType(Type.List)
                sections.assign(call(SettingsRows.withSection, sections, call(SettingsRows.section)))
            }
        }
    }
}

private val settingsListState = klass("settingsListState") { strings("SettingsListState(onBackClicked=") }

// A title given as a string resource.
private val resourceText = klass("resourceText") { strings("Resource(id=", ", formatArgs=") }
private val resourceTextId = resourceText.method("toString")
    .point { string("Resource(id=") }
    .next { opcode(Opcode.IGET) }
    .field("resourceTextId")

// The Additional resources page builds plain rows around stable URLs; its constructors give the model shapes.
val additionalResourcesRows = method("additionalResourcesRows") {
    strings("https://x.com/privacy", "https://business.x.com/help/troubleshooting/how-twitter-ads-work.html")
}
val settingsItemCtor = additionalResourcesRows.point { invokeDirect { name("<init>"); hasParam(FUNCTION0) } }.callee("settingsItemCtor")
val settingsSectionCtor = additionalResourcesRows.point { invokeDirect { name("<init>"); hasParam(Type.List); paramCount(4) } }.callee("settingsSectionCtor")
// The literal variant of the text type settings rows take.
val literalTextCtor = klass("literalText") {
    strings("Literal(text=")
    extends(settingsItemCtor.parameterTypes[0])
}.method("<init>") { params(Type.String) }

private object Resources

val unitInstance = fieldTarget("unitInstance") {
    val unit = bytecode.findClass("kotlin.Unit") ?: error("kotlin.Unit missing")
    unit.staticFields.single { it.fieldType == descriptor("kotlin.Unit") }.ref
}
