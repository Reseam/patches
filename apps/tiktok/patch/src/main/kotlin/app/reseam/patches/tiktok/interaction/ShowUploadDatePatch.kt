// SPDX-FileCopyrightText: 2026 Reseam contributors <https://reseam.app>
// SPDX-License-Identifier: AGPL-3.0-or-later

package app.reseam.patches.tiktok.interaction

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

private val authorInfo =
    klass("com.ss.android.ugc.aweme.feed.assem.videoauthorinfo.VideoAuthorInfoVM")
        .method("paramSync2StateAccept")

private val videoItemAweme = klass(VIDEO_ITEM_PARAMS).method("getAweme")
