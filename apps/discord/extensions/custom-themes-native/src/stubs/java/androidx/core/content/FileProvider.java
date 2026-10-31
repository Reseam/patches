// SPDX-FileCopyrightText: 2026 Reseam contributors <https://reseam.app>
// SPDX-License-Identifier: AGPL-3.0-or-later

// Compile-only stub; Discord ships the real class and registers it in its manifest.
package androidx.core.content;

import android.content.Context;
import android.net.Uri;

import java.io.File;

public class FileProvider {
    public static Uri getUriForFile(Context context, String authority, File file) { return null; }
}
