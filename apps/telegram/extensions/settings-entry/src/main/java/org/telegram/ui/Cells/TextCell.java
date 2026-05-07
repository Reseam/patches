// SPDX-FileCopyrightText: 2026 AunAli K. <hello@auna.li>
// SPDX-License-Identifier: GPL-3.0-or-later

package org.telegram.ui.Cells;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.widget.FrameLayout;

public class TextCell extends FrameLayout {
    public TextCell(Context context) { super(context); }
    public void setText(CharSequence text, boolean divider) {}
    public void setTextAndIcon(CharSequence text, Drawable drawable, boolean divider) {}
    public void setTextAndIcon(CharSequence text, int resId, boolean divider) {}
}
