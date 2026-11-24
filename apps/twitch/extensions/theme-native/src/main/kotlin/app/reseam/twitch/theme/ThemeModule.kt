// SPDX-FileCopyrightText: 2026 Reseam contributors <https://reseam.app>
// SPDX-License-Identifier: AGPL-3.0-or-later

package app.reseam.twitch.theme

import app.reseam.bundle.identifier
import app.reseam.runtime.Export
import com.facebook.react.bridge.NativeModule
import com.facebook.react.bridge.ReactApplicationContext
import com.facebook.react.bridge.ReactContextBaseJavaModule
import com.facebook.react.bridge.ReactMethod
import com.facebook.react.module.annotations.ReactModule
import org.json.JSONObject

private const val THEME_MODULE_NAME = "ReseamTwitchTheme"

// Twitch's R8 build renames this annotation, so the app finds the module through getName. The build
// still reads it to declare the module for the JavaScript extension.
@ReactModule(name = THEME_MODULE_NAME)
class ThemeModule(context: ReactApplicationContext) : ReactContextBaseJavaModule(context) {
    override fun getName(): String = THEME_MODULE_NAME

    /** Returns a JSON object of the app's colors among the comma-separated [names], each as `#rrggbb`, or `#rrggbbaa` when it is translucent. */
    @ReactMethod(isBlockingSynchronousMethod = true)
    fun colors(names: String): String {
        val colors = JSONObject()
        for (name in names.split(',')) {
            val id = reactApplicationContext.identifier(name, "color")
            if (id != 0) colors.put(name, hex(reactApplicationContext.getColor(id)))
        }
        return colors.toString()
    }

    companion object {
        @Export
        @JvmStatic
        fun withThemeModule(modules: List<NativeModule>?, context: ReactApplicationContext): List<NativeModule> =
            modules.orEmpty() + ThemeModule(context)
    }
}

private fun hex(color: Int): String {
    val rgb = "#" + (color and 0xFFFFFF).toString(16).padStart(6, '0')
    val alpha = color ushr 24
    return if (alpha == 0xFF) rgb else rgb + alpha.toString(16).padStart(2, '0')
}
