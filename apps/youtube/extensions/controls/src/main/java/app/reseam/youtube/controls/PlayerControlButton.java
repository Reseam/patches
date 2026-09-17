// SPDX-FileCopyrightText: 2026 AunAli K. <hello@auna.li>
// SPDX-FileCopyrightText: 2026 ReVanced contributors
// SPDX-License-Identifier: GPL-3.0-or-later

package app.reseam.youtube.controls;

import android.graphics.drawable.AnimatedVectorDrawable;
import android.graphics.drawable.Drawable;
import android.view.View;
import android.os.Looper;
import android.widget.ImageView;
import android.widget.TextView;

import java.lang.ref.WeakReference;

import app.reseam.youtube.core.Logger;

/** Base behavior shared by every XML-grafted player button. */
public class PlayerControlButton {
    public interface PlayerControlButtonStatus {
        boolean buttonEnabled();
    }

    private final WeakReference<View> container;
    private final WeakReference<View> button;
    private final WeakReference<TextView> textOverlay;
    private final PlayerControlButtonStatus enabledStatus;
    private boolean visible;

    public PlayerControlButton(View root, String buttonId,
                               PlayerControlButtonStatus enabledStatus,
                               View.OnClickListener click,
                               View.OnLongClickListener longClick) {
        this(root, buttonId, buttonId, null, enabledStatus, click, longClick);
    }

    public PlayerControlButton(View root, String containerId, String buttonId, String textId,
                               PlayerControlButtonStatus enabledStatus,
                               View.OnClickListener click,
                               View.OnLongClickListener longClick) {
        final View containerView = find(root, containerId);
        final View buttonView = find(root, buttonId);
        containerView.setVisibility(View.GONE);
        container = new WeakReference<>(containerView);
        button = new WeakReference<>(buttonView);
        textOverlay = new WeakReference<>(textId == null ? null : (TextView) find(root, textId));
        this.enabledStatus = enabledStatus;
        buttonView.setOnClickListener(view -> {
            animateIcon();
            if (click != null) click.onClick(view);
        });
        if (longClick != null) {
            buttonView.setOnLongClickListener(view -> {
                animateIcon();
                return longClick.onLongClick(view);
            });
        }
        PlayerControls.register(this);
    }

    private static View find(View root, String name) {
        final int id = root.getResources().getIdentifier(name, "id", root.getContext().getPackageName());
        if (id == 0) throw new IllegalStateException("Missing player control resource: " + name);
        final View result = root.findViewById(id);
        if (result == null) throw new IllegalStateException("Missing player control view: " + name);
        return result;
    }

    private void animateIcon() {
        final View view = button.get();
        if (view instanceof ImageView) {
            final Drawable drawable = ((ImageView) view).getDrawable();
            if (drawable instanceof AnimatedVectorDrawable) ((AnimatedVectorDrawable) drawable).start();
        }
    }

    public void setVisibility(boolean shouldShow, boolean animated) {
        try {
            final View view = container.get();
            if (view == null) return;
            final boolean enabled = enabledStatus == null || enabledStatus.buttonEnabled();
            visible = shouldShow;
            view.animate().cancel();
            if (shouldShow && enabled) {
                view.setVisibility(View.VISIBLE);
                view.setAlpha(1.0f);
            } else {
                view.setVisibility(View.GONE);
            }
        } catch (Exception exception) {
            Logger.error(() -> "Player control visibility failure: " + exception);
        }
    }

    public void setVisibilityImmediate(boolean shouldShow) {
        setVisibility(shouldShow, false);
    }

    public void setIcon(int resourceId) {
        final View view = button.get();
        if (view instanceof ImageView) ((ImageView) view).setImageResource(resourceId);
    }

    public void setTextOverlay(CharSequence text) {
        final TextView view = textOverlay.get();
        if (view == null) return;
        if (Looper.myLooper() == Looper.getMainLooper()) view.setText(text);
        else view.post(() -> view.setText(text));
    }
}
