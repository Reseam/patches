// SPDX-FileCopyrightText: 2026 AunAli K. <hello@auna.li>
// SPDX-FileCopyrightText: 2026 ReVanced contributors
// SPDX-License-Identifier: GPL-3.0-or-later

package app.reseam.patches.youtube.video.speed

import app.reseam.patch.ExtClass
import app.reseam.patch.Type
import app.reseam.patch.after
import app.reseam.patch.alwaysReturn
import app.reseam.patch.before
import app.reseam.patch.classTarget
import app.reseam.patch.dex.AccessFlags
import app.reseam.patch.dex.Opcode
import app.reseam.patch.dex.descriptor
import app.reseam.patch.dex.literal
import app.reseam.patch.dex.opcode
import app.reseam.patch.dex.parameterTypes
import app.reseam.patch.dex.returnType
import app.reseam.patch.field
import app.reseam.patch.method
import app.reseam.patch.native.NewField
import app.reseam.patch.patch
import app.reseam.patch.point
import app.reseam.patch.settings.section
import app.reseam.patches.youtube.core.YOUTUBE
import app.reseam.patches.youtube.core.YouTubeSettings
import app.reseam.patches.youtube.core.YouTubeSettingsPages
import app.reseam.patches.youtube.core.youTubeSettings
import app.reseam.patches.youtube.internal.lithoFilter
import app.reseam.patches.youtube.internal.lithoRecyclerViewBinder
import app.reseam.patches.youtube.internal.registerLithoFilter
import app.reseam.patches.youtube.internal.videoInformationHook

object CustomPlaybackSpeed : ExtClass("app.reseam.youtube.speed.CustomPlaybackSpeed") {
    val getTapAndHoldSpeed = static("getTapAndHoldSpeed", returns = Type.Float)
    val customPlaybackSpeedCount = static("customPlaybackSpeedCount", returns = Type.Int)
    val onFlyoutMenuCreate = static("onFlyoutMenuCreate", Type.View)
    val openOldPlaybackSpeedMenu = static("openOldPlaybackSpeedMenu")
    val customPlaybackSpeeds = field("customPlaybackSpeeds", "[F")
    val maximumSpeed = field("PLAYBACK_SPEED_MAXIMUM", Type.Float)
    val minimumSpeed = field("SPEED_LIMIT_MINIMUM", Type.Float)
}

/** YouTube's old speed menu, which builds its list from the speed values it is given. */
internal val oldPlaybackSpeedMenu = method("old playback speed menu") {
    strings("menu_item_playback_speed")
    paramCount(2)
    param(1, Type.Int)
    custom { parameterTypes[0].startsWith("[L") }
}

private val oldPlaybackSpeedMenuClass = classTarget("old playback speed menu class") {
    bytecode.findClass(oldPlaybackSpeedMenu.owner) ?: error("The old playback speed menu class is missing")
}

private object PlaybackSpeedMenuFilter : ExtClass("app.reseam.youtube.speed.PlaybackSpeedMenuFilter")

private val speedLimiterMethod = method("speed limiter") {
    flags(AccessFlags.PUBLIC or AccessFlags.FINAL)
    returns(Type.Void)
    paramCount(2)
    param(0, Type.Float)
    custom { parameterTypes[1].startsWith("L") }
    strings("setPlaybackRate")
}

private val serverSideMaxSpeedFlag = method("server side maximum speed flag") {
    flags(AccessFlags.PUBLIC or AccessFlags.FINAL)
    returns(Type.Boolean)
    params()
    literals(45719140L)
}

private val speedArrayGenerator = method("playback speed array generator") {
    flags(AccessFlags.PUBLIC or AccessFlags.STATIC)
    paramCount(1)
    custom { parameterTypes[0].startsWith("L") }
    custom { returnType.startsWith("[L") }
    strings("0.0#")
}

private val speedArray = speedArrayGenerator.point("original playback speed array") {
    field { type("[F") }
}

private val serverSpeedCount = speedArrayGenerator
    .point("server playback speed count") { invokeInterface { name("size"); returns(Type.Int) } }
    .next { resultOf(Type.Int) }

private val generatedSpeedCount = speedArrayGenerator.point("generated playback speed count") { literal(7) }

private val tapAndHoldSpeed = method("tap and hold playback speed") {
    name("run")
    flags(AccessFlags.PUBLIC or AccessFlags.FINAL)
    returns(Type.Void)
    params()
    literals(2.0f.toRawBits().toLong())
    calls { owner("Landroid/os/Handler;"); name("removeCallbacks") }
}

private val tapAndHoldSpeedLiteral = tapAndHoldSpeed.point("tap and hold speed literal") {
    opcode(Opcode.CONST_HIGH16)
    literal((2.0f.toRawBits()).toLong())
}.captureAs("tapSpeed", Type.Float)

// The quality menu can already have inserted its listener immediately before this call. Anchor
// the stable framework call itself so both filters can share the binder without re-matching the
// now-edited four-instruction sequence.
private val playbackSpeedRecyclerViewAttached = lithoRecyclerViewBinder.point("playback speed recycler view attached") {
    invokeVirtual {
        owner("android.support.v7.widget.RecyclerView")
        paramCount(1)
        returns(Type.Void)
    }
}

val customPlaybackSpeed = patch("Custom playback speed") {
    description("Adds configured playback speeds and supports the modern tap-and-hold speed.")
    compatibleWith(YOUTUBE)
    dependsOn(youTubeSettings, videoInformationHook, lithoFilter)
    settings(
        youTubeSettings,
        section(
            YouTubeSettingsPages.Video,
            "Playback speed",
            YouTubeSettings.customPlaybackSpeedMenu,
            YouTubeSettings.restoreOldPlaybackSpeedMenu,
            YouTubeSettings.customPlaybackSpeeds,
            YouTubeSettings.playbackSpeedTapAndHold,
        ),
    )

    execute {
        speedLimiterMethod.point("minimum playback speed") {
            opcode(Opcode.CONST_HIGH16)
            literal((0.25f.toRawBits()).toLong())
        }.captureAs("minimum", Type.Float).after {
            capture("minimum").assign(staticField(CustomPlaybackSpeed.minimumSpeed))
        }
        speedLimiterMethod.point("maximum playback speed") {
            opcode(Opcode.CONST_HIGH16)
            literal((4.0f.toRawBits()).toLong())
        }.captureAs("maximum", Type.Float).after {
            capture("maximum").assign(staticField(CustomPlaybackSpeed.maximumSpeed))
        }
        serverSideMaxSpeedFlag.alwaysReturn(false)

        // The old speed menu lists the custom speeds: an empty server list makes YouTube generate
        // its own, which then takes its length and values from the custom array.
        serverSpeedCount.captureAs("serverSpeeds", Type.Int).after { capture("serverSpeeds").assign(int(0)) }
        generatedSpeedCount.captureAs("generatedSpeeds", Type.Int).after {
            capture("generatedSpeeds").assign(call(CustomPlaybackSpeed.customPlaybackSpeedCount))
        }
        speedArray.captureAs("playbackSpeeds", "[F").after {
            capture("playbackSpeeds").assign(staticField(CustomPlaybackSpeed.customPlaybackSpeeds))
        }

        val menuType = oldPlaybackSpeedMenuClass.descriptor
        oldPlaybackSpeedMenuClass.classDef.addField(
            NewField(
                name = "INSTANCE",
                fieldType = menuType,
                accessFlags = (AccessFlags.PUBLIC or AccessFlags.STATIC).toUInt(),
                initialValue = null,
            ),
        )
        val menuInstance = field(menuType, "INSTANCE", menuType)
        oldPlaybackSpeedMenu.before { setStatic(menuInstance, thisObject) }
        val unavailableMessage = requireNotNull(resources.id("string", "varispeed_unavailable_message")) {
            "string/varispeed_unavailable_message is missing"
        }.toLong()
        val showOldMenu = method("show old playback speed menu") {
            inClass(oldPlaybackSpeedMenuClass)
            literals(unavailableMessage)
        }
        CustomPlaybackSpeed.openOldPlaybackSpeedMenu.implement {
            val menu = staticField(menuInstance)
            whenNotNull(menu) { menu.call(showOldMenu) }
            returnVoid()
        }

        tapAndHoldSpeedLiteral.after {
            capture("tapSpeed").assign(call(CustomPlaybackSpeed.getTapAndHoldSpeed))
        }

        playbackSpeedRecyclerViewAttached.before {
            call(CustomPlaybackSpeed.onFlyoutMenuCreate, param(1))
        }
        registerLithoFilter(PlaybackSpeedMenuFilter)
    }
}
