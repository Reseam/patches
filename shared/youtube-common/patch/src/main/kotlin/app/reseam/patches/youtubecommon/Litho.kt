// SPDX-FileCopyrightText: 2026 Cossale <hello@auna.li>
// SPDX-FileCopyrightText: 2026 ReVanced contributors
// SPDX-License-Identifier: GPL-3.0-or-later

package app.reseam.patches.youtubecommon

import app.reseam.patch.ExtClass
import app.reseam.patch.Type
import app.reseam.patch.appEntry
import app.reseam.patch.classTarget
import app.reseam.patch.dex.AccessFlags
import app.reseam.patch.dex.Opcode
import app.reseam.patch.dex.ref
import app.reseam.patch.fieldOfType
import app.reseam.patch.fieldTarget
import app.reseam.patch.klass
import app.reseam.patch.method
import app.reseam.patch.methods
import app.reseam.patch.patch
import app.reseam.patch.point
import app.reseam.youtube.litho.bindings.LithoFilter

/**
 * Hands every litho component to the extension as it is built, and swaps in an empty component
 * when a filter says to hide it. Every "hide" patch routes through here.
 */
// Not pinned to an app: YouTube Music builds its UI with the same library. It runs only as a
// dependency, so the dependent's own settings host is already in place.
val lithoFilter = patch {
    execute {
        installLithoHooks()
        keepElementsOnLitho()
    }
}

/**
 * Registers [filter], an extension class extending `app.reseam.youtube.litho.Filter`, when the
 * process starts. Call from `execute` of a patch that depends on [lithoFilter]; only the filters of
 * selected patches are ever searched.
 */
fun registerLithoFilter(filter: ExtClass) {
    appEntry { LithoFilter.register(newInstance(filter.descriptor)) }
}

// The one method that turns a parsed element into a litho component; it throws both of these.
val componentCreate = method("componentCreate") {
    strings("Element missing correct type extension", "Element missing type")
}

val componentCreateReturn = componentCreate.point { opcode(Opcode.RETURN_OBJECT) }

// Builds the whole tree under a root element into components, synchronously. Its trace section is
// labelled with the root's identifier.
val elementTreeBuild = method("elementTreeBuild") {
    strings("Elements.toComponent:eml=")
    returns(componentCreate.returnType)
}

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

// Elements backed by a native message serialize themselves through it; the one encoder is theirs.
val upbElementEncode = method("upbElementEncode") {
    calls(upbMessage.method("jniEncode"))
    params()
    returns("[B")
}

val protobufBufferSetter = method("protobufBufferSetter") {
    calls(upbMessageDecode)
    params("[B")
    returns(Type.Void)
    flags(AccessFlags.PUBLIC or AccessFlags.FINAL)
}

val flatbufferElementParsers = methods("flatbufferElementParsers") {
    strings("PbToFb failed: ")
    params("[B", Type.Boolean)
}

val upbElementParser = method("upbElementParser") {
    calls(protobufBufferSetter)
    params("[B", Type.Boolean)
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
