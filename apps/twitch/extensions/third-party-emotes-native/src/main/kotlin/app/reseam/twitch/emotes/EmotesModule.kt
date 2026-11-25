// SPDX-FileCopyrightText: 2026 Reseam contributors <https://reseam.app>
// SPDX-License-Identifier: AGPL-3.0-or-later

package app.reseam.twitch.emotes

import app.reseam.runtime.Export
import com.facebook.react.bridge.NativeModule
import com.facebook.react.bridge.ReactApplicationContext
import com.facebook.react.bridge.ReactContextBaseJavaModule
import com.facebook.react.bridge.ReactMethod
import com.facebook.react.module.annotations.ReactModule
import org.json.JSONArray

private const val EMOTES_MODULE_NAME = "ReseamTwitchEmotes"

// Twitch's R8 build renames this annotation, so the app finds the module through getName. The build
// still reads it to declare the module for the JavaScript extension.
@ReactModule(name = EMOTES_MODULE_NAME)
class EmotesModule(context: ReactApplicationContext) : ReactContextBaseJavaModule(context) {
    override fun getName(): String = EMOTES_MODULE_NAME

    /**
     * Returns a JSON array of the third-party emotes that chat shows in [channelId], global ones first and each
     * provider after those it overrides, or null while one of them has not answered yet.
     */
    @ReactMethod(isBlockingSynchronousMethod = true)
    fun emotes(channelId: String): String? {
        val providers = Provider.entries.filter { it.enabled }
        val lists = listOf(GLOBAL, channelId).flatMap { channel -> providers.map { reactApplicationContext.emotes(it, channel) } }
        if (lists.any { it == null }) return null
        return JSONArray(lists.flatMap { it.orEmpty() }.map(Emote::toJson)).toString()
    }

    companion object {
        @Export
        @JvmStatic
        fun withEmotesModule(modules: List<NativeModule>?, context: ReactApplicationContext): List<NativeModule> =
            modules.orEmpty() + EmotesModule(context)
    }
}
