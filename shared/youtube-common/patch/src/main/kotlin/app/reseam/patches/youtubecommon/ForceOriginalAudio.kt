// SPDX-FileCopyrightText: 2026 Reseam contributors <https://reseam.app>
// SPDX-FileCopyrightText: 2026 ReVanced contributors
// SPDX-License-Identifier: GPL-3.0-or-later

package app.reseam.patches.youtubecommon

import app.reseam.patch.MethodTarget
import app.reseam.patch.PatchRuntime
import app.reseam.patch.Type
import app.reseam.patch.addMethod
import app.reseam.patch.after
import app.reseam.patch.before
import app.reseam.patch.classTarget
import app.reseam.patch.dex.AccessFlags
import app.reseam.patch.dex.parameterTypes
import app.reseam.patch.field
import app.reseam.patch.fieldOfType
import app.reseam.patch.klass
import app.reseam.patch.method
import app.reseam.patch.methodTarget
import app.reseam.patch.point
import app.reseam.patch.replace
import app.reseam.patch.types.NewField
import app.reseam.youtube.audio.bindings.AudioTrack
import app.reseam.youtube.audio.bindings.AudioTrackSelector
import app.reseam.youtube.audio.bindings.ForceOriginalAudio

private val AUDIO_TRACK_INTERFACE = AudioTrack.descriptor
private val AUDIO_TRACK_SELECTOR_INTERFACE = AudioTrackSelector.descriptor

/** Prefers the original audio track; [activityOnCreate] is where the setting is read. */
fun PatchRuntime.preferOriginalAudio(activityOnCreate: MethodTarget) {
    activityOnCreate.before { ForceOriginalAudio.setEnabled() }
    val cacheName = "reseam_is_default_audio_override"
    formatStreamClass.classDef.addField(
        NewField(
            name = cacheName,
            fieldType = "Ljava/lang/Boolean;",
            accessFlags = (AccessFlags.PRIVATE or AccessFlags.VOLATILE).toUInt(),
            initialValue = null,
        ),
    )
    val cacheField = formatStreamClass.field(cacheName)
    // Resolve these callees before replacing the getter; their owners are part of the
    // format-stream model identified by the same structural toString target.
    val audioTrackIdMethod = audioTrackId.also { it.owner }
    val audioTrackDisplayNameMethod = audioTrackDisplayName.also { it.owner }

    val originalDefaultAudioTrackName = "reseam_original_default_audio_track"
    isDefaultAudioTrack.method.clone(originalDefaultAudioTrackName)
    val originalDefaultAudioTrack = methodTarget("original default audio getter") {
        formatStreamClass.classDef.method(originalDefaultAudioTrackName, isDefaultAudioTrack.proto)
            ?: error("The cloned default audio getter is missing")
    }

    // The clone keeps YouTube's original branches and return values. The wrapper only adds
    // the cached policy decision through the code emitter.
    isDefaultAudioTrack.replace {
        val cached = thisObject.field(cacheField)
        whenNotNull(cached) {
            returnValue(cached.callVirtual("java.lang.Boolean", "booleanValue", "()Z"))
        }
        val original = thisObject.call(originalDefaultAudioTrack)
        val audioId = thisObject.call(audioTrackIdMethod)
        val displayName = thisObject.call(audioTrackDisplayNameMethod)
        val selected = ForceOriginalAudio.isDefaultAudioStream(original, audioId, displayName)
        thisObject.set(cacheField, callStatic("java.lang.Boolean", "valueOf", "(Z)Ljava/lang/Boolean;", selected))
        returnValue(selected)
    }

    newVideoId.after { ForceOriginalAudio.newVideo(capture("videoId")) }
    selectOriginalAudioTrack(audioTrackIdMethod)
}

// The server picks the streamed track from the account's preferred languages, so the original is
// chosen through the player controller, the same call the audio track menu makes.
private fun PatchRuntime.selectOriginalAudioTrack(formatAudioTrackId: MethodTarget) {
    val trackClass = klass(audioTrackToString.owner)
    trackClass.classDef.addInterface(AUDIO_TRACK_INTERFACE)
    trackClass.addMethod("patch_getId", "()Ljava/lang/String;") {
        returnValue(thisObject.field(audioTrackIdField))
    }

    val controllerClass = klass(selectAudioTrack.owner)
    controllerClass.classDef.addInterface(AUDIO_TRACK_SELECTOR_INTERFACE)
    controllerClass.addMethod("patch_selectAudioTrack", "(Ljava/lang/String;)V") {
        thisObject.call(selectAudioTrack, param(0))
        returnVoid()
    }

    val qualityAuto = requireNotNull(resources.id("string", "quality_auto")) { "string/quality_auto is missing" }.toLong()
    val formatChanged = method("format change handler") {
        literals(qualityAuto)
        paramCount(1)
        returns(Type.Void)
    }
    val eventClass = klass(formatChanged.parameterTypes.single())
    val formatEventToString = method("format event toString") {
        inClass(eventClass)
        name("toString")
        returns(Type.String)
        strings(" currentAudioFormat=")
    }
    val currentAudioFormat = formatEventToString
        .point("current video format") { field { type(formatStreamClass.descriptor) } }
        .next { field { type(formatStreamClass.descriptor) } }
        .field("current audio format")
    val currentAudioTrackId = eventClass.addMethod("patch_getCurrentAudioTrackId", "()Ljava/lang/String;") {
        val format = thisObject.field(currentAudioFormat)
        whenNotNull(format) { returnValue(format.call(formatAudioTrackId)) }
        returnNull()
    }
    val tracksField = eventClass.fieldOfType("[${audioTrackToString.owner}")
    val controllerField = klass(formatChanged.owner).fieldOfType(selectAudioTrack.owner)
    formatChanged.before {
        ForceOriginalAudio.formatChanged(
            thisObject.field(controllerField).cast(AUDIO_TRACK_SELECTOR_INTERFACE),
            param(0).field(tracksField).cast("[$AUDIO_TRACK_INTERFACE"),
            param(0).call(currentAudioTrackId),
        )
    }
}

private val formatStreamToString = method("format stream toString") {
    name("toString")
    flags(AccessFlags.PUBLIC or AccessFlags.FINAL)
    returns(Type.String)
    params()
    strings(" isDefaultAudioTrack=")
}

private val formatStreamClass = classTarget("format stream class") {
    bytecode.findClass(formatStreamToString.owner) ?: error("The format stream class is missing")
}

private val isDefaultAudioTrack = formatStreamToString
    .point("default audio label") { stringContains("isDefaultAudioTrack=") }
    .next { invokeDirect { name("<init>"); owner("java.lang.StringBuilder") } }
    .next { invokeVirtual { owner("java.lang.StringBuilder"); name("append"); params(Type.Boolean) } }
    .writer(1, "default audio getter")
    .previous { invokeVirtual { owner(formatStreamToString.owner); returns(Type.Boolean); params() } }
    .callee("default audio getter")

private val audioTrackId = formatStreamToString
    .point("audio track id label") { stringContains("audioTrackId=") }
    .next { invokeVirtual { owner("java.lang.StringBuilder"); name("append"); params(Type.String) } }
    .next { invokeVirtual { owner("java.lang.StringBuilder"); name("append"); params(Type.String) } }
    .writer(1, "audio track id getter")
    .previous { invokeVirtual { owner(formatStreamToString.owner); returns(Type.String); params() } }
    .callee("audio track id getter")

private val audioTrackDisplayName = formatStreamToString
    .point("audio track display label") { stringContains("audioTrackDisplayName=") }
    .next { invokeVirtual { owner("java.lang.StringBuilder"); name("append"); params(Type.String) } }
    .next { invokeVirtual { owner("java.lang.StringBuilder"); name("append"); params(Type.String) } }
    .writer(1, "audio track display getter")
    .previous { invokeVirtual { owner(formatStreamToString.owner); returns(Type.String); params() } }
    .callee("audio track display getter")

private val audioTrackToString = method("audio track toString") {
    name("toString")
    params()
    returns(Type.String)
    strings("id;displayName;isAutoDubbed;isDefault")
}

private val audioTrackIdField = audioTrackToString
    .point("audio track id") { field { type(Type.String) } }
    .field("audio track id")

private val audioTrackMenuClick = method("audio track menu click") {
    name("onItemClick")
    returns(Type.Void)
    accesses { owner(audioTrackToString.owner); type(Type.String) }
}

private val selectAudioTrack = audioTrackMenuClick
    .point("chosen audio track id") { field { owner(audioTrackToString.owner); type(Type.String) } }
    .next { invokeVirtual { params(Type.String); returns(Type.Void) } }
    .callee("select audio track")
