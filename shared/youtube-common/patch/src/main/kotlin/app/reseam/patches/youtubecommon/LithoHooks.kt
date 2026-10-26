// SPDX-FileCopyrightText: 2026 Cossale <hello@auna.li>
// SPDX-FileCopyrightText: 2026 ReVanced contributors
// SPDX-License-Identifier: GPL-3.0-or-later

package app.reseam.patches.youtubecommon

import app.reseam.patch.PatchRuntime
import app.reseam.patch.Type
import app.reseam.patch.after
import app.reseam.patch.before
import app.reseam.patch.method
import app.reseam.patch.point
import app.reseam.patch.reserveLocal
import app.reseam.youtube.litho.bindings.LithoFilter

internal fun PatchRuntime.installLithoHooks() {
    // Observe both providers without changing their model representation. Some components
    // (notably expandable community posts) serialize UPB models back to protobuf; the
    // FlatBuffer implementation of the same interface cannot perform that operation.
    // YouTube Music has dropped the FlatBuffer one.
    flatbufferElementParsers.all.forEach { it.before { LithoFilter.setProtoBuffer(param(0)) } }
    upbElementParser.before { LithoFilter.setProtoBuffer(param(0)) }

    // A tree's components take its root's own bytes. Templates are materialized into new
    // elements natively, so a parsed buffer cannot be traced to the tree built from it, and the
    // materialized root does not carry its identifier; the conversion context does.
    val outerTree = elementTreeBuild.reserveLocal("outerTree", Type.Object)
    elementTreeBuild.before {
        local(outerTree).assign(LithoFilter.enterTree(param(2), param(1).field(conversionContextIdentifier)))
    }
    elementTreeBuild.after { LithoFilter.exitTree(local(outerTree)) }
    LithoFilter.encodeElement.implement {
        whenInstanceOf(param(0), upbElementEncode.owner) {
            returnValue(param(0).cast(upbElementEncode.owner).call(upbElementEncode))
        }
        returnNull()
    }

    lithoLayoutExecutor.before {
        param(0).assign(LithoFilter.layoutThreadCount(param(0)))
        param(1).assign(LithoFilter.layoutThreadCount(param(1)))
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
        val filtered = LithoFilter.isFiltered(param(1).field(conversionContextIdentifier), local(accessibilityId), local(accessibilityText), param(1).field(conversionContextPath), param(2))
        whenTrue(filtered) {
            returnValue(call(emptyComponentBuilder, param(0)).field(emptyComponentField))
        }
    }
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
