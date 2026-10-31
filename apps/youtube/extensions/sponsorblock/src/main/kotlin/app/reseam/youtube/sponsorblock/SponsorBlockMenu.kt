// SPDX-FileCopyrightText: 2026 Reseam contributors <https://reseam.app>
// SPDX-License-Identifier: AGPL-3.0-or-later

package app.reseam.youtube.sponsorblock

import android.app.AlertDialog
import app.reseam.youtube.settings.VideoSettings
import app.reseam.youtube.settings.YouTubeStrings

internal data class SponsorBlockAction(val label: String, val invoke: () -> Unit)

internal fun AlertDialog.Builder.setActions(
    actions: List<SponsorBlockAction>
): AlertDialog.Builder =
    setItems(actions.map { it.label }.toTypedArray()) { _, which -> actions[which].invoke() }

internal fun showSponsorBlockMenu(overlay: SponsorBlockOverlay, createSegment: () -> Unit) {
    val context = overlay.context
    val items =
        buildList<SponsorBlockAction> {
            add(
                SponsorBlockAction(YouTubeStrings.sponsorblockSegmentsAndVoting) {
                    showSponsorBlockSegments(context)
                }
            )
            if (VideoSettings.sbCreateNewSegment.value)
                add(SponsorBlockAction(YouTubeStrings.sponsorblockCreateSegment, createSegment))
            add(
                SponsorBlockAction(YouTubeStrings.sponsorblockStatistics) {
                    showSponsorBlockStats(context, overlay)
                }
            )
            add(
                SponsorBlockAction(YouTubeStrings.sponsorblockAbout) {
                    AlertDialog.Builder(context)
                        .setTitle(YouTubeStrings.sponsorblockTitle)
                        .setMessage(YouTubeStrings.sponsorblockAboutMessage)
                        .setPositiveButton(YouTubeStrings.sponsorblockOk, null)
                        .show()
                }
            )
        }
    AlertDialog.Builder(context).setTitle(YouTubeStrings.sponsorblockTitle).setActions(items).show()
}
