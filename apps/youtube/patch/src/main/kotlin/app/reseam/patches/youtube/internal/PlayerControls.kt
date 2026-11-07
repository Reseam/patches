// SPDX-FileCopyrightText: 2026 Reseam contributors <https://reseam.app>
// SPDX-FileCopyrightText: 2026 ReVanced contributors
// SPDX-License-Identifier: GPL-3.0-or-later

package app.reseam.patches.youtube.internal

import app.reseam.patch.ExtMethod
import app.reseam.patch.ResourceScope
import app.reseam.patch.RunList
import app.reseam.patch.Type
import app.reseam.patch.XmlDocument
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
import app.reseam.patch.resourceRef
import app.reseam.patches.youtube.core.YOUTUBE
import app.reseam.youtube.controls.bindings.PlayerControls

private const val AUTO_NS = "http://schemas.android.com/apk/res-auto"
// Older releases inflate the support library's ConstraintLayout.
private val CONSTRAINT_LAYOUTS = listOf(
    "androidx.constraintlayout.widget.ConstraintLayout",
    "android.support.constraint.ConstraintLayout",
)

// Inflates youtube_video_exploder_controls_bottom_ui_container in place of youtube_controls_bottom_ui_container.
private const val BOLD_BOTTOM_CONTROLS = 45643739L
private const val BUTTON_STROKE = 45713296L
// 45686474 (before 21.36) puts a circle behind the fullscreen button, and 45724241 sizes it for the bold layout.
private val BOLD_PLAYER_BUTTONS = listOf(BOLD_BOTTOM_CONTROLS, 45709810L, BUTTON_STROKE, 45686474L, 45724241L)

// Releases before the button stroke experiment only have the thin player buttons.
private fun boldPlayerButtons() = optionalBooleanFeatureReads(BUTTON_STROKE).isNotEmpty()

private fun ResourceScope.id(name: String): Long = id("id", name)?.toLong()
    ?: error("id/$name is missing")

private fun bundledResource(path: String): ByteArray =
    PlayerControls::class.java.getResourceAsStream(path)?.use { it.readBytes() } ?: error("$path is missing")

private class BottomLayout(val thinResourcePath: String, val boldResourcePath: String, val idName: String, val position: Int)
private class ButtonIcon(val name: String, val thinResourcePath: String, val boldResourcePath: String)

private val bottomLayouts = RunList<BottomLayout>()
private val bottomInitializers = RunList<ExtMethod>()
private val buttonIcons = RunList<ButtonIcon>()

/** Adds drawable [name], drawn to match the release's player buttons. */
fun registerPlayerControlIcon(name: String, thinResourcePath: String, boldResourcePath: String) {
    buttonIcons.add(ButtonIcon(name, thinResourcePath, boldResourcePath))
}

/**
 * Registers an XML fragment, drawn to match the release's player buttons, before the shared seam grafts all controls.
 * [position] orders the buttons outward from the fullscreen button, lowest nearest.
 */
fun registerPlayerControlLayout(thinResourcePath: String, boldResourcePath: String, idName: String, position: Int) {
    bottomLayouts.add(BottomLayout(thinResourcePath, boldResourcePath, idName, position))
}

/** Registers the extension initializer corresponding to a previously registered XML fragment. */
fun registerPlayerControlInitializer(initializer: ExtMethod) {
    bottomInitializers.add(initializer)
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

private fun bottomControlsInflate(bottomStub: Long) = method("bottom player controls inflate") {
    params()
    returns(Type.Object)
    literals(bottomStub)
    calls { params(); returns(Type.View) }
}

val playerControls = patch {
    compatibleWith(YOUTUBE)

    execute {
        // Resource-seeded targets are intentionally created where they are used so the ids come
        // from this YouTube APK rather than from a copied resource table.
        val heatseeker = resources.id("heatseeker_viewstub")
        val fullscreen = resources.id("fullscreen_button")
        val bottomControlsInflate = bottomControlsInflate(resources.id("bottom_ui_container_stub"))
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
            .after { PlayerControls.setFullscreenCloseButton(capture("fullscreenButton")) }

        controlsOverlay.before { PlayerControls.setVisibility(param(0), param(1)) }
        visibilityEntityConstructor
            .point("resolved player controls visibility") { opcode(Opcode.IPUT_OBJECT) }
            .captureAs("visibilityContainer", visibilityEntityConstructor.parameterTypes.single())
            .after {
                PlayerControls.setPlayerControlsVisibility(call(visibilityFactory, capture("visibilityContainer").field(visibilityStateField)))
            }

        // YouTube enables the bold player buttons for every client, from a config a GmsCore install lacks until its
        // third launch. Fixing them keeps the bottom layout our buttons are added to the one YouTube inflates.
        if (boldPlayerButtons()) BOLD_PLAYER_BUTTONS.forEach(::enableBooleanFeature)
        else disableBooleanFeature(BOLD_BOTTOM_CONTROLS)
    }

    afterDependents {
        val initializers = bottomInitializers.values
        val layouts = bottomLayouts.values.sortedBy { it.position }
        if (initializers.isNotEmpty()) {
            bottomControlsInflate(resources.id("bottom_ui_container_stub"))
                .point("bottom controls inflate result") {
                    invoke { params(); returns(Type.View) }
                }
                .next { opcode(Opcode.MOVE_RESULT_OBJECT) }
                .captureAs("bottomControls", Type.View)
                .after {
                    initializers.forEach { call(it, capture("bottomControls")) }
                    PlayerControls.fitButtonRow(capture("bottomControls"))
                }
        }

        val bold = boldPlayerButtons()
        buttonIcons.values.forEach { icon ->
            resources.addFile("drawable", icon.name, "res/drawable/${icon.name}.xml",
                bundledResource(if (bold) icon.boldResourcePath else icon.thinResourcePath))
        }

        if (layouts.isEmpty()) return@afterDependents
        if (bold) {
            resources.addFile("drawable", "reseam_player_button_circle", "res/drawable/reseam_player_button_circle.xml",
                bundledResource("/buttons/bold/reseam_player_button_circle.xml"))
        }
        val fullscreenId = resources.id("fullscreen_button")
        // Placed before these, the buttons would overlap them; they move left of the last button.
        val followingIds = setOf(resources.id("bottom_end_container"), resources.id("multiview_button"))
        val bottomLayout = if (bold) "youtube_video_exploder_controls_bottom_ui_container" else "youtube_controls_bottom_ui_container"
        resources.editXml("layout", bottomLayout) {
            declareNamespace("yt", AUTO_NS)
            val parent = CONSTRAINT_LAYOUTS.flatMap { findByTag(it) }.singleOrNull()
                ?: error("$bottomLayout has no constraint parent")
            fun idOf(element: XmlElement, attr: String) = resourceRef(element[attr].orEmpty())?.toLong()
            val anchor = parent.children.firstOrNull { idOf(it, "android:inflatedId") == fullscreenId }
                ?: error("fullscreen_button is missing from $bottomLayout")
            var last = "@id/fullscreen_button"
            var insertBefore = anchor
            for (layout in layouts) {
                val fragment = bundledResource(if (bold) layout.boldResourcePath else layout.thinResourcePath)
                XmlDocument.compile(fragment.toString(Charsets.UTF_8)).use { source ->
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
