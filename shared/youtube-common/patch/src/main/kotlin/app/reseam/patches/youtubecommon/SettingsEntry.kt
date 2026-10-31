// SPDX-FileCopyrightText: 2026 Reseam contributors <https://reseam.app>
// SPDX-License-Identifier: AGPL-3.0-or-later

package app.reseam.patches.youtubecommon

import app.reseam.patch.PatchRuntime
import app.reseam.patch.Type
import app.reseam.patch.after
import app.reseam.patch.klass
import app.reseam.patch.method
import app.reseam.patch.point
import app.reseam.patch.redirectTo
import app.reseam.patch.settings.Reseam
import app.reseam.youtube.flags.bindings.FlagsExport

/**
 * Makes a `<Preference>` row whose `<intent>` was set up by
 * [app.reseam.patch.settings.opensSettings] open the bundle's settings when tapped.
 */
fun PatchRuntime.openSettingsFromPreferenceIntents() {
    preferencePerformClick.point { invokeVirtual { name("startActivity") } }
        .redirectTo(Reseam.startActivity)
}

// androidx keeps the class name, but R8 renames performClick.
private val preferencePerformClick = method("preferencePerformClick") {
    inClass(klass("androidx.preference.Preference"))
    params()
    returns(Type.Void)
    calls { owner("android.content.Context"); name("startActivity") }
}

/** Long-pressing the Reseam settings row saves the app's experiment flags to Downloads. */
fun PatchRuntime.exportFlagsFromSettingsRow() {
    preferenceBind.after {
        FlagsExport.bindSettingsRow(thisObject.field(preferenceKey), param(0).field(preferenceRowView))
    }
}

private const val ANDROID_ID_SUMMARY = 0x01020010L

private val preference = klass("androidx.preference.Preference")

// The key setter requires a key once the preference persists, so it names the key field.
private val preferenceKey = method("preferenceSetKey") {
    inClass(preference)
    strings("Preference does not have a key assigned.")
}.point { field { type(Type.String) } }
    .field("preferenceKey")

// onBindViewHolder: the only Preference method that looks up android.R.id.summary in a row.
private val preferenceBind = method("preferenceBind") {
    inClass(preference)
    returns(Type.Void)
    paramCount(1)
    literals(ANDROID_ID_SUMMARY)
}

private val preferenceRowView = preferenceBind.point { field { type(Type.View) } }.field("preferenceRowView")
