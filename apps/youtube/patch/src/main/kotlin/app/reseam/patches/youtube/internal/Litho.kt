// SPDX-FileCopyrightText: 2026 AunAli K. <hello@auna.li>
// SPDX-FileCopyrightText: 2026 ReVanced contributors
// SPDX-License-Identifier: GPL-3.0-or-later

package app.reseam.patches.youtube.internal

import app.reseam.patch.ExtClass
import app.reseam.patch.Type
import app.reseam.patch.appEntry
import app.reseam.patch.before
import app.reseam.patch.classTarget
import app.reseam.patch.dex.AccessFlags
import app.reseam.patch.dex.Opcode
import app.reseam.patch.dex.fieldRef
import app.reseam.patch.dex.ref
import app.reseam.patch.fieldOfType
import app.reseam.patch.fieldTarget
import app.reseam.patch.klass
import app.reseam.patch.method
import app.reseam.patch.patch
import app.reseam.patch.point
import app.reseam.patch.reserveLocal
import app.reseam.patches.youtube.core.YOUTUBE
import app.reseam.patches.youtube.core.youTubeSettings

/**
 * Hands every litho component to the extension as it is built, and swaps in an empty component
 * when a filter says to hide it. Every "hide" patch routes through here.
 */
val lithoFilter = patch {
    compatibleWith(YOUTUBE)
    dependsOn(youTubeSettings)

    execute {
        // Observe both providers without changing their model representation. Some components
        // (notably expandable community posts) serialize UPB models back to protobuf; the
        // FlatBuffer implementation of the same interface cannot perform that operation.
        flatbufferElementParser.before { call(LithoFilter.setProtoBuffer, param(0)) }
        upbElementParser.before { call(LithoFilter.setProtoBuffer, param(0)) }

        lithoLayoutExecutor.before {
            param(0).assign(call(LithoFilter.layoutThreadCount, param(0)))
            param(1).assign(call(LithoFilter.layoutThreadCount, param(1)))
        }

        // Keep the optional accessibility properties in method locals: recursive component
        // creation must not overwrite a parent's values through shared extension state.
        val accessibilityId = componentCreate.reserveLocal("accessibilityId", Type.String)
        val accessibilityText = componentCreate.reserveLocal("accessibilityText", Type.String)
        componentCreate.point("componentAccessibility") {
            invokeStatic { owner(accessibilityProperties.owner); returns(Type.Void) }
        }.captureArgumentAs("accessibility", 0).before {
            local(accessibilityId).assign(capture("accessibility").call(accessibilityIdentifier))
            local(accessibilityText).assign(capture("accessibility").call(accessibilityDescription))
        }

        // Filtering happens at the return, after the component is fully built. Returning earlier
        // leaves the litho tree half-constructed, which costs memory and stalls layout.
        componentCreateReturn.before {
            val filtered = call(
                LithoFilter.isFiltered,
                param(1).field(conversionContextIdentifier),
                local(accessibilityId),
                local(accessibilityText),
                param(1).field(conversionContextPath),
            )
            whenTrue(filtered) {
                returnValue(call(emptyComponentBuilder, param(0)).field(emptyComponentField))
            }
        }
    }
}

/**
 * Registers [filter], an extension class extending `app.reseam.youtube.litho.Filter`, when the
 * process starts. Call from `execute` of a patch that depends on [lithoFilter]; only the filters of
 * selected patches are ever searched.
 */
fun registerLithoFilter(filter: ExtClass) {
    appEntry.before { call(LithoFilter.register, newInstance(filter.descriptor)) }
}

object LithoFilter : ExtClass("app.reseam.youtube.litho.LithoFilter") {
    val register = static("register", "app.reseam.youtube.litho.Filter")
    val setProtoBuffer = static("setProtoBuffer", "[B")
    val layoutThreadCount = static("layoutThreadCount", Type.Int, returns = Type.Int)
    val isFiltered = static(
        "isFiltered",
        Type.String,
        Type.String,
        Type.String,
        "java.lang.StringBuilder",
        returns = Type.Boolean,
    )
}

// The one method that turns a parsed element into a litho component; it throws both of these.
val componentCreate = method("componentCreate") {
    strings("Element missing correct type extension", "Element missing type")
}

val componentCreateReturn = componentCreate.point { opcode(Opcode.RETURN_OBJECT) }

// The conversion context carries the component's identifier and the path built up to it. Only its
// toString names the two fields, by labelling each value as it appends it.
val conversionContextToString = method("conversionContextToString") {
    strings(", identifierProperty=")
    name("toString")
    params()
    returns(Type.String)
}

val conversionContext = classTarget("conversionContext") {
    bytecode.findClass(conversionContextToString.owner) ?: error("The conversion context class is missing")
}

val conversionContextPath = conversionContext.fieldOfType("java.lang.StringBuilder")

private const val STRING_BUILDER = "java.lang.StringBuilder"

// toString appends the label and then the field it names. The compiler hoists every field read to
// the top of the method, so the field is found by walking back from the appended value to the
// instruction that wrote it.
val conversionContextIdentifier = conversionContextToString
    .point("identifierLabel") { string(", identifierProperty=") }
    .next { invokeVirtual { owner(STRING_BUILDER); name("append") } }
    .next { invokeVirtual { owner(STRING_BUILDER); name("append") } }
    .writer(1, "conversionContextIdentifier")
    .field()

// The component every hidden component is replaced with. It is the only class whose private
// no-argument constructor names itself "EmptyComponent".
val emptyComponentConstructor = method("emptyComponentConstructor") {
    strings("EmptyComponent")
    params()
    flags(AccessFlags.PRIVATE or AccessFlags.CONSTRUCTOR)
}

val emptyComponentClass = classTarget("emptyComponentClass") {
    bytecode.findClass(emptyComponentConstructor.owner) ?: error("The empty component class is missing")
}

// The class exists only to build that one component, so its single static method is the builder.
val emptyComponentBuilder = method("emptyComponentBuilder") {
    inClass(emptyComponentClass)
    flags(AccessFlags.STATIC)
}

val emptyComponentField = fieldTarget("emptyComponent") {
    val builder = bytecode.findClass(emptyComponentBuilder.returnType)
        ?: error("The empty component builder class is missing")
    builder.instanceFields.single { it.fieldType == emptyComponentConstructor.owner }.ref
}

// Native code decodes the component's protocol buffer; this is the one bridge back into Java.
val upbMessage = klass("com.google.android.libraries.elements.adl.UpbMessage")

val upbMessageDecode = upbMessage.method("jniDecode")

val protobufBufferSetter = method("protobufBufferSetter") {
    calls(upbMessageDecode)
    params("[B")
    returns(Type.Void)
    flags(AccessFlags.PUBLIC or AccessFlags.FINAL)
}

val flatbufferElementParser = method("flatbufferElementParser") {
    strings("PbToFb failed: ")
    params("[B", Type.Boolean)
}

val upbElementParser = method("upbElementParser") {
    calls(protobufBufferSetter)
    params("[B", Type.Boolean)
    returns(flatbufferElementParser.returnType)
}

// The layout pool is sized by core count and memory; its thread factory is what names it litho's.
val lithoThreadFactory = klass("lithoThreadFactory") {
    strings("ComponentLayoutThread")
    implements("java.util.concurrent.ThreadFactory")
}

val lithoThreadFactoryInit = lithoThreadFactory.method("<init>") { params(Type.Int) }

val lithoLayoutExecutor = method("lithoLayoutExecutor") {
    calls(lithoThreadFactoryInit)
    params(Type.Int, Type.Int, Type.Int)
    returns(Type.Void)
    flags(AccessFlags.PUBLIC or AccessFlags.CONSTRUCTOR)
}

// Accessibility roles are mapped to Android widgets here. The first two empty checks guard
// the identifier and spoken description; follow their producers instead of obfuscated names.
private val accessibilityProperties = method("accessibilityProperties") {
    strings("android.widget.ToggleButton", "android.widget.Spinner")
    returns(Type.Void)
}
private val accessibilityIdentifierCheck = accessibilityProperties.point {
    invokeStatic { owner("android.text.TextUtils"); name("isEmpty") }
}
private val accessibilityIdentifier = accessibilityIdentifierCheck.writer(0)
    .previous { invokeInterface { returns(Type.String) } }.callee("accessibilityIdentifier")
private val accessibilityDescription = accessibilityIdentifierCheck
    .next { invokeStatic { owner("android.text.TextUtils"); name("isEmpty") } }
    .writer(0).previous { invokeInterface { returns(Type.String) } }.callee("accessibilityDescription")
