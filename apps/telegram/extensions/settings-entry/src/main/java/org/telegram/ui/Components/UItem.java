// SPDX-FileCopyrightText: 2026 AunAli K. <hello@auna.li>
// SPDX-License-Identifier: GPL-3.0-or-later

package org.telegram.ui.Components;

import android.view.View;

public class UItem {
    public int id;
    public View view;
    public Object object;
    public static UItem asCustom(int id, View view) { return null; }
}
