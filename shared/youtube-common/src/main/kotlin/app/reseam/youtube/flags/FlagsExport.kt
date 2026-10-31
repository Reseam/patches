// SPDX-FileCopyrightText: 2026 Reseam contributors <https://reseam.app>
// SPDX-License-Identifier: AGPL-3.0-or-later

@file:JvmName("FlagsExport")

package app.reseam.youtube.flags

import android.content.Context
import android.view.View
import app.reseam.bundle.Logger
import app.reseam.bundle.saveToDownloads
import app.reseam.runtime.Export
import app.reseam.youtube.core.experimentFlags
import app.reseam.youtube.core.showToast
import app.reseam.youtubecommon.settings.YouTubeCommonStrings
import java.util.Collections
import java.util.WeakHashMap

private const val SETTINGS_ROW = "reseam_settings"

// Rows are recycled, so a row bound to another preference must lose the export.
private val exportRows = Collections.newSetFromMap(WeakHashMap<View, Boolean>())

/** Long-pressing the Reseam settings row saves the app's experiment flags, for bug reports. */
@Export
fun bindSettingsRow(key: String?, row: View) {
    if (key == SETTINGS_ROW) {
        exportRows.add(row)
        row.setOnLongClickListener {
            Thread { export(it.context.applicationContext) }.start()
            true
        }
    } else if (exportRows.remove(row)) {
        row.setOnLongClickListener(null)
        row.isLongClickable = false
    }
}

private fun export(context: Context) {
    try {
        val version = context.packageManager.getPackageInfo(context.packageName, 0).versionName
        val app = context.applicationInfo.loadLabel(context.packageManager).toString()
        val name = "${app.lowercase().replace(Regex("[^a-z0-9]+"), "-")}-flags-$version.tsv"
        val rows = experimentFlags(context).map { "${it.config}\t${it.id}\t${it.type}\t${it.value}" }
        val table = (listOf("config\tid\ttype\tvalue") + rows).joinToString("\n", postfix = "\n")
        context.saveToDownloads(name, "text/tab-separated-values", table.toByteArray())
        showToast(YouTubeCommonStrings.experimentFlagsSaved(name))
    } catch (exception: Exception) {
        Logger.error(exception) { "Experiment flag export failed" }
        showToast(YouTubeCommonStrings.experimentFlagsFailed)
    }
}
