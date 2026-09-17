// SPDX-FileCopyrightText: 2026 AunAli K. <hello@auna.li>
// SPDX-FileCopyrightText: 2026 ReVanced contributors
// SPDX-License-Identifier: GPL-3.0-or-later

package app.reseam.patches.youtube.internal

import app.reseam.patch.ExtClass
import app.reseam.patch.ExtMethod
import app.reseam.patch.MethodTarget
import app.reseam.patch.ResourceScope
import app.reseam.patch.Type
import app.reseam.patch.XmlElement
import app.reseam.patch.after
import app.reseam.patch.before
import app.reseam.patch.classTarget
import app.reseam.patch.dex.AccessFlags
import app.reseam.patch.dex.Opcode
import app.reseam.patch.dex.parameterTypes
import app.reseam.patch.dex.returnType
import app.reseam.patch.method
import app.reseam.patch.patch
import app.reseam.patch.point
import app.reseam.patch.XmlDocument
import app.reseam.patch.resourceRef
import app.reseam.patches.youtube.core.YOUTUBE

private const val AUTO_NS = "http://schemas.android.com/apk/res-auto"
private const val CONSTRAINT_LAYOUT = "androidx.constraintlayout.widget.ConstraintLayout"

private fun ResourceScope.id(name: String): Long = id("id", name)?.toLong()
    ?: error("id/$name is missing")

private class BottomLayout(val resourcePath: String, val idName: String, val position: Int)

private val bottomLayouts = mutableListOf<BottomLayout>()
private val bottomInitializers = mutableListOf<ExtMethod>()
private var bottomControlsInflateTarget: MethodTarget? = null

/**
 * Registers an XML fragment and its ids before the shared seam grafts all controls.
 * [position] orders the buttons outward from the fullscreen button, lowest nearest.
 */
fun registerPlayerControlLayout(resourcePath: String, idName: String, position: Int) {
    bottomLayouts += BottomLayout(resourcePath, idName, position)
}

/** Registers the extension initializer corresponding to a previously registered XML fragment. */
fun registerPlayerControlInitializer(initializer: ExtMethod) {
    bottomInitializers += initializer
}

object PlayerControls : ExtClass("app.reseam.youtube.controls.PlayerControls") {
    val setVisibility = static("setVisibility", Type.Boolean, Type.Boolean)
    val setVisibilityImmediate = static("setVisibilityImmediate", Type.Boolean)
    val setFullscreenCloseButton = static("setFullscreenCloseButton", Type.View)
    val setPlayerControlsVisibility = static("setPlayerControlsVisibility", "java.lang.Enum")
}

private val visibilityEntityMethod = method("player controls visibility entity") {
    name("getPlayerControlsVisibility")
    flags(AccessFlags.PUBLIC)
    params()
    custom { returnType.startsWith("L") }
    opcodeSequence(Opcode.IGET, Opcode.INVOKE_STATIC)
}

private val visibilityEntityClass = classTarget("player controls visibility entity class") {
    bytecode.findClass(visibilityEntityMethod.owner) ?: error("player controls visibility class is missing")
}

private val visibilityEntityConstructor = method("player controls visibility constructor") {
    inClass(visibilityEntityClass)
    flags(AccessFlags.CONSTRUCTOR)
    returns(Type.Void)
    paramCount(1)
    custom { parameterTypes.singleOrNull()?.startsWith("L") == true }
}

private val visibilityStateField = visibilityEntityMethod
    .point("player controls visibility state field") { opcode(Opcode.IGET) }
    .field()

private val visibilityFactory = visibilityEntityMethod
    .point("player controls visibility factory") { invokeStatic { } }
    .callee()

val playerControls = patch {
    compatibleWith(YOUTUBE)

    execute {
        // Resource-seeded targets are intentionally created inside execute so the ids come from
        // this YouTube APK rather than from a copied resource table.
        val bottomStub = resources.id("bottom_ui_container_stub")
        val heatseeker = resources.id("heatseeker_viewstub")
        val fullscreen = resources.id("fullscreen_button")
        val bottomControlsInflate = method("bottom player controls inflate") {
            params()
            returns(Type.Object)
            literals(bottomStub)
            calls { params(); returns(Type.View) }
        }
        bottomControlsInflateTarget = bottomControlsInflate
        val overlayInflate = method("player controls overlay inflate") {
            flags(AccessFlags.PUBLIC or AccessFlags.FINAL)
            hasParam(Type.View)
            returns(Type.Void)
            literals(heatseeker, fullscreen)
            opcode(Opcode.CHECK_CAST)
        }
        val controlsOwnerMethod = bottomControlsInflate
            .point("player controls owner method") { invokeVirtual { params(); returns(Type.Void) } }
            .callee()
        val controlsOverlay = method("player controls overlay visibility") {
            inClass(classTarget("player controls owner") {
                bytecode.findClass(controlsOwnerMethod.owner) ?: error("player controls owner is missing")
            })
            flags(AccessFlags.PRIVATE or AccessFlags.FINAL)
            params(Type.Boolean, Type.Boolean)
            returns(Type.Void)
        }
        overlayInflate
            .point("fullscreen button id") { literal(fullscreen) }
            .next { checkCast("android.widget.ImageView") }
            .captureAs("fullscreenButton", Type.View)
            .after { call(PlayerControls.setFullscreenCloseButton, capture("fullscreenButton")) }

        controlsOverlay.before { call(PlayerControls.setVisibility, param(0), param(1)) }
        visibilityEntityConstructor
            .point("resolved player controls visibility") { opcode(Opcode.IPUT_OBJECT) }
            .captureAs("visibilityContainer", visibilityEntityConstructor.parameterTypes.single())
            .after {
                call(
                    PlayerControls.setPlayerControlsVisibility,
                    call(visibilityFactory, capture("visibilityContainer").field(visibilityStateField)),
                )
            }

        listOf(
            45643739L,
            45709810L,
            45713296L,
        ).forEach { flag ->
            val reads = booleanFeatureReads(flag)
            reads.forEach { site ->
                site.next { resultOf(Type.Boolean) }.captureAs("flag").after {
                    capture("flag").assign(bool(false))
                }
            }
        }
    }

    afterDependents {
        val initializers = bottomInitializers.toList()
        val layouts = bottomLayouts.sortedBy { it.position }
        val inflateTarget = bottomControlsInflateTarget
        bottomInitializers.clear()
        bottomLayouts.clear()
        bottomControlsInflateTarget = null
        if (initializers.isNotEmpty()) {
            val inflate = inflateTarget
                ?: error("bottom player controls inflate target is missing")
            inflate
                .point("bottom controls inflate result") {
                    invoke { params(); returns(Type.View) }
                }
                .next { opcode(Opcode.MOVE_RESULT_OBJECT) }
                .captureAs("bottomControls", Type.View)
                .after {
                    initializers.forEach { call(it, capture("bottomControls")) }
                }
        }

        if (layouts.isEmpty()) return@afterDependents
        val fullscreenId = resources.id("fullscreen_button")
        val fullscreenStubId = resources.id("youtube_controls_fullscreen_button_stub")
        // Placed before these, the buttons would overlap them; they move left of the last button.
        val followingIds = setOf(resources.id("bottom_end_container"), resources.id("multiview_button"))
        resources.editXml("layout", "youtube_controls_bottom_ui_container") {
            declareNamespace("yt", AUTO_NS)
            val parent = findByTag(CONSTRAINT_LAYOUT).singleOrNull()
                ?: error("youtube_controls_bottom_ui_container has no constraint parent")
            fun idOf(element: XmlElement, attr: String) = resourceRef(element[attr].orEmpty())?.toLong()
            val anchor = parent.children.firstOrNull { idOf(it, "android:inflatedId") == fullscreenId }
                ?: error("fullscreen_button is missing from youtube_controls_bottom_ui_container")
            // The stub sits lower than the added buttons, so it gets their width and bottom margin.
            val stub = parent.children.firstOrNull { idOf(it, "android:id") == fullscreenStubId }
                ?: error("youtube_controls_fullscreen_button_stub is missing")
            stub["android:layout_marginBottom"] = "6.0dip"
            stub["android:layout_width"] = "48.0dip"

            var last = "@id/fullscreen_button"
            var insertBefore = anchor
            for (layout in layouts) {
                val bytes = PlayerControls::class.java.getResourceAsStream(layout.resourcePath)?.use { it.readBytes() }
                    ?: error("player-control layout fragment ${layout.resourcePath} is missing")
                XmlDocument.compile(bytes.toString(Charsets.UTF_8)).use { source ->
                    val child = adopt(source.root)
                    child["yt:layout_constraintRight_toLeftOf"] = last
                    insertBefore = parent.insertBefore(child, insertBefore)
                    last = "@id/${layout.idName}"
                }
            }
            parent.children
                .filter { idOf(it, "android:id") in followingIds }
                .forEach { it["yt:layout_constraintRight_toLeftOf"] = last }
        }
    }
}
