// SPDX-FileCopyrightText: 2026 AunAli K. <hello@auna.li>
// SPDX-License-Identifier: GPL-3.0-or-later

package org.telegram.ui.Cells;

import android.content.Context;
import android.widget.FrameLayout;

public class TextCheckCell extends FrameLayout {
    public TextCheckCell(Context context) { super(context); }
    public void setTextAndCheck(CharSequence text, boolean checked, boolean divider) {}
    public void setTextAndValueAndCheck(String text, String value, boolean checked, boolean multiline, boolean divider) {}
    public void setChecked(boolean checked) {}
    public boolean isChecked() { return false; }
}
