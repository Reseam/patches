// SPDX-FileCopyrightText: 2026 Reseam contributors <https://reseam.app>
// SPDX-FileCopyrightText: 2026 ReVanced contributors
// SPDX-License-Identifier: GPL-3.0-or-later

package app.reseam.youtube.video

import app.reseam.bundle.Logger
import app.reseam.runtime.Export
import app.reseam.youtube.core.debug
import app.reseam.youtube.player.Event
import app.reseam.youtube.player.ShortsPlayer

@Export
object VideoQuality {
    @Export
    interface Menu {
        @Export fun patch_setQuality(quality: Option?)
    }

    @Export
    interface Option {
        @Export fun patch_getQualityName(): String?

        @Export fun patch_getResolution(): Int
    }

    const val AUTOMATIC = -2

    val onChange = Event<Option?>()

    private var menu: Menu? = null
    private var desiredResolution = AUTOMATIC
    private var needsUpdating = false

    var available: Array<out Option?>? = null
        private set

    var current: Option? = null
        private set(quality) {
            if (field === quality) return
            field = quality
            onChange.fire(quality)
        }

    internal fun reset() {
        desiredResolution = AUTOMATIC
        needsUpdating = false
        available = null
        menu = null
        current = null
    }

    /** Some qualities report a resolution that disagrees with their name. */
    @Export
    @JvmStatic
    fun fixResolution(name: String?, quality: Int): Int {
        if (name == null || name.startsWith(quality.toString())) return quality
        val fixed = name.substringBefore('p', "").toIntOrNull() ?: return quality
        Logger.debug { "Fixed quality resolution $quality -> $fixed" }
        return fixed
    }

    @Export
    @JvmStatic
    fun update(qualities: Array<out Option?>?, menu: Menu?, originalIndex: Int): Int = runCatching {
        select(qualities, menu, originalIndex)
    }
        .onFailure { Logger.error(it) { "Video quality update failure" } }
        .getOrDefault(originalIndex)

    private fun select(qualities: Array<out Option?>?, menu: Menu?, originalIndex: Int): Int {
        if (qualities.isNullOrEmpty()) return originalIndex
        this.menu = menu
        val changed = !available.contentEquals(qualities)
        if (changed) {
            available = qualities
            Logger.debug { "Video qualities: ${qualities.contentToString()}" }
        }
        val index = originalIndex.coerceIn(qualities.indices)
        qualities[index]?.takeIf { it.patch_getResolution() != AUTOMATIC }?.let { current = it }
        if (desiredResolution == AUTOMATIC) return index
        if (needsUpdating) needsUpdating = false else if (!changed) return index

        val desired =
            qualities.indices.firstOrNull { i ->
                val resolution = qualities[i]?.patch_getResolution() ?: return@firstOrNull false
                (resolution != AUTOMATIC && resolution <= desiredResolution) ||
                    i == qualities.lastIndex
            } ?: return index
        if (desired != index || !ShortsPlayer.isOpen) change(qualities[desired])
        return desired
    }

    fun setDesiredResolution(resolution: Int) {
        desiredResolution = resolution
        needsUpdating = true
        Logger.debug { "Desired video resolution: $resolution" }
    }

    fun change(quality: Option?) {
        val menu = menu ?: return Logger.error { "Cannot change quality: no quality menu" }
        menu.patch_setQuality(quality)
    }

    fun isPremium(quality: Option?): Boolean =
        quality?.patch_getQualityName()?.contains("Premium") == true
}
