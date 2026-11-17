// SPDX-FileCopyrightText: 2026 Reseam contributors <https://reseam.app>
// SPDX-License-Identifier: AGPL-3.0-or-later

package app.reseam.ailabels

import android.app.Activity
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.format.DateUtils
import android.text.method.LinkMovementMethod
import android.text.style.ClickableSpan
import android.view.Gravity
import android.view.View
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.widget.FrameLayout
import app.reseam.ailabels.settings.AiLabelsStrings
import app.reseam.bundle.openUrl
import app.reseam.runtime.ui.Icons
import app.reseam.runtime.ui.Tokens

/** A post's text verdict, laid out like the detector's browser extension shows it. */
internal fun showTextVerdict(activity: Activity, detector: Detector, post: Post, verdict: TextVerdict) {
    val shortText = verdict.wordCount < detector.shortTextWords
    val subtitle = listOfNotNull(AiLabelsStrings.aiLabelWords(verdict.wordCount), AiLabelsStrings.aiLabelShortText.takeIf { shortText })
    with(Sheet(activity)) {
        add(header(verdict, subtitle.joinToString(" · ")))
        add(ui.text(verdict.summary, ui.type.bodyMedium, colors.secondaryText), top = Tokens.SPACE_LG)
        add(postCard(post, verdict.wordCount), top = Tokens.SPACE_LG)
        verdict.shares?.let {
            add(ring(verdict.label, it), top = Tokens.SPACE_XXL)
            add(legend(it), top = Tokens.SPACE_SM)
        }
        verdict.distribution?.let { add(distributionCard(it, verdict.segments), top = Tokens.SPACE_XXL) }
        if (shortText) detector.shortTextUrl?.let { add(shortTextNote(it), top = Tokens.SPACE_LG) }
        add(actions({ detector.text.share(post, verdict) }, { detector.text.fullAnalysis(post, verdict) }), top = Tokens.SPACE_XXL)
        show()
    }
}

private fun Sheet.postCard(post: Post, words: Int): View {
    val posted = post.createdAtMillis?.let { DateUtils.getRelativeTimeSpanString(it, System.currentTimeMillis(), DateUtils.MINUTE_IN_MILLIS) }
    return card(Tokens.SPACE_MD).apply {
        add(
            context.row {
                add(ui.text(listOfNotNull(post.authorName, posted).joinToString(" · "), ui.type.labelLarge, colors.text).ellipsized(1), width = 0, weight = 1f)
                add(ui.text(AiLabelsStrings.aiLabelWords(words), ui.type.labelMedium, colors.secondaryText), width = WRAP_CONTENT, start = Tokens.SPACE_SM)
            },
        )
        add(ui.text(post.text.replace(WHITESPACE, " "), ui.type.bodyMedium, colors.secondaryText).ellipsized(PREVIEW_LINES), top = Tokens.SPACE_SM)
    }
}

/** The AI, AI-assisted and human shares as one ring, with the share [label] is about in the middle. */
private fun Sheet.ring(label: Label, shares: Shares): View {
    val human = label == Label.Human
    val size = px(RING_SIZE)
    val caption = context.column {
        gravity = Gravity.CENTER
        add(glyphTile(label, MARK, MARK / 2f), width = px(MARK), height = px(MARK), bottom = Tokens.SPACE_SM)
        add(
            ui.text(percent(if (human) shares.human else shares.ai), ui.type.headlineSmall, colors.text).apply {
                textSize = PERCENT_SP
                includeFontPadding = false
                gravity = Gravity.CENTER
            },
        )
        add(
            ui.text(if (human) AiLabelsStrings.aiLabelShareHuman else AiLabelsStrings.aiLabelShareAi, ui.type.labelMedium, colors.secondaryText).apply {
                gravity = Gravity.CENTER
            },
            top = Tokens.SPACE_XS,
        )
    }
    return FrameLayout(context).apply {
        addView(ShareRing(context, shares), FrameLayout.LayoutParams(size, size, Gravity.CENTER))
        addView(caption, FrameLayout.LayoutParams((size * CAPTION_WIDTH).toInt(), WRAP_CONTENT, Gravity.CENTER))
    }
}

private fun Sheet.legend(shares: Shares): View =
    context.row {
        gravity = Gravity.CENTER
        val parts = listOf(
            Triple(AiLabelsStrings.aiLabelAi, shares.ai, ShareColors.AI),
            Triple(AiLabelsStrings.aiLabelAiAssisted, shares.assisted, ShareColors.ASSISTED),
            Triple(AiLabelsStrings.aiLabelHuman, shares.human, ShareColors.HUMAN_RING),
        )
        for ((name, share, color) in parts) {
            val dot = ui.shape(color, DOT / 2f).apply { setBounds(0, 0, px(DOT), px(DOT)) }
            add(
                ui.text("$name ${percent(share)}", ui.type.labelMedium, colors.secondaryText).apply {
                    setCompoundDrawablesRelative(dot, null, null, null)
                    compoundDrawablePadding = px(Tokens.SPACE_XS)
                },
                width = WRAP_CONTENT,
                start = Tokens.SPACE_SM,
                end = Tokens.SPACE_SM,
            )
        }
    }

/** Where the AI content sits, in the detector's words; opening it shows each stretch on a chart. */
private fun Sheet.distributionCard(distribution: Distribution, segments: List<Segment>): View {
    val details = context.column {
        visibility = View.GONE
        add(View(context).apply { setBackgroundColor(colors.border) }, height = px(Sheet.BORDER), top = Tokens.SPACE_LG)
        add(ui.text(distribution.description, ui.type.bodyMedium, colors.text), top = Tokens.SPACE_LG)
        if (segments.isNotEmpty()) {
            val levels = listOf(AiLabelsStrings.aiLabelAi, AiLabelsStrings.aiLabelAiAssisted, AiLabelsStrings.aiLabelHuman)
            val axis = AiLabelsStrings.aiLabelWords(0) to AiLabelsStrings.aiLabelWords(segments.sumOf { it.words })
            add(StepChart(context, segments, levels, axis, colors.secondaryText, colors.border), height = px(CHART_HEIGHT), top = Tokens.SPACE_LG)
        }
    }
    val arrow = ui.icon(Icons.CHEVRON_RIGHT, Tokens.ICON_MD, colors.secondaryText)
    return card(Tokens.SPACE_LG).apply {
        add(
            context.row {
                add(ui.text(distribution.title, ui.type.titleMedium, colors.text), width = 0, weight = 1f)
                add(arrow, width = px(Tokens.ICON_MD), height = px(Tokens.ICON_MD), start = Tokens.SPACE_SM)
            },
        )
        add(details)
        setOnClickListener {
            val open = details.visibility != View.VISIBLE
            details.visibility = if (open) View.VISIBLE else View.GONE
            val down = if (arrow.layoutDirection == View.LAYOUT_DIRECTION_RTL) -90f else 90f
            arrow.animate().rotation(if (open) down else 0f).setDuration(ARROW_MS).start()
        }
    }
}

private fun Sheet.shortTextNote(url: String): View {
    val link = object : ClickableSpan() {
        override fun onClick(widget: View) = widget.context.openUrl(url)
    }
    val text = SpannableStringBuilder("${AiLabelsStrings.aiLabelConfidenceLimited} · ")
        .append(AiLabelsStrings.aiLabelLearnMore, link, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
    return ui.text(text, ui.type.labelMedium, colors.secondaryText).apply {
        gravity = Gravity.CENTER
        movementMethod = LinkMovementMethod.getInstance()
        setLinkTextColor(colors.accent)
    }
}

private const val PREVIEW_LINES = 2
private const val RING_SIZE = 200
private const val MARK = 36
private const val PERCENT_SP = 36f
private const val CAPTION_WIDTH = 0.62f
private const val DOT = 8
private const val CHART_HEIGHT = 140
private const val ARROW_MS = 150L
