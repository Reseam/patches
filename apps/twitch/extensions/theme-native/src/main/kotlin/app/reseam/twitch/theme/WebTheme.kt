// SPDX-FileCopyrightText: 2026 Reseam contributors <https://reseam.app>
// SPDX-License-Identifier: AGPL-3.0-or-later

@file:JvmName("WebTheme")

package app.reseam.twitch.theme

import android.content.Context
import android.webkit.WebResourceResponse
import android.webkit.WebView
import app.reseam.runtime.Export
import java.io.ByteArrayInputStream
import java.io.IOException
import java.io.InputStream
import java.io.SequenceInputStream
import java.util.Collections
import org.json.JSONObject

private const val HEAD = "<head>"
private const val BACKGROUND_VARIABLE = "--color-background-body"

// Twitch's pages open <head> in their first lines.
private const val HEAD_SEARCH_LENGTH = 1024

// The web pages name the steps of Twitch's purple and grey scales as the app's colors do, with dashes. Purple step 1
// has no app color, and Material You leaves grey steps 7 to 11 alone.
private val themedColors = listOf("twitch_purple") + (2..15).map { "twitch_purple_$it" } +
    listOf(1, 2, 3, 4, 5, 6, 12, 13, 14, 15).map { "hinted_grey_$it" }

/** Returns the web page in [response] with the app's purple and grey colors and the background colors named [darkBackgroundName] and [lightBackgroundName]. */
@Export
fun withTheme(response: WebResourceResponse?, view: WebView?, darkBackgroundName: String, lightBackgroundName: String): WebResourceResponse? {
    val page = response?.data
    if (page == null || view == null || response.mimeType != "text/html") return response
    val start = ByteArray(HEAD_SEARCH_LENGTH)
    // A missing page throws again when WebView reads it, and WebView reports that error itself.
    val length = try {
        page.readUpTo(start)
    } catch (_: IOException) {
        return response
    }
    // Latin-1 decodes each byte to one character, so the index is a byte offset.
    val head = String(start, 0, length, Charsets.ISO_8859_1).indexOf(HEAD)
    val parts = if (head < 0) {
        listOf(ByteArrayInputStream(start, 0, length), page)
    } else {
        val end = head + HEAD.length
        val style = "<style>${view.context.themeCss(darkBackgroundName, lightBackgroundName)}</style>".toByteArray()
        listOf(ByteArrayInputStream(start, 0, end), ByteArrayInputStream(style), ByteArrayInputStream(start, end, length - end), page)
    }
    response.data = SequenceInputStream(Collections.enumeration(parts))
    return response
}

/** Returns [script], which a bridge runs at the start of each of its pages, followed by code that adds the theme style of [withTheme]. */
@Export
fun withThemeScript(script: String?, view: WebView?, darkBackgroundName: String, lightBackgroundName: String): String? {
    if (script == null || view == null) return script
    val css = JSONObject.quote(view.context.themeCss(darkBackgroundName, lightBackgroundName))
    // At the start of a page, <html> can still be missing, so the style waits for it.
    return script + "\n;(function(){var s=document.createElement('style');s.textContent=$css;" +
        "function add(){var p=document.head||document.documentElement;if(p)p.appendChild(s);return !!p}" +
        "if(!add())new MutationObserver(function(r,o){if(add())o.disconnect()}).observe(document,{childList:true})})();"
}

/** Returns style rules that set the purple and grey steps and the body background of a web page to the app's colors. */
private fun Context.themeCss(darkBackgroundName: String, lightBackgroundName: String): String {
    // Twitch writes the colors into a :root rule and onto a wrapper element, so every element gets the override.
    val steps = themedColors.joinToString("") { declaration("--color-${it.replace('_', '-')}", it) }
    val light = declaration(BACKGROUND_VARIABLE, lightBackgroundName)
    val dark = declaration(BACKGROUND_VARIABLE, darkBackgroundName)
    // A page marks its dark theme on <body> until the app sets a theme class on <html>.
    return "*{$steps}" +
        "body,:root.tw-root--theme-light,:root.tw-root--theme-light *{$light}" +
        "body.dark-theme,:root.tw-root--theme-dark,:root.tw-root--theme-dark *{$dark}"
}

/** Returns a declaration that sets [variable] to the app's color [name] over the page's own, or nothing when the app has no such color. */
private fun Context.declaration(variable: String, name: String): String =
    colorHex(name)?.let { "$variable:$it!important;" }.orEmpty()

/** Fills [buffer] until it is full or the stream ends, and returns how many bytes it read. */
private fun InputStream.readUpTo(buffer: ByteArray): Int {
    var length = 0
    while (length < buffer.size) {
        val read = read(buffer, length, buffer.size - length)
        if (read < 0) break
        length += read
    }
    return length
}
