// SPDX-FileCopyrightText: 2026 Reseam contributors <https://reseam.app>
// SPDX-License-Identifier: AGPL-3.0-or-later

package app.reseam.ailabels

import android.app.Activity
import android.content.Context
import app.reseam.ailabels.settings.AiLabelsStrings
import app.reseam.bundle.dpToPx

/** An image's label, laid over the image. It checks only images of posts with a text label. */
class ImageLabelView(context: Context) : LabelView(context) {
    private var url: String? = null

    init {
        val margin = resources.dpToPx(MARGIN_DP)
        setPadding(margin, margin, margin, margin)
    }

    fun bind(url: String?) {
        if (url == this.url) return
        this.url = url
        render()
    }

    override fun look(): Look? {
        val state = state() ?: return null
        val label = (state as? CheckState.Done)?.verdict?.label
        val text = when (label) {
            null -> state.waitingText()
            Label.Human -> AiLabelsStrings.aiLabelNotAi
            else -> AiLabelsStrings.aiLabelAi
        }
        return Look(label, text, SCRIM, INK)
    }

    override fun onTap(activity: Activity) {
        val image = image() ?: return
        val detector = detector ?: return
        when (val state = detector.images.state(image)) {
            is CheckState.Done -> showImageVerdict(activity, detector, image, state.verdict)
            CheckState.SignedOut -> detector.openAccount(activity)
            CheckState.NeedsConsent -> detector.requestImageConsent(activity)
            CheckState.Failed -> detector.images.retry(image)
            else -> Unit
        }
    }

    private fun state(): CheckState<ImageVerdict>? = image()?.let { imageChecks()?.state(it) }

    private fun image(): PostImage? = url?.let { PostImages[it] }

    private companion object {
        const val MARGIN_DP = 8
        const val SCRIM = 0xB3000000.toInt()
        const val INK = 0xFFFFFFFF.toInt()
    }
}
