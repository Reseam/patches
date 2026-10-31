// SPDX-FileCopyrightText: 2026 Reseam contributors <https://reseam.app>
// SPDX-License-Identifier: AGPL-3.0-or-later

package com.bytedance.tux.icon;

import android.content.Context;
import android.widget.ImageView;

public class TuxIconView extends ImageView {
    public TuxIconView(Context context) {
        super(context);
    }

    public final native void setIconRes(int icon);

    public final native void setTintColor(int color);

    public final native void setIconWidth(int width);

    public final native void setIconHeight(int height);
}
