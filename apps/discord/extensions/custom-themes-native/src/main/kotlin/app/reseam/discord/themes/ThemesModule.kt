// SPDX-FileCopyrightText: 2026 Reseam contributors <https://reseam.app>
// SPDX-License-Identifier: AGPL-3.0-or-later

package app.reseam.discord.themes

import android.content.ClipData
import android.content.Intent
import androidx.core.content.FileProvider
import com.facebook.react.bridge.ReactApplicationContext
import com.facebook.react.bridge.ReactContextBaseJavaModule
import com.facebook.react.bridge.ReactMethod
import com.facebook.react.module.annotations.ReactModule
import java.io.File
import java.io.IOException
import org.json.JSONException

internal const val THEMES_MODULE_NAME = "ReseamDiscordThemes"
private const val MIME_TYPE = "application/json"

@ReactModule(name = THEMES_MODULE_NAME)
class ThemesModule(context: ReactApplicationContext) : ReactContextBaseJavaModule(context) {
    override fun getName(): String = THEMES_MODULE_NAME

    @ReactMethod(isBlockingSynchronousMethod = true)
    @Throws(IOException::class)
    fun readThemes(): String = reactApplicationContext.themeFile("themes.json").readAtomically()

    @ReactMethod
    @Throws(IOException::class)
    fun writeThemes(json: String) =
        reactApplicationContext.themeFile("themes.json").writeAtomically(json)

    @ReactMethod
    @Throws(IOException::class, JSONException::class)
    fun applyNativeColors(json: String) = applyNativePalette(reactApplicationContext, json)

    @ReactMethod
    @Throws(IOException::class)
    fun share(fileName: String, json: String, title: String?) {
        val activity = getCurrentActivity() ?: return
        val directory = activity.themeFile("shared")
        directory.listFiles()?.forEach { it.delete() }
        val file = File(directory, fileName)
        file.writeAtomically(json)
        val uri =
            FileProvider.getUriForFile(activity, "${activity.packageName}.file-provider", file)
        val send =
            Intent(Intent.ACTION_SEND)
                .setType(MIME_TYPE)
                .putExtra(Intent.EXTRA_STREAM, uri)
                .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                .apply { clipData = ClipData.newRawUri(fileName, uri) }
        activity.runOnUiThread { activity.startActivity(Intent.createChooser(send, title)) }
    }
}
