// SPDX-FileCopyrightText: 2026 Reseam contributors <https://reseam.app>
// SPDX-License-Identifier: AGPL-3.0-or-later

package app.reseam.pangram

import android.content.Context
import android.content.SharedPreferences
import app.reseam.ailabels.DetectorException
import app.reseam.ailabels.Login
import java.util.UUID

/**
 * The Pangram login, kept out of the settings so a settings export never carries it. [installId]
 * stands in for the browser's extension id, which Pangram's API expects in the Origin header.
 */
internal object Session : Login {
    private const val ID = "session_id"
    private const val INSTALL_ID = "install_id"

    private lateinit var prefs: SharedPreferences

    fun init(context: Context) {
        prefs = context.getSharedPreferences("reseam_pangram", Context.MODE_PRIVATE)
        if (!prefs.contains(INSTALL_ID)) prefs.edit().putString(INSTALL_ID, UUID.randomUUID().toString()).apply()
    }

    val id: String? get() = prefs.getString(ID, null)

    val installId: String get() = checkNotNull(prefs.getString(INSTALL_ID, null))

    override val isSignedIn: Boolean get() = id != null

    fun require(): String = id ?: throw DetectorException.SignedOut()

    fun save(id: String) {
        prefs.edit().putString(ID, id).apply()
    }

    override fun signOut() {
        prefs.edit().remove(ID).apply()
    }
}
