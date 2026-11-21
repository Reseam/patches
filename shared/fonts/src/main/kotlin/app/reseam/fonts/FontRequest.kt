// SPDX-FileCopyrightText: 2026 Reseam contributors <https://reseam.app>
// SPDX-License-Identifier: AGPL-3.0-or-later

package app.reseam.fonts

import android.app.Activity
import android.app.Fragment
import android.content.Context
import android.content.Intent
import android.graphics.fonts.Font
import android.net.Uri
import android.widget.Toast
import app.reseam.bundle.showToast
import app.reseam.fonts.settings.FontSettings
import app.reseam.fonts.settings.FontStrings
import java.io.File
import java.io.IOException
import kotlin.concurrent.thread

/** Receives the font file the user picks; a framework fragment gets the result without the activity forwarding it. */
@Suppress("DEPRECATION")
class FontRequest : Fragment() {
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        fragmentManager.beginTransaction().remove(this).commit()
        val uri = data?.data
        if (requestCode != REQUEST_CODE || resultCode != Activity.RESULT_OK || uri == null) return
        val context = activity.applicationContext
        thread { store(context, uri) }
    }

    internal companion object {
        private const val TAG = "ReseamFontRequest"
        private const val REQUEST_CODE = 0x57C5

        fun start(activity: Activity) {
            val request = FontRequest()
            activity.fragmentManager.beginTransaction().add(request, TAG).commitNow()
            val intent = Intent(Intent.ACTION_OPEN_DOCUMENT)
                .addCategory(Intent.CATEGORY_OPENABLE)
                .setType("*/*")
                .putExtra(Intent.EXTRA_MIME_TYPES, FONT_TYPES)
            request.startActivityForResult(intent, REQUEST_CODE)
        }
    }
}

// Pickers report font files under several types, and some only as generic binary data.
private val FONT_TYPES = arrayOf("font/*", "application/x-font-ttf", "application/x-font-otf", "application/font-sfnt", "application/octet-stream")

private fun store(context: Context, uri: Uri) {
    val font = fontFile().also { it.parentFile!!.mkdirs() }
    val picked = File(font.parentFile, "font.picked")
    context.contentResolver.openInputStream(uri)!!.use { input -> picked.outputStream().use(input::copyTo) }
    if (!isFont(picked)) {
        picked.delete()
        context.showToast(FontStrings.fontFileUnreadable, Toast.LENGTH_SHORT)
        return
    }
    picked.renameTo(font)
    FontSettings.font.value = "custom"
    fontChanged()
}

private fun isFont(file: File): Boolean =
    try {
        Font.Builder(file).build()
        true
    } catch (_: IOException) {
        false
    } catch (_: IllegalArgumentException) {
        false
    }
