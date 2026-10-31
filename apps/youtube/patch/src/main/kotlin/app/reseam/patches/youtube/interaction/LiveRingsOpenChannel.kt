// SPDX-FileCopyrightText: 2026 Reseam contributors <https://reseam.app>
// SPDX-License-Identifier: AGPL-3.0-or-later

package app.reseam.patches.youtube.interaction

import app.reseam.patch.Type
import app.reseam.patch.before
import app.reseam.patch.method
import app.reseam.patch.patch
import app.reseam.patch.settings.gate
import app.reseam.patch.settings.section
import app.reseam.patches.youtube.core.YOUTUBE
import app.reseam.patches.youtube.core.YouTubeSettingsPages
import app.reseam.patches.youtube.core.youTubeSettings
import app.reseam.patches.youtube.internal.commandRouter
import app.reseam.patches.youtubecommon.protobufBytes
import app.reseam.youtube.misc.bindings.LiveAvatar
import app.reseam.youtube.settings.FeedSettings
import app.reseam.youtube.settings.YouTubeStrings

val liveRingsOpenChannel = patch("Live rings open the channel") {
    description("Opens the channel when its live ring is tapped instead of starting the live stream.")
    compatibleWith(YOUTUBE)
    dependsOn(commandRouter)
    settings(youTubeSettings, section(YouTubeSettingsPages.Feed, YouTubeStrings.sectionFeed, FeedSettings.liveRingsOpenChannel))

    execute {
        gate(FeedSettings.liveRingsOpenChannel) {
            watchCommand.before { whenTrue(LiveAvatar.openChannel(protobufBytes(param(0)))) { returnVoid() } }
        }
    }
}

// Every tap that plays a video in the regular player runs this command.
private val watchCommand = method("watchCommand") {
    strings("force_fullscreen", "VideoPresenterConstants.VIDEO_THUMBNAIL_BITMAP_KEY")
    hasParam(Type.Map)
    returns(Type.Void)
}
