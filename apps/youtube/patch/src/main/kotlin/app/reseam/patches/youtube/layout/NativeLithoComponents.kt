// SPDX-FileCopyrightText: 2026 Reseam contributors <https://reseam.app>
// SPDX-License-Identifier: AGPL-3.0-or-later

package app.reseam.patches.youtube.layout

import app.reseam.patch.*
import app.reseam.patch.dex.Opcode
import app.reseam.patch.dex.fieldRef
import app.reseam.patch.dex.opcode
import app.reseam.patch.dex.ref
import app.reseam.patches.youtubecommon.*

internal const val CHAR_SEQUENCE = "java.lang.CharSequence"

internal val nativeGesture = klass("native gesture component") { strings("ElementEventWithGesture") }
internal val nativeColumn = klass("native Litho column") {
    strings("Column")
    extends(componentCreate.returnType)
}

internal val nativeText = klass("native Litho text") {
    strings("Text")
    extends(componentCreate.returnType)
}

internal val nativeTextAccessibility = method("native text accessibility") {
    inClass(nativeText)
    calls { owner(Type.View); name("getImportantForAccessibility") }
}

internal val nativeTextValue = nativeTextAccessibility.point {
    opcode(Opcode.IGET_OBJECT)
    field { type(CHAR_SEQUENCE) }
}.field()
private val nativeCommonPropertiesField = fieldTarget("native component common properties") {
    klass(componentCreate.returnType).classDef.instanceFields.single { candidate ->
        bytecode.findClass(candidate.fieldType)?.instanceFields?.any {
            it.fieldType == "Landroid/graphics/drawable/Drawable;"
        } == true
    }.ref
}

internal val nativeCommonProperties = method("native component properties getter") {
    inClass(klass(componentCreate.returnType))
    params()
    returns(nativeCommonPropertiesField.type)
}

internal val nativeWrapInView = method("native component accessibility host") {
    inClass(klass(nativeCommonPropertiesField.type))
    params()
    returns(Type.Void)
    opcode(Opcode.IPUT_BOOLEAN)
}

// Follow Yoga's retained platform bridge back to the column's align-items property.
private val yogaLayout = method("native Litho Yoga layout") {
    calls { owner("com.facebook.yoga.YogaNative"); name("jni_YGNodeStyleSetAlignItemsJNI") }
    calls { owner("com.facebook.yoga.YogaNative"); name("jni_YGNodeStyleSetFlexDirectionJNI") }
}

private val yogaAlignItems = yogaLayout.point {
    invokeStatic { owner("com.facebook.yoga.YogaNative"); name("jni_YGNodeStyleSetAlignItemsJNI") }
}.previous { opcode(Opcode.IGET); field { type(Type.Int) } }.field()
internal val nativeColumnAlignItems = method("native column layout") {
    inClass(nativeColumn)
    returns(yogaAlignItems.ref.definingClass)
}.point {
    opcode(Opcode.IPUT)
    field { owner(yogaAlignItems.ref.definingClass); name(yogaAlignItems.ref.name) }
}.previous { opcode(Opcode.IGET); field { owner(nativeColumn.descriptor); type(Type.Int) } }.field()
private val yogaMinHeight = method("Yoga minimum height adapter") {
    params(Type.Int)
    calls { owner("com.facebook.yoga.YogaNative"); name("jni_YGNodeStyleSetMinHeightJNI") }
}

private val yogaHeightBridge = method("Yoga height bridge") {
    inClass(klass("com.facebook.yoga.YogaNodeJNIBase"))
    calls { owner("com.facebook.yoga.YogaNative"); name("jni_YGNodeStyleSetHeightJNI") }
}

private val yogaHeight = method("Yoga height adapter") {
    inClass(klass(yogaMinHeight.owner))
    params(Type.Int)
    calls { name(yogaHeightBridge.name); params(*yogaHeightBridge.parameterTypes.toTypedArray()) }
}

internal val nativeHeight = yogaLayout.point {
    invokeInterface { name(yogaHeight.name); params(Type.Int); returns(Type.Void) }
}.writer(1).field()
internal val nativeMinHeight = yogaLayout.point {
    invokeInterface { name(yogaMinHeight.name); params(Type.Int); returns(Type.Void) }
}.writer(1).field()
private val nativeBuilder = klass("native component builder") { strings("unknown component") }
private fun sizeSetter(size: FieldTarget) = method("native size setter") {
    inClass(nativeBuilder)
    params(Type.Int)
    returns(Type.Void)
    custom { instructions.any { it.opcode == Opcode.IPUT && it.fieldRef == size.ref } }
}

internal val nativeHeightSetter = sizeSetter(nativeHeight)
internal val nativeMinHeightSetter = sizeSetter(nativeMinHeight)
internal val nativeLayoutFlags = nativeHeightSetter.point { opcode(Opcode.IGET) }.field()
internal val nativeLayoutProperties = nativeHeightSetter.point {
    invokeVirtual { owner(nativeCommonPropertiesField.type) }
}.callee()
