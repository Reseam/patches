// SPDX-FileCopyrightText: 2026 Reseam contributors <https://reseam.app>
// SPDX-License-Identifier: AGPL-3.0-or-later

@file:JvmName("CommentCopy")

package app.reseam.instagram.comments

import android.app.Application
import android.content.ClipData
import android.content.ClipboardManager
import android.os.Build
import android.widget.Toast
import app.reseam.bundle.showToast
import app.reseam.instagram.settings.InstagramStrings
import app.reseam.runtime.Export

private lateinit var app: Application

@Export
fun init(application: Application) {
    app = application
}

@Export
fun copy(text: String?) {
    app.getSystemService(ClipboardManager::class.java).setPrimaryClip(ClipData.newPlainText(null, text.orEmpty()))
    // Android 13 and later confirm clipboard writes themselves.
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) app.showToast(InstagramStrings.commentCopied, Toast.LENGTH_SHORT)
}
