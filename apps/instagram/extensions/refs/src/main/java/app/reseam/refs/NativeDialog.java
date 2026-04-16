// SPDX-FileCopyrightText: 2026 AunAli K. <hello@auna.li>
// SPDX-License-Identifier: GPL-3.0-or-later


package app.reseam.instagram.refs;

import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;

/**
 * Wraps Instagram's own dialog builder so extension code can present
 * natively-styled modals. The constructor and every setter is a bridge whose
 * body is rewritten at patch time by {@code DialogRefs.kt} to forward into
 * Instagram's builder class.
 */
public final class NativeDialog {

    @SuppressWarnings("unused")
    private Object handle;

    public NativeDialog(Context ctx) {}

    public NativeDialog setTitle(CharSequence title) { return this; }

    public NativeDialog setMessage(CharSequence message) { return this; }

    public NativeDialog setPositiveButton(CharSequence text, DialogInterface.OnClickListener l) { return this; }

    public NativeDialog setNegativeButton(CharSequence text, DialogInterface.OnClickListener l) { return this; }

    public NativeDialog setCancelable(boolean cancelable) { return this; }

    public NativeDialog setCanceledOnTouchOutside(boolean cancel) { return this; }

    public NativeDialog setOnDismissListener(DialogInterface.OnDismissListener l) { return this; }

    public NativeDialog addItems(CharSequence[] items, DialogInterface.OnClickListener l) { return this; }

    public Dialog getDialog() { return null; }

    public void show() {}
}
