// SPDX-FileCopyrightText: 2026 Reseam contributors <https://reseam.app>
// SPDX-License-Identifier: AGPL-3.0-or-later

@file:JvmName("Fonts")

package app.reseam.fonts

import android.app.Activity
import android.app.Application
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.fonts.Font
import android.graphics.text.TextRunShaper
import android.os.Build
import android.widget.TextView
import app.reseam.fonts.settings.FontSettings
import app.reseam.runtime.Export
import java.io.File
import java.io.RandomAccessFile
import java.nio.channels.FileChannel
import java.util.WeakHashMap
import kotlin.math.abs

private lateinit var app: Application
private val dataDir by lazy { app.dataDir.canonicalPath + File.separator }

@Volatile private var replacement: Replacement? = null
@Volatile private var decided = false
private val replaced = WeakHashMap<Typeface, Typeface>()

@Export
fun init(application: Application) {
    app = application
}

@Export fun pickFont(activity: Activity) = FontRequest.start(activity)

@Export(name = "paintTypeface") fun setTypeface(paint: Paint, typeface: Typeface?): Typeface? = paint.setTypeface(typeface(typeface))

@Export(name = "viewTypeface") fun setTypeface(view: TextView, typeface: Typeface?) = view.setTypeface(typeface(typeface))

@Export(name = "viewTypefaceStyle") fun setTypeface(view: TextView, typeface: Typeface?, style: Int) = view.setTypeface(typeface(typeface), style)

/** The typeface to draw with instead of [typeface]: the chosen font at its weight and slant, for text fonts only. */
@Export
fun typeface(typeface: Typeface?): Typeface? {
    // Telling a text font from an icon font needs the shaped glyph's font, which Android 12 added.
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return typeface
    val replacement = replacement() ?: return typeface
    val original = typeface ?: Typeface.DEFAULT
    return synchronized(replaced) {
        replaced.getOrPut(original) {
            (if (isText(original)) replacement.at(original.weight, original.isItalic) else original)
                .also { replaced[it] = it }
        }
    }
}

internal fun fontFile(): File = File(app.filesDir, "reseam/font")

/** Drops what was resolved for the old font, so text created from now on uses the new one. */
internal fun fontChanged() {
    synchronized(replaced) {
        replaced.clear()
        decided = false
    }
}

private fun replacement(): Replacement? {
    if (decided) return replacement
    // Settings read as their defaults until the app starts, so nothing is decided before then.
    if (!::app.isInitialized) return null
    return synchronized(replaced) {
        if (!decided) {
            replacement = when (FontSettings.font.value) {
                "system" -> Replacement { Typeface.DEFAULT }
                "custom" -> fontFile().takeIf(File::exists)?.let(::custom)
                else -> null
            }
            decided = true
        }
        replacement
    }
}

private fun custom(file: File): Replacement =
    if (FontData(map(file), 0).weightAxis) {
        Replacement { weight -> Typeface.Builder(file).setFontVariationSettings("'wght' $weight").setWeight(weight).build() }
    } else {
        val font = Typeface.Builder(file).build()
        Replacement { font }
    }

private fun map(file: File) = RandomAccessFile(file, "r").use { it.channel.map(FileChannel.MapMode.READ_ONLY, 0, it.length()) }

/** The chosen font at each weight and slant, built from [base], which may already carry the weight. */
private class Replacement(private val base: (Int) -> Typeface) {
    private val styles = HashMap<Int, Typeface>()

    fun at(weight: Int, italic: Boolean): Typeface =
        synchronized(styles) { styles.getOrPut(weight * 2 + if (italic) 1 else 0) { Typeface.create(base(weight), weight, italic) } }
}

private fun isText(typeface: Typeface): Boolean {
    val paint = Paint().apply {
        this.typeface = typeface
        textSize = PROBE_SIZE
    }
    val glyphs = TextRunShaper.shapeTextRun("a", 0, 1, 0, 1, 0f, 0f, false, paint)
    if (glyphs.glyphCount() != 1) return false
    val font = glyphs.getFont(0)
    // Fonts the app downloads into its storage style content, such as the text creators put on videos.
    if (font.file?.canonicalPath?.startsWith(dataDir) == true) return false
    // A font without Latin letters, such as an icon font, draws "a" from a fallback font with other metrics.
    return ownFont(paint, font) && FontData(font.buffer, font.ttcIndex).isText
}

private fun ownFont(paint: Paint, font: Font): Boolean {
    val own = paint.fontMetrics
    val shaped = Paint.FontMetrics().also { font.getMetrics(paint, it) }
    return abs(own.ascent - shaped.ascent) < METRIC_TOLERANCE && abs(own.descent - shaped.descent) < METRIC_TOLERANCE
}

private const val PROBE_SIZE = 100f
private const val METRIC_TOLERANCE = 0.01f
