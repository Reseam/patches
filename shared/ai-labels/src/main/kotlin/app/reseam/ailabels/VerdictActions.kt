// SPDX-FileCopyrightText: 2026 Reseam contributors <https://reseam.app>
// SPDX-License-Identifier: AGPL-3.0-or-later

package app.reseam.ailabels

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.view.Gravity
import android.view.View
import app.reseam.ailabels.settings.AiLabelsStrings
import app.reseam.bundle.Logger
import app.reseam.bundle.openUrl
import app.reseam.bundle.postToMainThread
import app.reseam.bundle.showToast
import app.reseam.runtime.ui.Tokens
import java.util.concurrent.Executors

private val actionThread = Executors.newSingleThreadExecutor()

/** Share result and Full analysis side by side, as the detector's extension shows them. */
internal fun Sheet.actions(share: () -> String, fullAnalysis: () -> String): View = VerdictActions(this, share, fullAnalysis).view

// Either action can take a few seconds at the detector, so a line under the buttons says what is happening.
private class VerdictActions(private val sheet: Sheet, private val share: () -> String, private val fullAnalysis: () -> String) {
    private val context = sheet.context
    private val status = sheet.ui.text(null, sheet.ui.type.labelMedium, sheet.colors.secondaryText).apply {
        gravity = Gravity.CENTER
        visibility = View.GONE
    }
    private val shareButton = sheet.outlinedButton(AiLabelsStrings.aiLabelShareResult) {
        run(AiLabelsStrings.aiLabelPreparingShare, AiLabelsStrings.aiLabelShareFailed, share, ::copy)
    }
    private val analysisButton = sheet.filledButton(AiLabelsStrings.aiLabelFullAnalysis) {
        run(AiLabelsStrings.aiLabelOpeningAnalysis, AiLabelsStrings.aiLabelAnalysisFailed, fullAnalysis, ::open)
    }

    val view: View = context.column {
        add(
            context.row {
                add(shareButton, width = 0, weight = 1f, end = Tokens.SPACE_XS)
                add(analysisButton, width = 0, weight = 1f, start = Tokens.SPACE_XS)
            },
        )
        add(status, top = Tokens.SPACE_SM)
    }

    private fun run(progress: String, failure: String, action: () -> String, done: (String) -> Unit) {
        setBusy(true)
        status.text = progress
        status.visibility = View.VISIBLE
        actionThread.execute {
            val link = runCatching(action).onFailure { Logger.warn(it) { "AI label action failed" } }.getOrNull()
            postToMainThread {
                setBusy(false)
                if (link == null) {
                    status.text = failure
                } else {
                    status.visibility = View.GONE
                    done(link)
                }
            }
        }
    }

    private fun setBusy(busy: Boolean) {
        for (button in listOf(shareButton, analysisButton)) {
            button.isEnabled = !busy
            button.alpha = if (busy) Tokens.DISABLED else 1f
        }
    }

    private fun copy(link: String) {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        clipboard.setPrimaryClip(ClipData.newPlainText(AiLabelsStrings.aiLabelShareResult, link))
        context.showToast(AiLabelsStrings.aiLabelLinkCopied)
    }

    private fun open(link: String) {
        context.openUrl(link)
        sheet.dismiss()
    }
}
