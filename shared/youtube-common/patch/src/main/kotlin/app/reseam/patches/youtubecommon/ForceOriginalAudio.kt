// SPDX-FileCopyrightText: 2026 Cossale <hello@auna.li>
// SPDX-FileCopyrightText: 2026 ReVanced contributors
// SPDX-License-Identifier: GPL-3.0-or-later

package app.reseam.patches.youtubecommon

import app.reseam.patch.ExtClass
import app.reseam.patch.MethodTarget
import app.reseam.patch.PatchRuntime
import app.reseam.patch.Type
import app.reseam.patch.before
import app.reseam.patch.classTarget
import app.reseam.patch.dex.AccessFlags
import app.reseam.patch.field
import app.reseam.patch.method
import app.reseam.patch.methodTarget
import app.reseam.patch.native.NewField
import app.reseam.patch.point
import app.reseam.patch.replace

/** Prefers the original audio track; [activityOnCreate] is where the setting is read. */
fun PatchRuntime.preferOriginalAudio(activityOnCreate: MethodTarget) {
    activityOnCreate.before { call(ForceOriginalAudio.setEnabled) }
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
        val selected = call(ForceOriginalAudio.isDefaultAudioStream, original, audioId, displayName)
        thisObject.set(cacheField, callStatic("java.lang.Boolean", "valueOf", "(Z)Ljava/lang/Boolean;", selected))
        returnValue(selected)
    }
}

private object ForceOriginalAudio : ExtClass("app.reseam.youtube.audio.ForceOriginalAudio") {
    val setEnabled by static()
    val isDefaultAudioStream by static(Type.Boolean, Type.String, Type.String, returns = Type.Boolean)
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

