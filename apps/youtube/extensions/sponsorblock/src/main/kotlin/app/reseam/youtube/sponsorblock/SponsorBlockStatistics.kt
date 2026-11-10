// SPDX-FileCopyrightText: 2026 Reseam contributors <https://reseam.app>
// SPDX-License-Identifier: AGPL-3.0-or-later

package app.reseam.youtube.sponsorblock

import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.view.View
import app.reseam.youtube.core.showToast
import app.reseam.youtube.settings.YouTubeStrings
import app.reseam.youtube.sponsorblock.common.executeSponsorBlockRequest
import app.reseam.youtube.sponsorblock.common.requestSponsorBlock
import app.reseam.youtube.sponsorblock.common.storedSponsorBlockLong
import org.json.JSONObject

internal fun showSponsorBlockStats(context: Context, overlay: View) {
    AlertDialog.Builder(context)
        .setTitle(YouTubeStrings.sponsorblockStatisticsTitle)
        .setMessage(
            listOf(
                    YouTubeStrings.sponsorblockSegmentsSkipped(
                        storedSponsorBlockLong("you_tube_settings.sb_local_skip_count").toInt()
                    ),
                    YouTubeStrings.sponsorblockTimeSaved(
                        formatSponsorBlockDuration(
                            storedSponsorBlockLong("you_tube_settings.sb_local_time_saved") / 1000
                        )
                    ),
                )
                .joinToString("\n")
        )
        .setPositiveButton(YouTubeStrings.sponsorblockOk, null)
        .setNeutralButton(YouTubeStrings.sponsorblockMyContributions) { _, _ ->
            executeSponsorBlockRequest {
                try {
                    val info =
                        JSONObject(
                            requestSponsorBlock(
                                "GET",
                                "/api/userInfo",
                                "userID" to sponsorBlockUserId(),
                                "values" to
                                    "[\"userName\",\"segmentCount\",\"viewCount\",\"minutesSaved\"]",
                            )
                        )
                    val message =
                        listOf(
                                info.optString("userName"),
                                YouTubeStrings.sponsorblockSubmittedSegments(info.optInt("segmentCount")),
                                YouTubeStrings.sponsorblockViewerSkips(info.optInt("viewCount")),
                                YouTubeStrings.sponsorblockMinutesSaved(info.optDouble("minutesSaved").toString()),
                            )
                            .joinToString("\n")
                    overlay.post {
                        if (!(context as Activity).isDestroyed) {
                            AlertDialog.Builder(context)
                                .setTitle(YouTubeStrings.sponsorblockMyContributions)
                                .setMessage(message)
                                .setPositiveButton(YouTubeStrings.sponsorblockOk, null)
                                .show()
                        }
                    }
                } catch (_: Exception) {
                    showToast(YouTubeStrings.sponsorblockContributionStatisticsFailed)
                }
            }
        }
        .show()
}
