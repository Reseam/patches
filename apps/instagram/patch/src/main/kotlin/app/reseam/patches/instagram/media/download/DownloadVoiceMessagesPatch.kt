// SPDX-FileCopyrightText: 2026 Reseam contributors <https://reseam.app>
// SPDX-License-Identifier: AGPL-3.0-or-later

package app.reseam.patches.instagram.media.download

import app.reseam.instagram.download.bindings.VoiceMessages
import app.reseam.patch.Type
import app.reseam.patch.after
import app.reseam.patch.before
import app.reseam.patch.dex.Opcode
import app.reseam.patch.klass
import app.reseam.patch.method
import app.reseam.patch.patch
import app.reseam.patch.point
import app.reseam.patch.reserveLocal
import app.reseam.patches.instagram.core.INSTAGRAM
import app.reseam.patches.instagram.core.signatureCheck

private const val MEDIA = "com.instagram.feed.media.Media"
private const val ACTIVITY = "android.app.Activity"
private const val AUDIO_INFO = "com.instagram.api.schemas.AudioInfoIntf"

val downloadVoiceMessages = patch("Download voice messages") {
    description("Adds Save to the menu of voice messages in chats.")
    compatibleWith(INSTAGRAM)
    dependsOn(signatureCheck, downloadMedia)

    execute {
        val icon = resources.id("drawable", "instagram_download_outline_24")?.toLong() ?: error("drawable/instagram_download_outline_24 missing")
        val saveItem = method("saveMediaAction") { name("<init>"); paramCount(0); literals(icon) }
        val contentType = method("messageContentType") {
            inClass(klass(showMessageActions.parameterTypes[1]))
            paramCount(0)
            returns(contentTypeEnum.descriptor)
        }

        // The menu adds Save only when the message's capabilities allow saving media, which they
        // never do for voice messages. The capabilities are final, so the menu's read is changed.
        val voice = showMessageActions.reserveLocal("voiceMessage", Type.Boolean)
        showMessageActions.before { local(voice).assign(VoiceMessages.isVoice(param(1).call(contentType))) }
        showMessageActions.point { field { type(saveItem.owner) } }
            .previous { opcode(Opcode.IGET_BOOLEAN) }
            .captureAs("canSaveMedia")
            .after { whenTrue(local(voice)) { capture("canSaveMedia").assign(bool(true)) } }

        // Instagram's save handles photos and videos and throws on any other message, so a voice
        // message's audio is downloaded before that check.
        val voiceMedia = method("messageParser") { name("unsafeParseFromJson"); strings("voice_media", "direct_media_share") }
            .point { string("voice_media") }
            .next { opcode(Opcode.IPUT_OBJECT) }
            .field("voiceMedia")
        val messageContentType = saveMessageMedia.point { field { type(contentTypeEnum.descriptor) } }.field("saveContentType")
        saveMessageMedia.before {
            val request = paramOfType(messageContentType.owner)
            whenTrue(VoiceMessages.isVoice(request.field(messageContentType))) {
                val audio = request.fieldOfType(voiceMedia.owner).field(voiceMedia).fieldOfType(MEDIA)
                VoiceMessages.download(audio, audio.call(mediaAudio).call(audioSource), paramOfType(saveMessageMedia.owner).fieldOfType(ACTIVITY))
                returnVoid()
            }
        }
    }
}

private val messageActions = klass("messageActions") { strings("DirectMessageActionsInteractor.handleMessageAction") }

private val saveMessageMedia = method("saveMessageMedia") { strings("Invalid message contentType: ") }

private val showMessageActions = method("showMessageActions") {
    inClass(messageActions)
    paramCount(6)
    param(0, "android.graphics.PointF")
    param(3, Type.Long)
    param(4, Type.Boolean)
    param(5, Type.Boolean)
}

private val contentTypeEnum = klass("messageContentType") { enumConstants("VOICE_MEDIA", "REEL_SHARE", "EXPIRING_MEDIA") }

private val mediaAudio = method("mediaAudio") { inClass(klass(MEDIA)); paramCount(0); returns(AUDIO_INFO) }

private val audioSource = method("audioInfoToMap") { strings("audio_src", "audio_src_expiration_timestamp_us"); returns(Type.Map) }
    .point { string("audio_src") }
    .next { invokeInterface { owner(AUDIO_INFO) } }
    .callee("audioSource")
