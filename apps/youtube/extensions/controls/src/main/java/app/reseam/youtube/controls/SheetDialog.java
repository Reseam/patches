// SPDX-FileCopyrightText: 2026 AunAli K. <hello@auna.li>
// SPDX-FileCopyrightText: 2026 ReVanced contributors
// SPDX-License-Identifier: GPL-3.0-or-later

package app.reseam.youtube.controls;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.annotation.SuppressLint;
import android.app.Dialog;
import android.content.Context;
import android.graphics.drawable.ShapeDrawable;
import android.graphics.drawable.shapes.RoundRectShape;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.view.animation.DecelerateInterpolator;
import android.widget.LinearLayout;
import android.widget.Scroller;

/** A bottom sheet that slides in, and closes when dragged down or tapped outside. */
public final class SheetDialog extends Dialog {
    private final int animationDuration;
    private final int screenHeight;
    private View animatedView;
    private boolean dismissing;

    private SheetDialog(Context context, int animationDuration) {
        super(context);
        this.animationDuration = animationDuration;
        screenHeight = context.getResources().getDisplayMetrics().heightPixels;
    }

    /** A rounded, themed column with a drag handle, which the caller fills and passes to {@link #create}. */
    public static LinearLayout createContent(Context context) {
        LinearLayout content = new LinearLayout(context);
        content.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        params.setMargins(Ui.dp(8), 0, Ui.dp(8), Ui.dp(8));
        content.setLayoutParams(params);

        int color = Ui.dialogBackgroundColor(context);
        ShapeDrawable background = new ShapeDrawable(new RoundRectShape(Ui.roundedCorners(12), null, null));
        background.getPaint().setColor(color);
        content.setBackground(background);

        LinearLayout handleContainer = new LinearLayout(context);
        LinearLayout.LayoutParams containerParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        containerParams.setMargins(0, Ui.dp(8), 0, 0);
        handleContainer.setLayoutParams(containerParams);
        handleContainer.setGravity(Gravity.CENTER_HORIZONTAL | Gravity.BOTTOM);
        View handle = new View(context);
        ShapeDrawable handleBackground = new ShapeDrawable(new RoundRectShape(Ui.roundedCorners(4), null, null));
        handleBackground.getPaint().setColor(Ui.adjustBrightness(context, color, 0.9f, 1.25f));
        handle.setLayoutParams(new LinearLayout.LayoutParams(Ui.dp(40), Ui.dp(4)));
        handle.setBackground(handleBackground);
        handleContainer.addView(handle);
        content.addView(handleContainer);
        return content;
    }

    public static SheetDialog create(Context context, View content) {
        int duration = Ui.fadeInDuration();
        SheetDialog dialog = new SheetDialog(context, duration);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setCanceledOnTouchOutside(true);
        dialog.setCancelable(true);

        LinearLayout wrapper = new LinearLayout(context);
        wrapper.setOrientation(LinearLayout.VERTICAL);
        DragLayout drag = new DragLayout(context, dialog, duration);
        drag.setOrientation(LinearLayout.VERTICAL);
        // The empty band above the sheet takes touches, so a tap there is not a tap outside.
        View spacer = new View(context);
        spacer.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, Ui.dp(40)));
        spacer.setClickable(true);
        drag.addView(spacer);
        if (content.getParent() instanceof ViewGroup) ((ViewGroup) content.getParent()).removeView(content);
        drag.addView(content);
        wrapper.addView(drag);
        dialog.setContentView(wrapper);

        Window window = dialog.getWindow();
        if (window != null) {
            WindowManager.LayoutParams params = window.getAttributes();
            params.width = Ui.portraitWidth(100);
            params.height = WindowManager.LayoutParams.WRAP_CONTENT;
            params.gravity = Gravity.BOTTOM;
            params.y = 0;
            window.setAttributes(params);
            window.setBackgroundDrawable(null);
        }
        dialog.animatedView = drag;
        return dialog;
    }

    @Override
    public void show() {
        super.show();
        animatedView.setTranslationY(screenHeight);
        animatedView.animate().translationY(0).setDuration(animationDuration).setListener(null).start();
    }

    @Override
    public void cancel() {
        dismiss();
    }

    @Override
    public void dismiss() {
        if (dismissing) return;
        dismissing = true;
        Window window = getWindow();
        if (window == null) {
            super.dismiss();
            dismissing = false;
            return;
        }
        WindowManager.LayoutParams params = window.getAttributes();
        ValueAnimator dim = ValueAnimator.ofFloat(params.dimAmount, 0f);
        dim.setDuration(animationDuration);
        dim.addUpdateListener(animation -> {
            params.dimAmount = (float) animation.getAnimatedValue();
            window.setAttributes(params);
        });
        dim.start();
        animatedView.animate().translationY(screenHeight).setDuration(animationDuration)
                .setListener(new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(Animator animation) {
                        SheetDialog.super.dismiss();
                        dismissing = false;
                    }
                })
                .start();
    }

    /** Follows a downward drag and dismisses past half its height or on a fling. */
    private static final class DragLayout extends LinearLayout {
        private static final int MIN_FLING_VELOCITY = 800;
        private static final float DISMISS_HEIGHT_FRACTION = 0.5f;

        private final SheetDialog dialog;
        private final int animationDuration;
        private final Scroller scroller;
        private VelocityTracker velocityTracker = VelocityTracker.obtain();
        private final Runnable settle = this::settle;
        private float initialTouchY;
        private float dragOffset;
        private float dismissThreshold;
        private boolean dragging;
        private boolean dragEnabled;

        DragLayout(Context context, SheetDialog dialog, int animationDuration) {
            super(context);
            this.dialog = dialog;
            this.animationDuration = animationDuration;
            scroller = new Scroller(context, new DecelerateInterpolator());
            setClickable(true);
            // Dragging during the slide-in would fight the animation.
            postDelayed(() -> dragEnabled = true, animationDuration + 50);
        }

        @Override
        protected void onSizeChanged(int w, int h, int oldw, int oldh) {
            super.onSizeChanged(w, h, oldw, oldh);
            dismissThreshold = h * DISMISS_HEIGHT_FRACTION;
        }

        @Override
        public boolean onInterceptTouchEvent(MotionEvent event) {
            if (!dragEnabled) return false;
            switch (event.getActionMasked()) {
                case MotionEvent.ACTION_DOWN:
                    initialTouchY = event.getRawY();
                    dragging = false;
                    scroller.forceFinished(true);
                    removeCallbacks(settle);
                    velocityTracker.clear();
                    velocityTracker.addMovement(event);
                    dragOffset = getTranslationY();
                    break;
                case MotionEvent.ACTION_MOVE:
                    float dy = event.getRawY() - initialTouchY;
                    if (dy > ViewConfiguration.get(getContext()).getScaledTouchSlop() && !canChildScrollUp(this)) {
                        dragging = true;
                        return true;
                    }
                    break;
            }
            return false;
        }

        @SuppressLint("ClickableViewAccessibility")
        @Override
        public boolean onTouchEvent(MotionEvent event) {
            if (!dragEnabled) return super.onTouchEvent(event);
            velocityTracker.addMovement(event);
            switch (event.getActionMasked()) {
                case MotionEvent.ACTION_MOVE:
                    if (dragging) {
                        dragOffset = Math.max(0, event.getRawY() - initialTouchY);
                        setTranslationY(dragOffset);
                    }
                    return true;
                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_CANCEL:
                    velocityTracker.computeCurrentVelocity(1000);
                    int target = event.getActionMasked() != MotionEvent.ACTION_CANCEL
                            && (dragOffset > dismissThreshold || velocityTracker.getYVelocity() > MIN_FLING_VELOCITY)
                            ? getHeight()
                            : 0;
                    scroller.startScroll(0, (int) dragOffset, 0, target - (int) dragOffset, animationDuration);
                    postOnAnimation(settle);
                    dragging = false;
                    return true;
            }
            // Consumed, so a touch on the sheet never moves focus between its children.
            return true;
        }

        @Override
        protected void onAttachedToWindow() {
            super.onAttachedToWindow();
            if (velocityTracker == null) velocityTracker = VelocityTracker.obtain();
        }

        @Override
        protected void onDetachedFromWindow() {
            removeCallbacks(settle);
            scroller.forceFinished(true);
            velocityTracker.recycle();
            velocityTracker = null;
            super.onDetachedFromWindow();
        }

        private void settle() {
            if (!scroller.computeScrollOffset()) {
                dragOffset = getTranslationY();
                return;
            }
            dragOffset = scroller.getCurrY();
            setTranslationY(dragOffset);
            if (dragOffset >= getHeight()) {
                dialog.dismiss();
                scroller.forceFinished(true);
            } else {
                postOnAnimation(settle);
            }
        }

        private static boolean canChildScrollUp(ViewGroup group) {
            for (int i = 0; i < group.getChildCount(); i++) {
                View child = group.getChildAt(i);
                if (child.canScrollVertically(-1)) return true;
                if (child instanceof ViewGroup && canChildScrollUp((ViewGroup) child)) return true;
            }
            return false;
        }
    }
}
