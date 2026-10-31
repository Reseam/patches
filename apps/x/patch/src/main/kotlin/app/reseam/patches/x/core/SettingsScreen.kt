// SPDX-FileCopyrightText: 2026 Cossale <hello@auna.li>
// SPDX-License-Identifier: AGPL-3.0-or-later

package app.reseam.patches.x.core

import app.reseam.patch.CodeScope
import app.reseam.patch.MethodQuery
import app.reseam.patch.MethodsTarget
import app.reseam.patch.PatchRuntime
import app.reseam.patch.Type
import app.reseam.patch.ValueRef
import app.reseam.patch.before
import app.reseam.patch.descriptor
import app.reseam.patch.dex.ref
import app.reseam.patch.fieldTarget
import app.reseam.patch.klass
import app.reseam.patch.method
import app.reseam.patch.methods
import app.reseam.patch.point
import app.reseam.x.settings.bindings.SettingsRows

const val FUNCTION0 = "kotlin.jvm.functions.Function0"

private const val LOGO_PATH = "res/drawable/reseam_logo.png"
private const val SECTION_DEFAULTS_HEADER_AND_SUBTITLE = 0b11

fun PatchRuntime.reseamLogoId(): Int {
    resources.id("drawable", "reseam_logo")?.let { return it.toInt() }
    val logo = Resources::class.java.getResourceAsStream("/reseam-logo.png")?.use { it.readBytes() }
        ?: error("reseam-logo.png missing from patch jar resources")
    return resources.addFile("drawable", "reseam_logo", LOGO_PATH, logo).toInt()
}

/** [section] is built per settings design; [tag] keeps it from being added again when the screen copies its state. */
fun PatchRuntime.addSettingsSection(tag: String, section: CodeScope.(SettingsDesign) -> ValueRef) {
    val settingsTitle = resources.id("string", "settings") ?: error("string/settings missing")
    for (design in settingsDesigns()) {
        val rootSettings = method("rootSettings:$tag:${design.packageName}") {
            literals(settingsTitle.toLong())
            calls(design.stateCtor)
        }
        // Anchor on the state constructor call itself, so another row patch inserting before it
        // (both add a section at this one point) does not shift this one out of a fixed window.
        val sectionsArgument = design.stateCtor.parameterTypes.indexOf(Type.List) + 1
        val captured = "sections:$tag"
        rootSettings.point { calls(design.stateCtor) }
            .captureArgumentAs(captured, sectionsArgument, Type.List)
            .before {
                val sections = capture(captured)
                sections.assign(SettingsRows.withSection(sections, string(tag), section(design)))
            }
    }
}

fun CodeScope.settingsItem(design: SettingsDesign, title: String, subtitle: String, iconId: Int, onClick: String): ValueRef {
    val text = design.literalTextCtor
    val item = design.itemCtor
    // R8 drops constructor parameters without renumbering the default bits, so pass every default.
    val arguments = listOf(
        newInstance(text.owner, text.proto, string(title)),
        newInstance(text.owner, text.proto, string(subtitle)),
        newInstance(item.parameterTypes[2], "(I)V", int(iconId)),
    ) + item.parameterTypes.drop(3).map { type ->
        when (type) {
            descriptor(FUNCTION0) -> newInstance(onClick)
            Type.Boolean -> bool(false)
            Type.Int -> int(0)
            else -> nullObject
        }
    }
    return newInstance(item.owner, item.proto, *arguments.toTypedArray())
}

fun CodeScope.settingsSection(design: SettingsDesign, items: ValueRef): ValueRef {
    val section = design.sectionCtor
    return newInstance(
        section.owner, section.proto,
        nullObject, nullObject, items, int(SECTION_DEFAULTS_HEADER_AND_SUBTITLE),
    )
}

class SettingsDesign(val packageName: String) {
    val stateCtor = model(settingsListStates).method("<init>")
    val sectionCtor = model(settingsListSections).method("<init>") { hasParam(Type.List); defaultsMask() }
    val itemCtor = model(settingsListItems).method("<init>") { hasParam(FUNCTION0); defaultsMask() }

    val literalTextCtor = klass("literalText:$packageName") {
        strings("Literal(text=")
        extends(itemCtor.parameterTypes[0])
    }.method("<init>") { params(Type.String) }

    private fun model(toStrings: MethodsTarget) =
        klass(toStrings.all.single { it.owner.substringBeforeLast('/') == packageName }.owner)
}

private fun MethodQuery.defaultsMask() = custom { parameterTypes.last() == Type.Int }

private fun settingsDesigns() =
    settingsListStates.all.map { SettingsDesign(it.owner.substringBeforeLast('/')) }

private val settingsListStates = methods("settingsListState.toString") {
    name("toString")
    strings("SettingsListState(onBackClicked=")
}
private val settingsListSections = methods("settingsListSection.toString") {
    name("toString")
    strings("SettingsListSection(title=")
}
private val settingsListItems = methods("settingsListItem.toString") {
    name("toString")
    strings("SettingsListItem(title=")
}

private object Resources

val unitInstance = fieldTarget("unitInstance") {
    val unit = bytecode.findClass("kotlin.Unit") ?: error("kotlin.Unit missing")
    unit.staticFields.single { it.fieldType == descriptor("kotlin.Unit") }.ref
}
