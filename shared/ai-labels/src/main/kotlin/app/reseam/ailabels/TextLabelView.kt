// SPDX-FileCopyrightText: 2026 Reseam contributors <https://reseam.app>
// SPDX-License-Identifier: AGPL-3.0-or-later

package app.reseam.ailabels

import android.app.Activity
import android.content.Context
import app.reseam.bundle.dpToPx

/** A post's text label, for the post header. */
class TextLabelView(context: Context) : LabelView(context) {
    private var post: Post? = null

    init {
        setPaddingRelative(resources.dpToPx(GAP_DP), 0, 0, 0)
    }

    /** Null clears the label. */
    fun bind(post: Post?) {
        post?.images?.forEach(PostImages::add)
        if (post?.id == this.post?.id && post?.text == this.post?.text) return
        this.post = post
        render()
    }

    override fun look(): Look? {
        val state = state() ?: return null
        val label = (state as? CheckState.Done)?.verdict?.label
        val tone = label?.let(Tone::of) ?: Tone.Unsure
        return Look(label, label?.text ?: state.waitingText(), tone.fill(resources), tone.ink(resources))
    }

    override fun onTap(activity: Activity) {
        val post = post ?: return
        val detector = detector ?: return
        when (val state = detector.text.state(post)) {
            is CheckState.Done -> showTextVerdict(activity, detector, post, state.verdict)
            CheckState.SignedOut -> detector.openAccount(activity)
            CheckState.Failed -> detector.text.retry(post)
            else -> Unit
        }
    }

    private fun state(): CheckState<TextVerdict>? = post?.let { textChecks(it)?.state(it) }

    private companion object {
        const val GAP_DP = 4
    }
}
