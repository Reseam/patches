// SPDX-FileCopyrightText: 2026 Reseam contributors <https://reseam.app>
// SPDX-FileCopyrightText: 2026 ReVanced contributors
// SPDX-License-Identifier: GPL-3.0-or-later

package app.reseam.youtube.controls

import android.graphics.drawable.AnimatedVectorDrawable
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import app.reseam.bundle.Logger
import app.reseam.bundle.identifier
import app.reseam.bundle.isMainThread
import java.lang.ref.WeakReference

/** Null when the patch did not add the control's views, which is logged. */
fun playerControlButton(
    root: View,
    buttonId: String,
    containerId: String = buttonId,
    textId: String? = null,
    enabled: () -> Boolean,
    longClick: ((View) -> Unit)? = null,
    click: (View) -> Unit,
): PlayerControlButton? = runCatching {
    PlayerControlButton(root, buttonId, containerId, textId, enabled, longClick, click)
}
    .onFailure { Logger.error(it) { "Player control $buttonId failed to initialize" } }
    .getOrNull()

class PlayerControlButton
internal constructor(
    root: View,
    buttonId: String,
    containerId: String,
    textId: String?,
    private val enabled: () -> Boolean,
    longClick: ((View) -> Unit)?,
    click: (View) -> Unit,
) {
    private val container = WeakReference(root.find(containerId))
    private val button = WeakReference(root.find(buttonId))
    private val textOverlay = WeakReference(textId?.let { root.find(it) as TextView })

    internal val alignmentView: View?
        get() = button.get()

    init {
        container.get()?.visibility = View.GONE
        button.get()?.run {
            setOnClickListener {
                animateIcon()
                click(it)
            }
            if (longClick != null)
                setOnLongClickListener {
                    animateIcon()
                    longClick(it)
                    true
                }
        }
        PlayerControls.register(this)
    }

    private fun animateIcon() {
        ((button.get() as? ImageView)?.drawable as? AnimatedVectorDrawable)?.start()
    }

    fun setVisibility(shouldShow: Boolean) {
        val view = container.get() ?: return
        view.animate().cancel()
        if (shouldShow && enabled()) {
            view.visibility = View.VISIBLE
            view.alpha = 1f
        } else view.visibility = View.GONE
    }

    fun setIcon(resourceId: Int) {
        (button.get() as? ImageView)?.setImageResource(resourceId)
    }

    fun setTextOverlay(text: CharSequence?) {
        val view = textOverlay.get() ?: return
        if (isMainThread()) view.text = text else view.post { view.text = text }
    }
}

private fun View.find(name: String): View {
    val id = context.identifier(name, "id")
    check(id != 0) { "Missing player control resource: $name" }
    return findViewById(id) ?: error("Missing player control view: $name")
}
