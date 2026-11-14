// SPDX-FileCopyrightText: 2026 Reseam contributors <https://reseam.app>
// SPDX-License-Identifier: AGPL-3.0-or-later

package app.reseam.ailabels

import android.app.Activity
import android.view.Gravity
import app.reseam.ailabels.settings.AiLabelsStrings
import app.reseam.runtime.ui.Tokens

internal fun showImageVerdict(activity: Activity, detector: Detector, image: PostImage, verdict: ImageVerdict) {
    with(Sheet(activity)) {
        add(header(verdict, "${AiLabelsStrings.aiLabelConfidence(verdict.confidence)} · ${AiLabelsStrings.aiLabelPercentAi(percent(verdict.aiLikelihood))}"))
        add(LikelihoodBar(context, verdict.aiLikelihood.toFloat(), colors.text), top = Tokens.SPACE_XXL, height = px(BAR_HEIGHT))
        val scale = listOf(
            AiLabelsStrings.aiLabelNotAi to Gravity.START,
            AiLabelsStrings.aiLabelUnsure to Gravity.CENTER_HORIZONTAL,
            AiLabelsStrings.aiLabelAi to Gravity.END,
        )
        add(
            context.row {
                for ((label, side) in scale) {
                    add(ui.text(label, ui.type.labelMedium, colors.secondaryText).apply { gravity = side }, width = 0, weight = 1f)
                }
            },
            top = Tokens.SPACE_SM,
        )
        add(actions({ detector.images.share(image, verdict) }, { detector.images.fullAnalysis(image, verdict) }), top = Tokens.SPACE_XXL)
        show()
    }
}

private const val BAR_HEIGHT = 16
