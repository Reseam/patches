// SPDX-FileCopyrightText: 2026 Reseam contributors <https://reseam.app>
// SPDX-License-Identifier: AGPL-3.0-or-later

@file:JvmName("ShareLinks")

package app.reseam.instagram.links

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Intent
import android.net.Uri
import android.os.BaseBundle
import app.reseam.instagram.settings.LinkSettings
import app.reseam.runtime.Export

private val instagramLink = Regex("""https?://(?:www\.)?instagram\.com/\S*""")

// Embed hosts mirror posts and Reels, not profiles or stories.
private val postPath = Regex("""^/(?:p|reel|reels|tv)/""")

@Export
fun setPrimaryClip(clipboard: ClipboardManager, clip: ClipData) = clipboard.setPrimaryClip(rewrite(clip))

@Export
fun setText(clipboard: android.text.ClipboardManager, text: CharSequence?) = clipboard.setText(rewrite(text?.toString()))

@Export
fun putString(bundle: BaseBundle, key: String?, value: String?) = bundle.putString(key, rewrite(value))

@Export
fun putExtra(intent: Intent, key: String?, value: String?): Intent = intent.putExtra(key, rewrite(value))

private fun rewrite(clip: ClipData): ClipData {
    val text = clip.takeIf { it.itemCount == 1 }?.getItemAt(0)?.text?.toString() ?: return clip
    val rewritten = rewrite(text)
    return if (rewritten == text) clip else ClipData.newPlainText(clip.description.label, rewritten)
}

private fun rewrite(text: String?): String? = text?.let { instagramLink.replace(it) { link -> rewriteLink(link.value) } }

private fun rewriteLink(link: String): String {
    val uri = Uri.parse(link)
    val host = providerHost()?.takeIf { postPath.containsMatchIn(uri.path.orEmpty()) }
    val clean = LinkSettings.cleanShareLinks.value
    if (host == null && !clean) return link
    return uri.buildUpon()
        .apply { if (host != null) authority(host) }
        .apply { if (clean) clearQuery().fragment(null) }
        .build()
        .toString()
}

private fun providerHost(): String? =
    when (val provider = LinkSettings.shareLinkProvider.value) {
        "off" -> null
        "custom" -> LinkSettings.customShareLinkHost.value.trim().substringAfter("://").trimEnd('/').ifEmpty { null }
        else -> provider
    }
