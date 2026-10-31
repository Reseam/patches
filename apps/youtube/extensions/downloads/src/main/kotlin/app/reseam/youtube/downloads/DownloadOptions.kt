// SPDX-FileCopyrightText: 2026 Reseam contributors <https://reseam.app>
// SPDX-License-Identifier: AGPL-3.0-or-later

@file:JvmName("DownloadOptions")

package app.reseam.youtube.downloads

import app.reseam.bundle.Logger
import app.reseam.runtime.Export
import app.reseam.youtube.core.Proto

private const val PICKER_BODY = 372095937
private const val PREMIUM_LOCKED = 2L
private const val PREMIUM_UNLOCKED = 3L
private const val PREMIUM_UPSELL_SECTION = 2L

@Export
fun unlock(response: ByteArray): ByteArray? =
    try {
        replaceUpsell(response) ?: rewrite(response).takeUnless { it === response }
    } catch (exception: IllegalArgumentException) {
        Logger.error(exception) { "Download action response is not a protobuf message" }
        null
    }

private fun rewrite(message: ByteArray): ByteArray =
    Proto.rewriteLength(message) { number, value ->
        if (number == PICKER_BODY) rewritePicker(value)
        else
            try {
                rewrite(value)
            } catch (_: IllegalArgumentException) {
                value
            }
    }

private fun rewritePicker(model: ByteArray): ByteArray =
    Proto.rewriteLength(model) { number, value ->
        when (number) {
            1 -> unlockQualities(removeUpsell(value))
            2 -> unlockQualities(value)
            else -> value
        }
    }

private fun unlockQualities(message: ByteArray): ByteArray =
    Proto.rewriteLength(message) { number, option ->
        if (number != 2) option
        else
            Proto.rewriteNumber(option, 4) { availability ->
                if (availability == PREMIUM_LOCKED) PREMIUM_UNLOCKED else availability
            }
    }

private fun removeUpsell(optionList: ByteArray): ByteArray =
    Proto.rewriteLength(optionList) { number, section ->
        if (number == 12 && Proto.number(section, 1, 0) == PREMIUM_UPSELL_SECTION) null else section
    }
