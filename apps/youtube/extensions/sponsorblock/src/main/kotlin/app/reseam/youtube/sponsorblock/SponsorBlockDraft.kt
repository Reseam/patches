// SPDX-FileCopyrightText: 2026 Reseam contributors <https://reseam.app>
// SPDX-License-Identifier: AGPL-3.0-or-later

package app.reseam.youtube.sponsorblock

import android.app.AlertDialog
import android.text.InputType
import android.widget.EditText
import android.widget.LinearLayout
import app.reseam.bundle.dpToPx
import app.reseam.youtube.core.showToast
import app.reseam.youtube.settings.YouTubeStrings
import app.reseam.youtube.video.VideoInformation
import java.util.Locale

internal class SponsorBlockDraft(private val overlay: SponsorBlockOverlay) {
    private val context
        get() = overlay.context

    private data class Draft(val videoId: String = "", val start: Long = -1, val end: Long = -1)

    private var draft = Draft()

    fun createSegment() {
        val id = SponsorBlockSession.videoId
        if (id != draft.videoId) draft = Draft(id)
        val actions =
            listOf(
                SponsorBlockAction(YouTubeStrings.sponsorblockMarkStart) {
                    draft = draft.copy(start = VideoInformation.videoTime)
                    showToast(YouTubeStrings.sponsorblockStartMarked)
                },
                SponsorBlockAction(YouTubeStrings.sponsorblockMarkEnd) {
                    draft = draft.copy(end = VideoInformation.videoTime)
                    showToast(YouTubeStrings.sponsorblockEndMarked)
                },
                SponsorBlockAction(YouTubeStrings.sponsorblockEditPreviewSubmit, ::editDraft),
                SponsorBlockAction(YouTubeStrings.sponsorblockDiscard, ::discard),
            )
        AlertDialog.Builder(context)
            .setTitle(YouTubeStrings.sponsorblockCreateSegment)
            .setActions(actions)
            .show()
    }

    private fun discard() {
        draft = draft.copy(start = -1, end = -1)
    }

    private fun editDraft() {
        val fields =
            LinearLayout(context).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(resources.dpToPx(20f), 0, resources.dpToPx(20f), 0)
            }
        val from = timeField(YouTubeStrings.sponsorblockStartSeconds, draft.start, fields)
        val to = timeField(YouTubeStrings.sponsorblockEndSeconds, draft.end, fields)
        val dialog =
            AlertDialog.Builder(context)
                .setTitle(YouTubeStrings.sponsorblockSegmentTimes)
                .setView(fields)
                .setNegativeButton(YouTubeStrings.sponsorblockCancel, null)
                .setNeutralButton(YouTubeStrings.sponsorblockPreview, null)
                .setPositiveButton(YouTubeStrings.sponsorblockChooseCategory, null)
                .create()
        dialog.setOnShowListener {
            dialog.getButton(AlertDialog.BUTTON_NEUTRAL).setOnClickListener {
                if (readDraft(from, to)) VideoInformation.seekTo(maxOf(0, draft.start - 2000))
            }
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                if (readDraft(from, to)) {
                    dialog.dismiss()
                    chooseSponsorBlockCategory(context, ::confirmSubmission)
                }
            }
        }
        dialog.show()
    }

    private fun confirmSubmission(category: Segment.Category) {
        val start = draft.start
        val end = if (category == Segment.Category.HIGHLIGHT) draft.start else draft.end
        val id = draft.videoId
        val duration = VideoInformation.videoLength
        if (id != SponsorBlockSession.videoId) return
        if (end == start && category != Segment.Category.HIGHLIGHT) {
            showToast(YouTubeStrings.sponsorblockIdenticalTimes)
            return
        }
        AlertDialog.Builder(context)
            .setTitle(YouTubeStrings.sponsorblockSubmitCategory(category.title))
            .setMessage(
                YouTubeStrings.sponsorblockSubmissionMessage(
                    String.format(Locale.ROOT, "%.3f", start / 1000.0),
                    String.format(Locale.ROOT, "%.3f", end / 1000.0),
                )
            )
            .setNegativeButton(YouTubeStrings.sponsorblockCancel, null)
            .setPositiveButton(YouTubeStrings.sponsorblockSubmit) { _, _ ->
                executeSponsorBlockRequest {
                    try {
                        requestSponsorBlock(
                            "POST",
                            "/api/skipSegments",
                            "userID" to sponsorBlockUserId(),
                            "videoID" to id,
                            "category" to category.key,
                            "startTime" to (start / 1000.0).toString(),
                            "endTime" to (end / 1000.0).toString(),
                            "actionType" to
                                if (category == Segment.Category.HIGHLIGHT) "poi" else "skip",
                            "videoDuration" to (duration / 1000.0).toString(),
                        )
                        overlay.post {
                            if (id == draft.videoId && draft.start == start) discard()
                        }
                        showToast(YouTubeStrings.sponsorblockSegmentSubmitted)
                    } catch (error: Exception) {
                        showToast(
                            YouTubeStrings.sponsorblockSubmissionFailed(error.message.toString())
                        )
                    }
                }
            }
            .show()
    }

    private fun timeField(label: String, time: Long, parent: LinearLayout): EditText =
        EditText(context).apply {
            hint = label
            inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL
            if (time >= 0) setText(String.format(Locale.ROOT, "%.3f", time / 1000.0))
            parent.addView(this)
        }

    private fun readDraft(from: EditText, to: EditText): Boolean {
        val start = from.text.toString().toDoubleOrNull()
        val end = if (start == null) null else to.text.toString().toDoubleOrNull()
        return when {
            start == null || end == null -> false
            !start.isFinite() || !end.isFinite() -> false
            start < 0 || end < start -> false
            end * 1000 > VideoInformation.videoLength -> false
            draft.videoId != SponsorBlockSession.videoId -> false
            else -> {
                draft = draft.copy(start = Math.round(start * 1000), end = Math.round(end * 1000))
                true
            }
        }.also { valid -> if (!valid) showToast(YouTubeStrings.sponsorblockInvalidTimes) }
    }
}
