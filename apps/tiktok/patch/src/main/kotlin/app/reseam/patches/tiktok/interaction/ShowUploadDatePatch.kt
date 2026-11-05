// SPDX-FileCopyrightText: 2026 Reseam contributors <https://reseam.app>
// SPDX-License-Identifier: AGPL-3.0-or-later

package app.reseam.patches.tiktok.interaction

import app.reseam.patch.Type
import app.reseam.patch.after
import app.reseam.patch.klass
import app.reseam.patch.method
import app.reseam.patch.patch
import app.reseam.patch.points
import app.reseam.patch.settings.gate
import app.reseam.patch.settings.section
import app.reseam.patches.tiktok.core.TIKTOK
import app.reseam.patches.tiktok.core.TikTokSettingsPages
import app.reseam.patches.tiktok.core.VIDEO_ITEM_PARAMS
import app.reseam.patches.tiktok.core.tiktokSettings
import app.reseam.tiktok.interaction.bindings.UploadDate
import app.reseam.tiktok.settings.InterfaceSettings
import app.reseam.tiktok.settings.TikTokStrings

val showUploadDate =
    patch("Show upload date") {
        description("Shows the date and region a video was posted next to the author's name.")
        compatibleWith(TIKTOK)
        settings(
            tiktokSettings,
            section(
                TikTokSettingsPages.Video,
                TikTokStrings.sectionOverlay,
                InterfaceSettings.showUploadDate,
            ),
        )

        execute {
            val state = authorInfoState.classDef
            val postTime =
                state.directMethods
                    .single { it.name == "<init>" && it.parameterTypes.size == 5 }
                    .parameterTypes
                    .lastIndexOf("Ljava/lang/String;") + 1
            gate(InterfaceSettings.showUploadDate) {
                // TikTok's own time, where it shows one, would repeat the date.
                authorInfo
                    .points {
                        invokeStatic {
                            owner(VIDEO_AUTHOR_INFO_VM)
                            params(Type.String)
                            returns(Type.String)
                        }
                    }
                    .also { check(it.all.isNotEmpty()) { "No author info times" } }
                    .forEach {
                        next { resultOf(Type.String) }.captureAs("time", Type.String).after {
                            capture("time").assign(string(""))
                        }
                    }
                authorInfo
                    .points {
                        invokeDirect {
                            owner(state.descriptor)
                            name("<init>")
                            paramCount(5)
                        }
                    }
                    .also { check(it.all.isNotEmpty()) { "No author info states built" } }
                    .forEach {
                        captureArgumentAs("postTime", postTime).before {
                            val aweme = param(1).call(videoItemAweme)
                            capture("postTime")
                                .assign(call(UploadDate.postTime, capture("postTime"), aweme))
                        }
                    }
            }
        }
    }

// The line under the author's name: titleViewText, titleViewVisible, titleViewTabletFollowVisible,
// titleVerifyInfo, postTimeText.
private val authorInfoState =
    klass("authorInfoState") { strings("VideoAuthorInfoState(titleViewText=") }

private const val VIDEO_AUTHOR_INFO_VM =
    "com.ss.android.ugc.aweme.feed.assem.videoauthorinfo.VideoAuthorInfoVM"

private val authorInfo = klass(VIDEO_AUTHOR_INFO_VM).method("paramSync2StateAccept")

private val videoItemAweme = klass(VIDEO_ITEM_PARAMS).method("getAweme")
