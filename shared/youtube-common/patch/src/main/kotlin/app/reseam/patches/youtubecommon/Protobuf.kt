// SPDX-FileCopyrightText: 2026 Reseam contributors <https://reseam.app>
// SPDX-License-Identifier: AGPL-3.0-or-later

package app.reseam.patches.youtubecommon

import app.reseam.patch.ClassTarget
import app.reseam.patch.CodeScope
import app.reseam.patch.FieldTarget
import app.reseam.patch.MethodTarget
import app.reseam.patch.Type
import app.reseam.patch.ValueRef
import app.reseam.patch.classTarget
import app.reseam.patch.dex.AccessFlags
import app.reseam.patch.dex.Opcode
import app.reseam.patch.klass
import app.reseam.patch.method
import app.reseam.patch.point

private const val MESSAGE_LITE = "com.google.protobuf.MessageLite"
private const val EXTENSION_REGISTRY = "com.google.protobuf.ExtensionRegistryLite"

fun protobufDefaultInstance(message: ClassTarget, name: String): FieldTarget = message
    .method("<clinit>") {
        params()
        flags(AccessFlags.STATIC)
    }
    .point("$name default instance") {
        opcode(Opcode.SPUT_OBJECT)
        field { type(message.descriptor) }
    }
    .field("$name default instance field")

fun protobufParser(message: ClassTarget, name: String): MethodTarget = method("$name parser") {
    inClass(message, inherited = true)
    name("parseFrom")
    paramCount(3)
    param(1, "[B")
    param(2, EXTENSION_REGISTRY)
}

fun CodeScope.protobufBytes(value: ValueRef): ValueRef =
    value.callInterface(MESSAGE_LITE, "toByteArray", "()[B")

fun CodeScope.parseProtobuf(message: ClassTarget, name: String, bytes: ValueRef): ValueRef = call(
    protobufParser(message, name),
    staticField(protobufDefaultInstance(message, name)),
    bytes,
    callStatic(EXTENSION_REGISTRY, "getGeneratedRegistry", "()L${EXTENSION_REGISTRY.replace('.', '/')};"),
).cast(message.descriptor)

private val messageByteString = method("protobuf message byte string") {
    inClass(klass(MESSAGE_LITE))
    name("toByteString")
    params()
}

private val byteString = classTarget("protobuf byte string") {
    bytecode.findClass(messageByteString.returnType) ?: error("The protobuf byte string class is missing")
}

private val byteStringCopy = method("protobuf byte string copy") {
    inClass(byteString)
    flags(AccessFlags.STATIC)
    params("[B")
    returns(byteString.descriptor)
    calls { params("[B", Type.Int, Type.Int) }
}

private val byteStringBytes = method("protobuf byte string bytes") {
    inClass(byteString)
    params()
    returns("[B")
}

fun CodeScope.protobufByteString(bytes: ValueRef): ValueRef = call(byteStringCopy, bytes)

fun CodeScope.protobufByteStringBytes(value: ValueRef): ValueRef = value.cast(byteString.descriptor).call(byteStringBytes)
