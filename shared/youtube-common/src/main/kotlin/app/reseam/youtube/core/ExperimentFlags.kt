// SPDX-FileCopyrightText: 2026 Reseam contributors <https://reseam.app>
// SPDX-License-Identifier: AGPL-3.0-or-later

package app.reseam.youtube.core

import android.content.Context
import android.util.Base64
import java.nio.ByteBuffer
import java.nio.ByteOrder

private const val PREFERENCES = "youtube"
private const val CONFIG_KEY = "com.google.android.libraries.youtube.innertube.%s_config_group"
private const val FLAG_MAP = 346648555

/** A server-synced experiment flag; [config] is the store it came from, "hot" or "cold". */
class ExperimentFlag(val config: String, val id: Long, val type: String, val value: String)

/** The experiment flags the app last synced, as it stores them. */
fun experimentFlags(context: Context): List<ExperimentFlag> {
    val preferences = context.getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE)
    return listOf("hot", "cold").flatMap { config ->
        val stored = preferences.getString(CONFIG_KEY.format(config), null) ?: return@flatMap emptyList()
        val map = flagMap(Base64.decode(stored, Base64.URL_SAFE)) ?: return@flatMap emptyList()
        Proto.fields(map)
            .filter { it.number == 1 && it.wireType == Proto.WIRE_LENGTH }
            .map { flag(config, checkNotNull(it.bytes)) }
    }
}

// The map's depth inside the stored config group differs between apps and versions.
private fun flagMap(message: ByteArray): ByteArray? {
    val fields = runCatching { Proto.fields(message) }.getOrNull() ?: return null
    return fields.bytes(FLAG_MAP)
        ?: fields.firstNotNullOfOrNull { field ->
            if (field.wireType == Proto.WIRE_LENGTH) flagMap(checkNotNull(field.bytes)) else null
        }
}

private fun flag(config: String, entry: ByteArray): ExperimentFlag {
    val fields = Proto.fields(entry)
    val value = fields.bytes(2)?.let { Proto.fields(it).lastOrNull() }
    return ExperimentFlag(config, checkNotNull(fields.number(1)), type(value), display(value))
}

private fun type(value: Proto.Field?) =
    when (value?.number) {
        null -> "none"
        1 -> "bool"
        2 -> "int"
        3 -> "string"
        4 -> "double"
        5 -> "bytes"
        else -> value.number.toString()
    }

private fun display(value: Proto.Field?): String {
    val bytes = value?.bytes ?: return value?.varint?.toString().orEmpty()
    return when (value.number) {
        3 -> bytes.decodeToString()
        4 -> ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN).double.toString()
        else -> Base64.encodeToString(bytes, Base64.NO_WRAP)
    }
}
