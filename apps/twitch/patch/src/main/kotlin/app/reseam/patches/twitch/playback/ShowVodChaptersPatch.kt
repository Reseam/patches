// SPDX-FileCopyrightText: 2026 Reseam contributors <https://reseam.app>
// SPDX-License-Identifier: AGPL-3.0-or-later

package app.reseam.patches.twitch.playback

import app.reseam.patch.Type
import app.reseam.patch.klass
import app.reseam.patch.method
import app.reseam.patch.patch
import app.reseam.patch.settings.gate
import app.reseam.patch.settings.section
import app.reseam.patches.twitch.TwitchPatchStrings
import app.reseam.patches.twitch.core.TWITCH
import app.reseam.patches.twitch.core.TwitchSettingsPages
import app.reseam.patches.twitch.core.playerOverlay
import app.reseam.patches.twitch.core.playerViewAttached
import app.reseam.patches.twitch.core.resourceId
import app.reseam.patches.twitch.core.twitchSettings
import app.reseam.patches.twitch.core.viewLookup
import app.reseam.twitch.playback.bindings.VodChapters
import app.reseam.twitch.settings.PlaybackSettings
import app.reseam.twitch.settings.TwitchStrings

private const val CHAPTERS_ICON = "list"

// Twitch's R8 rules keep the names of its models.
private const val VOD_MODEL = "tv.twitch.android.models.videos.VodModel"

val showVodChapters = patch(TwitchPatchStrings.showVodChapters) {
    description(TwitchPatchStrings.showVodChaptersDescription)
    compatibleWith(TWITCH)
    settings(twitchSettings, section(TwitchSettingsPages.Player, TwitchStrings.sectionPlayerButtons, PlaybackSettings.showVodChapters))

    execute {
        VodChapters.vodId.implement {
            whenInstanceOf(param(0), vodPlayerPresenter.descriptor) {
                val vod = param(0).cast(vodPlayerPresenter.descriptor).fieldOfType(VOD_MODEL)
                whenNotNull(vod) { returnValue(vod.callVirtual(VOD_MODEL, "getId", "()Ljava/lang/String;")) }
            }
            returnNull()
        }
        VodChapters.seek.implement {
            whenInstanceOf(param(0), vodPlayerPresenter.descriptor) { param(0).cast(vodPlayerPresenter.descriptor).call(vodSeek, param(1)) }
            returnVoid()
        }

        val settingsButton = viewLookup(playerOverlay(), "settings_button")
        val icon = resourceId("drawable", CHAPTERS_ICON).toInt()

        gate(PlaybackSettings.showVodChapters) {
            playerViewAttached.before { VodChapters.trackPlayer(capture("playerView"), thisObject) }
            settingsButton.after { VodChapters.addChaptersButton(capture("view"), int(icon)) }
        }
    }
}

private val vodPlayerPresenter = klass("vodPlayerPresenter") { strings("Should not be called from VodPlayerPresenter") }
private val vodSeek = vodPlayerPresenter.method("seekTo") {
    params(Type.Int)
    returns(Type.Void)
}
