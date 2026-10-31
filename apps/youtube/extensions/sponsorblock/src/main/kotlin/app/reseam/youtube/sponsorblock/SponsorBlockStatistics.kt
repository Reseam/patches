// SPDX-FileCopyrightText: 2026 Reseam contributors <https://reseam.app>
// SPDX-License-Identifier: AGPL-3.0-or-later

package app.reseam.youtube.sponsorblock

import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.view.View
import app.reseam.youtube.core.showToast
import app.reseam.youtube.settings.YouTubeStrings
import org.json.JSONObject

internal fun showSponsorBlockStats(context: Context, overlay: View) {
    AlertDialog.Builder(context)
        .setTitle(YouTubeStrings.sponsorblockStatisticsTitle)
        .setMessage(
            YouTubeStrings.sponsorblockLocalStatistics(
                storedSponsorBlockLong("you_tube_settings.sb_local_skip_count").toString(),
                formatSponsorBlockDuration(
                    storedSponsorBlockLong("you_tube_settings.sb_local_time_saved") / 1000
                ),
            )
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
                        YouTubeStrings.sponsorblockContributionStatistics(
                            info.optString("userName"),
                            info.optInt("segmentCount"),
                            info.optInt("viewCount"),
                            info.optDouble("minutesSaved").toString(),
                        )
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
