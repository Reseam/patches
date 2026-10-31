// SPDX-FileCopyrightText: 2026 Reseam contributors <https://reseam.app>
// SPDX-License-Identifier: AGPL-3.0-or-later

package app.reseam.discord.themes

import android.content.Context
import android.util.AtomicFile
import java.io.File
import java.io.FileNotFoundException
import java.io.IOException

internal fun Context.themeFile(name: String): File =
    File(filesDir, "reseam-patches/discord-themes/$name")

@Throws(IOException::class)
internal fun File.readAtomically(): String =
    try {
        AtomicFile(this).readFully().toString(Charsets.UTF_8)
    } catch (_: FileNotFoundException) {
        ""
    }

@Throws(IOException::class)
internal fun File.writeAtomically(text: String) {
    val parent = checkNotNull(parentFile) { "Theme files need a parent directory" }
    if (!parent.isDirectory && !parent.mkdirs()) throw IOException("Cannot create $parent")
    val atomic = AtomicFile(this)
    val output = atomic.startWrite()
    try {
        // Only IOExceptions roll back; other failures leave the pending atomic write in place.
        output.write(text.toByteArray(Charsets.UTF_8))
        atomic.finishWrite(output)
    } catch (exception: IOException) {
        atomic.failWrite(output)
        throw exception
    }
}
