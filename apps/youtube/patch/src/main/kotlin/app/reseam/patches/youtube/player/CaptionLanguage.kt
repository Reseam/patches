// SPDX-FileCopyrightText: 2026 Reseam contributors <https://reseam.app>
// SPDX-License-Identifier: AGPL-3.0-or-later

package app.reseam.patches.youtube.player

import app.reseam.patch.CodeScope
import app.reseam.patch.FieldTarget
import app.reseam.patch.PointTarget
import app.reseam.patch.Type
import app.reseam.patch.ValueRef
import app.reseam.patch.addMethod
import app.reseam.patch.after
import app.reseam.patch.classTarget
import app.reseam.patch.dex.AccessFlags
import app.reseam.patch.dex.Opcode
import app.reseam.patch.dex.opcode
import app.reseam.patch.dex.parameterTypes
import app.reseam.patch.klass
import app.reseam.patch.method
import app.reseam.patch.patch
import app.reseam.patch.point
import app.reseam.patch.settings.section
import app.reseam.patches.youtube.YouTubePatchStrings
import app.reseam.patches.youtube.core.YOUTUBE
import app.reseam.patches.youtube.core.YouTubeSettingsPages
import app.reseam.patches.youtube.core.youTubeSettings
import app.reseam.youtube.playerui.bindings.CaptionLanguage
import app.reseam.youtube.playerui.bindings.CaptionTrack
import app.reseam.youtube.playerui.bindings.CaptionTracks
import app.reseam.youtube.settings.PlayerOverlaySettings
import app.reseam.youtube.settings.YouTubeStrings

private const val CAPTIONING_MANAGER = "android.view.accessibility.CaptioningManager"
private const val LIST = "java.util.List"
private const val STRING = "Ljava/lang/String;"

val captionLanguage = patch(YouTubePatchStrings.captionLanguage) {
    description(YouTubePatchStrings.captionLanguageDescription)
    compatibleWith(YOUTUBE)
    settings(youTubeSettings, section(YouTubeSettingsPages.Overlay, YouTubeStrings.sectionPlayerOverlay, PlayerOverlaySettings.captionLanguage))

    execute {
        val track = klass(captionTrack.descriptor)
        track.classDef.addInterface(CaptionTrack.descriptor)
        track.addMethod("patch_getLanguageCode", "()$STRING") { returnValue(trackProperty(trackLanguage)) }
        track.addMethod("patch_getVssId", "()$STRING") { returnValue(sourceField(sourceVssId.field())) }
        track.addMethod("patch_getUrl", "()$STRING") { returnValue(sourceField(sourceUrl.field())) }
        // Builds a translation of this track the way YouTube builds the ones it offers.
        track.addMethod("patch_translate", "($STRING$STRING$STRING$STRING)${CaptionTrack.descriptor}") {
            val builder = call(newBuilder.callee())
            builder.call(setLanguage.callee(), param(0))
            builder.call(setId.callee(), string(""))
            builder.call(setVideoId.callee(), sourceField(setVideoId.writer(1).field()))
            builder.call(setVssId.callee(), param(1))
            builder.call(setUrl.callee(), param(2))
            builder.set(displayName.field(), param(3))
            returnValue(builder.call(displayName.next { invokeVirtual { returns(captionTrack.descriptor) } }.callee()))
        }

        val tracks = klass(captionTracks.descriptor)
        tracks.classDef.addInterface(CaptionTracks.descriptor)
        tracks.addMethod("patch_getTracks", "()Ljava/util/List;") { returnValue(thisObject.call(menuTracks)) }

        // Every track YouTube picks by itself, for a new video or another audio track, passes here.
        selectedTrack.captureAs("track", captionTrack.descriptor).after {
            capture("track").assign(
                CaptionLanguage.selectTrack(
                    thisObject.field(currentTracks).cast(CaptionTracks.descriptor),
                    capture("track").cast(CaptionTrack.descriptor),
                    param(0).fieldOfType(selectionReason.descriptor)
                        .callVirtual("java.lang.Enum", "name", "()$STRING"),
                ).cast(captionTrack.descriptor),
            )
        }
    }
}

// The off entry of the captions menu is a track, told apart by this marker.
private val isOffOption = method("isOffOption") {
    strings("DISABLE_CAPTIONS_OPTION")
    params()
    returns(Type.Boolean)
}

private val captionTrack = classTarget("captionTrack") {
    bytecode.findClass(isOffOption.owner) ?: error("The caption track class is missing")
}

private val trackLanguage = isOffOption.point { invokeVirtual { owner(Type.String); name("equals") } }
    .writer(1, "trackLanguage")

// Releases before 21 read a track's properties through getters, later ones from fields.
private fun CodeScope.trackProperty(reader: PointTarget): ValueRef =
    if (reader.instruction.opcode == Opcode.IGET_OBJECT) {
        thisObject.field(reader.field())
    } else {
        thisObject.call(reader.previous { invokeVirtual { params(); returns(Type.String) } }.callee())
    }

// Releases before 21 keep a track's fields in its generated subclass.
private fun CodeScope.sourceField(field: FieldTarget): ValueRef = thisObject.cast(field.owner).field(field)

private val offOption = method("offOption") {
    strings("DISABLE_CAPTIONS_OPTION")
    flags(AccessFlags.STATIC)
    params(Type.String)
    returns(captionTrack.descriptor)
}

// YouTube builds the translations it offers here, from the translation languages of the video.
private val translations = method("translations") {
    strings("&tlang=")
    params()
    returns(LIST)
}

private val captionTracks = classTarget("captionTracks") {
    bytecode.findClass(translations.owner) ?: error("The caption tracks class is missing")
}

private val menuTracks = method("menuTracks") {
    inClass(captionTracks)
    calls(offOption)
    params()
    returns(LIST)
}

private val newBuilder = translations.point("newBuilder") { invokeStatic { owner(captionTrack.descriptor); params() } }
private val setLanguage = newBuilder.next { invokeVirtual { params(Type.String); returns(Type.Void) } }
private val setId = setLanguage.next { invokeVirtual { params(Type.String); returns(Type.Void) } }
private val setVideoId = setId.next { invokeVirtual { params(Type.String); returns(Type.Void) } }
private val setVssId = setVideoId.next { invokeVirtual { params(Type.String); returns(Type.Void) } }
private val sourceVssId = setVssId.previous { invokeStatic { params(Type.String, Type.String); returns(Type.String) } }
    .writer(0, "sourceVssId")
private val sourceUrl = translations.point { string("&tlang=") }.previous { field { type(Type.String) } }
private val setUrl = sourceUrl.next { invokeVirtual { params(Type.String); returns(Type.Void) } }
private val displayName = setUrl.next { field { type("java.lang.CharSequence") } }

// Falls back from the remembered captions language to the system's captions locale.
private val startTrack = method("startTrack") {
    calls { owner(CAPTIONING_MANAGER); name("getLocale") }
    params()
    returns(captionTrack.descriptor)
}

private val currentTracks = startTrack.point("currentTracks") { field { type(captionTracks.descriptor) } }.field()

private val captionsOn = method("captionsOn") {
    flags(AccessFlags.STATIC)
    hasParam(CAPTIONING_MANAGER)
    returns(Type.Boolean)
    calls { owner(CAPTIONING_MANAGER); name("isEnabled") }
}

// A new video's captions: checks whether captions are on, then picks its start track.
private val newVideoCaptions = method("newVideoCaptions") {
    calls(captionsOn)
    calls(startTrack)
}

private val applySelection = newVideoCaptions.point {
    invokeVirtual { returns(Type.Void); paramCount(1) }
}.callee("applySelection")

private val selectionReasons = method("selectionReasons") { strings("COMPOSITE_VIDEO_CHANGED", "PREFERRED_TRACK") }

private val selectionReason = classTarget("selectionReason") {
    bytecode.findClass(selectionReasons.owner) ?: error("The caption selection reason enum is missing")
}

private val selectedTrack = applySelection.point {
    field { owner(applySelection.parameterTypes.single()); type(captionTrack.descriptor) }
}
