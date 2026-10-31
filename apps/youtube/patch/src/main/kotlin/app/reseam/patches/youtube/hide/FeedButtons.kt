// SPDX-FileCopyrightText: 2026 Reseam contributors <https://reseam.app>
// SPDX-FileCopyrightText: 2026 ReVanced contributors
// SPDX-License-Identifier: GPL-3.0-or-later

package app.reseam.patches.youtube.hide

import app.reseam.patch.PatchRuntime
import app.reseam.patch.Type
import app.reseam.patch.after
import app.reseam.patch.before
import app.reseam.patch.dex.AccessFlags
import app.reseam.patch.method
import app.reseam.patch.methods
import app.reseam.patch.point
import app.reseam.patch.points
import app.reseam.patch.settings.gate
import app.reseam.youtube.hidelayout.bindings.LayoutViews
import app.reseam.youtube.settings.FeedSettings

internal fun PatchRuntime.hideFeedButtons() {
    // The shared FAB controller hides the FAB when a search screen hands it no voice search state.
    val voiceSearch = requireNotNull(resources.id("string", "action_bar_voice_search")) {
        "string/action_bar_voice_search is missing"
    }.toLong()
    val voiceSearchFabs = methods("voiceSearchFab") { literals(voiceSearch) }
        .points("voiceSearchFabLabel") { literal(voiceSearch) }
        .all.also { check(it.isNotEmpty()) { "No voice search button state" } }
    gate(FeedSettings.hideFloatingMicrophoneButton) {
        voiceSearchFabs.forEach { label ->
            label.next { invokeDirect { name("<init>") } }
                .captureArgumentAs("voiceSearchFab", 0)
                .after { capture("voiceSearchFab").assign(nullObject) }
        }
    }

    val contentPill = requireNotNull(resources.id("layout", "content_pill")) {
        "layout/content_pill is missing"
    }.toLong()
    val bar = requireNotNull(resources.id("layout", "bar")) { "layout/bar is missing" }.toLong()
    listOf(
        method("latestVideosContentPill") {
            flags(AccessFlags.PUBLIC or AccessFlags.FINAL)
            paramCount(2)
            returns(Type.Void)
            literals(contentPill)
            calls { name("inflate"); paramCount(3); returns(Type.View) }
        },
        method("latestVideosBar") {
            flags(AccessFlags.PUBLIC or AccessFlags.FINAL)
            paramCount(2)
            returns(Type.Void)
            literals(bar)
            calls { name("inflate"); paramCount(3); returns(Type.View) }
        },
    ).forEach { target ->
        gate(FeedSettings.hideLatestVideosButton) {
            target.point("latestVideosButton") {
                invokeVirtual {
                    name("inflate")
                    paramCount(3)
                    returns(Type.View)
                }
            }
                .next { resultOf(Type.View) }
                .captureAs("view", Type.View)
                .after {
                    LayoutViews.hideLatestVideosButton(capture("view"))
                }
        }
    }

    val youtubeLogo = requireNotNull(resources.id("id", "youtube_logo")) {
        "id/youtube_logo is missing"
    }.toLong()
    val yoodles = method("yoodlesImageView") {
        flags(AccessFlags.PUBLIC or AccessFlags.FINAL)
        paramCount(2)
        returns(Type.View)
        literals(youtubeLogo)
    }
    val setImageDrawable: app.reseam.patch.PointMatch.() -> Unit = {
        invokeVirtual {
            owner("Landroid/widget/ImageView;")
            name("setImageDrawable")
            params("android.graphics.drawable.Drawable")
            returns(Type.Void)
        }
    }
    gate(FeedSettings.hideDoodles) {
        yoodles.point("doodleDrawable", setImageDrawable)
            .captureArgumentAs("image", 0, IMAGE_VIEW)
            .captureArgumentAs("drawable", 1, "android.graphics.drawable.Drawable")
            .before { capture("drawable").assign(nullObject) }
    }
}
