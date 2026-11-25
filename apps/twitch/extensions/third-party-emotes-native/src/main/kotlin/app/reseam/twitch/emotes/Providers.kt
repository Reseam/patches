// SPDX-FileCopyrightText: 2026 Reseam contributors <https://reseam.app>
// SPDX-License-Identifier: AGPL-3.0-or-later

package app.reseam.twitch.emotes

import app.reseam.runtime.ToggleValue
import app.reseam.twitch.settings.ChatSettings
import org.json.JSONArray
import org.json.JSONObject

internal class Emote(val code: String, val id: String, val url: String, val aspectRatio: Double) {
    fun toJson(): JSONObject = JSONObject().put("code", code).put("id", id).put("url", url).put("aspectRatio", aspectRatio)

    companion object {
        fun fromJson(json: JSONObject) =
            Emote(json.getString("code"), json.getString("id"), json.getString("url"), json.getDouble("aspectRatio"))
    }
}

// The order is the precedence: a later provider wins a code that several define.
internal enum class Provider(private val setting: ToggleValue) {
    FFZ(ChatSettings.ffzEmotes) {
        override fun url(channelId: String) =
            if (channelId == GLOBAL) "https://api.frankerfacez.com/v1/set/global" else "https://api.frankerfacez.com/v1/room/id/$channelId"

        override fun parse(body: String, channelId: String): List<Emote> {
            val root = JSONObject(body)
            val sets = root.getJSONObject("sets")
            val ids = if (channelId == GLOBAL) root.getJSONArray("default_sets").strings() else sets.keys().asSequence().toList()
            return ids.flatMap { sets.optJSONObject(it)?.getJSONArray("emoticons")?.objects().orEmpty() }.mapNotNull(::ffzEmote)
        }
    },
    BTTV(ChatSettings.bttvEmotes) {
        override fun url(channelId: String) =
            if (channelId == GLOBAL) "https://api.betterttv.net/3/cached/emotes/global" else "https://api.betterttv.net/3/cached/users/twitch/$channelId"

        override fun parse(body: String, channelId: String): List<Emote> {
            val emotes = if (channelId == GLOBAL) {
                JSONArray(body).objects()
            } else {
                JSONObject(body).let { it.getJSONArray("channelEmotes").objects() + it.getJSONArray("sharedEmotes").objects() }
            }
            return emotes.map(::bttvEmote)
        }
    },
    SEVEN_TV(ChatSettings.sevenTvEmotes) {
        override fun url(channelId: String) =
            if (channelId == GLOBAL) "https://7tv.io/v3/emote-sets/global" else "https://7tv.io/v3/users/twitch/$channelId"

        override fun parse(body: String, channelId: String): List<Emote> {
            val root = JSONObject(body)
            val emotes = if (channelId == GLOBAL) root.getJSONArray("emotes") else root.optJSONObject("emote_set")?.optJSONArray("emotes")
            return emotes?.objects().orEmpty().mapNotNull(::sevenTvEmote)
        }
    };

    val enabled: Boolean
        get() = setting.value

    abstract fun url(channelId: String): String

    abstract fun parse(body: String, channelId: String): List<Emote>
}

internal const val GLOBAL = ""

// The chat draws emotes at 24 dp, which the third size covers on every screen density.
private fun sevenTvEmote(json: JSONObject): Emote? {
    val data = json.getJSONObject("data")
    val host = data.getJSONObject("host")
    val fileName = if (data.optBoolean("animated")) "3x.gif" else "3x.webp"
    val file = host.getJSONArray("files").objects().firstOrNull { it.getString("name") == fileName } ?: return null
    return Emote(
        json.getString("name"),
        "7tv:${data.getString("id")}",
        "https:${host.getString("url")}/$fileName",
        file.getDouble("width") / file.getDouble("height"),
    )
}

private fun bttvEmote(json: JSONObject): Emote {
    val id = json.getString("id")
    val width = json.optDouble("width")
    val height = json.optDouble("height")
    return Emote(
        json.getString("code"),
        "bttv:$id",
        "https://cdn.betterttv.net/emote/$id/3x.${if (json.optBoolean("animated")) "gif" else "png"}",
        if (width > 0 && height > 0) width / height else 1.0,
    )
}

private fun ffzEmote(json: JSONObject): Emote? {
    val urls = json.optJSONObject("animated") ?: json.getJSONObject("urls")
    val url = listOf("4", "2", "1").firstOrNull { !urls.isNull(it) }?.let(urls::getString) ?: return null
    return Emote(json.getString("name"), "ffz:${json.getInt("id")}", url, json.getDouble("width") / json.getDouble("height"))
}

private fun JSONArray.objects(): List<JSONObject> = (0 until length()).map(::getJSONObject)

private fun JSONArray.strings(): List<String> = (0 until length()).map(::getString)
