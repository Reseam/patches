// SPDX-FileCopyrightText: 2026 Cossale <hello@auna.li>
// SPDX-License-Identifier: AGPL-3.0-or-later

@file:JvmName("LoginTransfer")

package app.reseam.x.login

import android.accounts.Account
import android.accounts.AccountManager
import android.app.Activity
import android.app.AlertDialog
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Bundle
import android.util.Base64
import android.widget.Toast
import app.reseam.bundle.Logger
import app.reseam.bundle.showToast
import app.reseam.runtime.Export
import app.reseam.x.settings.XSettings
import app.reseam.x.settings.XStrings
import org.json.JSONObject

// AccountManager keys X stores a session under; the literal prefix is fixed across package renames.
private const val USER_ID = "account_user_id"
private const val USERNAME = "com.twitter.android.username"
private const val NAME = "com.twitter.android.name"
private const val TOKEN = "com.twitter.android.oauth.token"
private const val SECRET = "com.twitter.android.oauth.token.secret"
private const val LITE_VERSION = "com.x.android.lite.version"
private const val CURRENT_USER = "current_user_id"
private const val PREFIX = "reseam-x:"

private fun Context.accountType() = "$packageName.auth.login"

private fun Context.appPrefs() = getSharedPreferences("${packageName}_preferences", Context.MODE_PRIVATE)

private fun appContext(): Context? =
    runCatching { Class.forName("android.app.ActivityThread").getMethod("currentApplication").invoke(null) as? Context }
        .getOrNull()

private fun Context.clipboard() = getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager

/** Copies the current session to the clipboard for another device to import. */
@Export
fun copyLogin() {
    val context = appContext() ?: return
    val blob = export(context)
    if (blob == null) {
        context.showToast(XStrings.loginExportEmpty, Toast.LENGTH_SHORT)
        return
    }
    context.clipboard()?.setPrimaryClip(ClipData.newPlainText("X login", blob))
    context.showToast(XStrings.loginExported, Toast.LENGTH_SHORT)
}

/**
 * When signed out, offers to import a login from the clipboard. Called from MainActivity so it
 * reaches the user on the welcome screen, where settings are not available.
 */
@Export
fun offerImport(activity: Activity?) {
    activity ?: return
    if (!XSettings.showImportOnLaunch.value) return
    if (AccountManager.get(activity).getAccountsByType(activity.accountType()).isNotEmpty()) return
    activity.window?.decorView?.post {
        AlertDialog.Builder(activity)
            .setTitle(XStrings.loginImportTitle)
            .setMessage(XStrings.loginImportPrompt)
            .setPositiveButton(XStrings.loginImport) { _, _ -> importFromClipboard(activity) }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }
}

// Read on tap, not to gate the prompt, so the app never touches the clipboard on its own.
private fun importFromClipboard(activity: Activity) {
    val blob = activity.clipboard()?.primaryClip?.takeIf { it.itemCount > 0 }?.getItemAt(0)?.text?.toString()
    if (import(activity, blob)) restart(activity)
    else activity.showToast(XStrings.loginImportFailed, Toast.LENGTH_SHORT)
}

private fun restart(context: Context) {
    val launch = context.packageManager.getLaunchIntentForPackage(context.packageName)
    launch?.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK or android.content.Intent.FLAG_ACTIVITY_CLEAR_TASK)
    context.startActivity(launch)
    Runtime.getRuntime().exit(0)
}

/** The signed-in session as a portable blob, or null when no account is present. */
private fun export(context: Context): String? {
    val manager = AccountManager.get(context)
    val accounts = manager.getAccountsByType(context.accountType())
    if (accounts.isEmpty()) return null
    val current = context.appPrefs().getLong(CURRENT_USER, 0L)
    val account = accounts.firstOrNull { manager.getUserData(it, USER_ID)?.toLongOrNull() == current } ?: accounts.first()
    val userId = manager.getUserData(account, USER_ID) ?: return null
    val token = manager.peekAuthToken(account, TOKEN) ?: return null
    val secret = manager.peekAuthToken(account, SECRET) ?: return null
    val payload =
        JSONObject().apply {
            put("u", userId)
            put("n", manager.getUserData(account, USERNAME))
            put("d", manager.getUserData(account, NAME))
            put("t", token)
            put("s", secret)
        }
    return PREFIX + Base64.encodeToString(payload.toString().toByteArray(), Base64.NO_WRAP or Base64.URL_SAFE)
}

/** Restores a session from [blob] into AccountManager. Caller restarts the app afterwards. */
private fun import(context: Context, blob: String?): Boolean {
    val encoded = blob?.trim()?.takeIf { it.startsWith(PREFIX) }?.removePrefix(PREFIX) ?: return false
    val payload =
        runCatching { JSONObject(String(Base64.decode(encoded, Base64.NO_WRAP or Base64.URL_SAFE))) }
            .onFailure { Logger.error(it) { "Unreadable login blob" } }
            .getOrNull() ?: return false
    val userId = payload.optString("u").toLongOrNull() ?: return false
    val userName = payload.optString("n")
    val token = payload.optString("t").ifEmpty { return false }
    val secret = payload.optString("s").ifEmpty { return false }

    val manager = AccountManager.get(context)
    val account = Account(userName.ifEmpty { userId.toString() }, context.accountType())
    val data =
        Bundle().apply {
            putString(USER_ID, userId.toString())
            putString(USERNAME, userName)
            putString(NAME, payload.optString("d"))
            putString(LITE_VERSION, "1")
        }
    if (!manager.addAccountExplicitly(account, null, data)) {
        data.keySet().forEach { manager.setUserData(account, it, data.getString(it)) }
    }
    manager.setAuthToken(account, TOKEN, token)
    manager.setAuthToken(account, SECRET, secret)
    context.appPrefs().edit().putLong(CURRENT_USER, userId).apply()
    return true
}
