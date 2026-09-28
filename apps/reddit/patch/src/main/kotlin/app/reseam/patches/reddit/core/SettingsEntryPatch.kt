// SPDX-FileCopyrightText: 2026 AunAli K. <hello@auna.li>
// SPDX-License-Identifier: GPL-3.0-or-later

package app.reseam.patches.reddit.core

import app.reseam.patch.MethodTarget
import app.reseam.patch.Type
import app.reseam.patch.before
import app.reseam.patch.descriptor
import app.reseam.patch.dex.AccessFlags
import app.reseam.patch.dex.DexClass
import app.reseam.patch.dex.Method
import app.reseam.patch.dex.buildInstructions
import app.reseam.patch.dex.isSet
import app.reseam.patch.dex.parseParameterTypes
import app.reseam.patch.dex.ref
import app.reseam.patch.dex.registerWordCount
import app.reseam.patch.fieldTarget
import app.reseam.patch.klass
import app.reseam.patch.method
import app.reseam.patch.methodTarget
import app.reseam.patch.native.NewMethod
import app.reseam.patch.patch
import app.reseam.patch.point
import app.reseam.patch.replace

private const val FUNCTION0 = "kotlin.jvm.functions.Function0"
private const val SETTINGS_SECTION = "com.reddit.settings.usersettings.UserSettingsSection"
private const val CONTRIBUTION_EXCEPTION = "com.reddit.settings.usersettings.UserSettingsContributionException"

private const val LOGO_PATH = "res/drawable/reseam_logo.png"

// Account settings has order 0 in General.
private const val GENERAL_ORDER = 1

// Defaulted row parameters, as Compose's synthetic defaults mask reads them: modifier, enabled,
// subtitle, trailing slot, colors, and the trailing flag. The leading icon slot is passed.
private const val ROW_DEFAULTS = 0b111011100

// Defaulted icon parameters: modifier, tint and the flag, so the icon takes the row's theme tint.
private const val ICON_DEFAULTS = 0b11100

// Reddit's icon size is two packed 32-bit dp values; settings rows use 20 by 20.
private const val ICON_SIZE = 0x14_00000014L

val settingsEntry = patch("Reseam entry in Reddit settings") {
    description("Adds a Reseam Settings row under Account settings in Reddit's settings.")
    compatibleWith(REDDIT)
    dependsOn(redditSettings)

    execute {
        val logo = Resources::class.java.getResourceAsStream("/reseam-logo.png")?.use { it.readBytes() }
            ?: error("reseam-logo.png missing from patch jar resources")
        val logoId = resources.addFile("drawable", "reseam_logo", LOGO_PATH, logo)

        // R8 renames kotlin.Unit.INSTANCE, so the Function0 body is emitted here.
        OpenReseamSettings.invoke.implement {
            call(RedditSettingsEntry.open)
            returnValue(staticField(unitInstance))
        }
        ReseamSettingsIcon.unit.implement { returnValue(staticField(unitInstance)) }
        // Inserted rather than replaced: the icon call needs more than a replaced body's 16 locals.
        ReseamSettingsIcon.draw.target.before {
            call(
                icon,
                newInstance(icon.parameterTypes[0], "(JIZ)V", long(ICON_SIZE), int(logoId.toInt()), bool(false)),
                string("Reseam"), nullObject, long(0), bool(false),
                param(0).cast(icon.parameterTypes[5]), int(0), int(ICON_DEFAULTS),
            )
        }

        val item = ReseamSettingsItem.target.classDef
        val itemInterface = bytecode.findClass(settingsItemKey.owner) ?: error("settings item interface missing")
        item.addInterface(itemInterface.descriptor)
        itemInterface.virtualMethods.filter { AccessFlags.ABSTRACT.isSet(it.info.accessFlags) }.forEach { member ->
            val bridge = item.addBridge(member)
            when (member.returnType) {
                descriptor("java.util.Set") -> bridge.replace { returnValue(call(ReseamSettingsItem.sessions)) }
                descriptor(SETTINGS_SECTION) -> bridge.replace { returnValue(enumValue(SETTINGS_SECTION, "General")) }
                Type.String -> bridge.replace { returnValue(string("key_pref_reseam")) }
                Type.Int -> bridge.replace { returnValue(int(GENERAL_ORDER)) }
                // Emitted before the bridge's return: a replaced body is capped at 16 locals, and the
                // 12-argument row call needs its values plus a range copy of them.
                Type.Void -> bridge.before {
                    call(
                        settingsRow,
                        string("Reseam Settings"), newInstance(OpenReseamSettings.descriptor),
                        nullObject, bool(false), nullObject, newInstance(ReseamSettingsIcon.descriptor), nullObject, nullObject, bool(false),
                        param(1), int(0), int(ROW_DEFAULTS),
                    )
                }
                else -> error("unknown settings item member ${member.name}${member.proto}")
            }
        }

        settingsItemRegistry.before {
            param(0).assign(call(ReseamSettingsItem.withItem, param(0)))
        }
    }
}

// Collects every contributed settings item and rejects duplicate keys; the view model reads its item set.
val settingsItemRegistry = method("settingsItemRegistry") {
    name("<init>")
    calls { owner(CONTRIBUTION_EXCEPTION); name("<init>") }
}
val settingsItemKey = settingsItemRegistry.point { invokeInterface { returns(Type.String); paramCount(0) } }.callee("settingsItemKey")

// The Privacy Policy item renders a plain row: title, click, then defaulted parameters.
val privacyPolicyItemContent = method("privacyPolicyItemContent") {
    inClass(klass("privacyPolicyItem") { strings("key_pref_privacy_policy") })
    returns(Type.Void)
    paramCount(3)
    param(2, Type.Int)
}
val settingsRow = privacyPolicyItemContent.point { invokeStatic { hasParam(FUNCTION0); hasParam(Type.String) } }.callee("settingsRow")

val unitInstance = fieldTarget("unitInstance") {
    val unit = bytecode.findClass("kotlin.Unit") ?: error("kotlin.Unit missing")
    unit.staticFields.single { it.fieldType == descriptor("kotlin.Unit") }.ref
}

/** An empty `return-void` method of [member]'s signature on this class, for a DSL body to fill. */
private fun DexClass.addBridge(member: Method): MethodTarget {
    val ins = 1 + parseParameterTypes(member.proto).sumOf(::registerWordCount)
    addMethod(NewMethod(
        name = member.name,
        proto = member.proto,
        accessFlags = (AccessFlags.PUBLIC or AccessFlags.FINAL).toUInt(),
        registersSize = ins.toUShort(),
        insSize = ins.toUShort(),
        outsSize = 0u,
        instructions = buildInstructions { returnVoid() },
        tries = emptyList(),
        catchHandlers = emptyList(),
    ))
    return methodTarget("${member.name}${member.proto} of $descriptor") { this@addBridge.method(member.name, member.proto) ?: error("added method missing") }
}

// Reddit's icon composable, as the post overflow menu calls it: icon, content description,
// modifier, tint, a flag, then the composer and its two Compose ints.
val icon = method("postOverflowItem") { strings("post_overflow_item_icon", "post_overflow_item_label") }
    .point { invokeStatic { returns(Type.Void); paramCount(8); hasParam(Type.String); hasParam(Type.Long) } }
    .callee("icon")

private object Resources
