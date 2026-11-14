// SPDX-FileCopyrightText: 2026 Reseam contributors <https://reseam.app>
// SPDX-License-Identifier: AGPL-3.0-or-later

package app.reseam.pangram

import android.app.Activity
import app.reseam.ailabels.CheckEvents
import app.reseam.ailabels.DetectorException
import app.reseam.bundle.Logger
import app.reseam.bundle.ReseamPatches
import app.reseam.bundle.openUrl
import app.reseam.bundle.postToMainThread
import app.reseam.bundle.resumedActivity
import app.reseam.bundle.showToast
import app.reseam.pangram.settings.PangramStrings
import java.io.IOException
import java.util.concurrent.Executors

/** Signing in to and out of Pangram, and what Pangram asks the user to agree to. */
internal object PangramAccount {
    private const val CONSENT_URL = "https://www.pangram.com/chrome/consent"
    private const val LOGIN_TIMEOUT_MS = 10 * 60_000L
    private const val LOGIN_POLL_MS = 1_000L
    private const val LOGIN_POLL_MAX_MS = 5_000L

    private val thread = Executors.newSingleThreadExecutor()

    fun open(activity: Activity) {
        val session = Session.id ?: return showSignedOut(activity)
        thread.execute {
            val account = runCatching { PangramApi.account(session) }
            postToMainThread {
                account
                    .onSuccess { showAccount(activity, it) }
                    .onFailure { error ->
                        if (error is DetectorException.SignedOut) {
                            signOut()
                            showSignedOut(activity)
                        } else {
                            Logger.warn(error) { "Loading the Pangram account failed" }
                            activity.showToast(PangramStrings.pangramUnavailable)
                        }
                    }
            }
        }
    }

    fun askImageConsent(activity: Activity) {
        ReseamPatches.bundle().ui(activity).dialog()
            .title(PangramStrings.pangramImageConsentTitle)
            .message(PangramStrings.pangramImageConsentMessage)
            .dismissAction(PangramStrings.pangramClose)
            .action(PangramStrings.pangramImageConsentTerms) { activity.openUrl(CONSENT_URL) }
            .action(PangramStrings.pangramImageConsentAgree) { thread.execute(ImageChecks::agree) }
            .show()
    }

    /** Signs in through the browser, so Google and the other sign-in methods work, then waits for the session. */
    private fun signIn(activity: Activity) {
        thread.execute {
            val token = runCatching { PangramApi.startLogin() }
                .onFailure { Logger.warn(it) { "Starting the Pangram login failed" } }
                .getOrElse { return@execute activity.showToast(PangramStrings.pangramUnavailable) }
            postToMainThread { activity.openUrl(PangramApi.loginUrl(token)) }
            val session = awaitLogin(token) ?: return@execute activity.showToast(PangramStrings.pangramSignInFailed)
            Session.save(session)
            CheckEvents.notifyChanged()
            activity.showToast(PangramStrings.pangramSignedIn)
            val account = runCatching { PangramApi.account(session) }
                .onFailure { Logger.warn(it) { "Loading the Pangram account failed" } }
                .getOrNull()
            if (account?.feedConsentAnswered == false) postToMainThread { askFeedConsent(session) }
        }
    }

    private fun awaitLogin(token: String): String? {
        val deadline = System.currentTimeMillis() + LOGIN_TIMEOUT_MS
        var wait = LOGIN_POLL_MS
        while (System.currentTimeMillis() < deadline) {
            Thread.sleep(wait)
            wait = minOf(wait + LOGIN_POLL_MS / 4, LOGIN_POLL_MAX_MS)
            // The session is handed out once; a background app may lose the answer to a cut connection.
            if (resumedActivity() == null) continue
            try {
                PangramApi.consumeLogin(token)?.let { return it }
            } catch (e: LoginExpiredException) {
                return null
            } catch (e: IOException) {
                Logger.info(e) { "Pangram login not reachable yet" }
            }
        }
        return null
    }

    /** Pangram asks once whether feed posts may be checked; its extension asks the same. */
    private fun askFeedConsent(session: String) {
        val activity = resumedActivity() ?: return
        val answer = { agree: Boolean ->
            thread.execute {
                runCatching { PangramApi.answerFeedConsent(session, agree) }
                    .onFailure { Logger.warn(it) { "Answering Pangram's feed consent failed" } }
            }
        }
        ReseamPatches.bundle().ui(activity).dialog()
            .title(PangramStrings.pangramFeedConsentTitle)
            .message(PangramStrings.pangramFeedConsentMessage)
            .action(PangramStrings.pangramFeedConsentDecline) { answer(false) }
            .action(PangramStrings.pangramFeedConsentAgree) { answer(true) }
            .show()
    }

    private fun signOut() {
        Session.signOut()
        CheckEvents.notifyChanged()
    }

    private fun showSignedOut(activity: Activity) {
        ReseamPatches.bundle().ui(activity).dialog()
            .title(NAME)
            .message(PangramStrings.pangramSignedOutMessage)
            .dismissAction(PangramStrings.pangramClose)
            .action(PangramStrings.pangramSignIn) { signIn(activity) }
            .show()
    }

    private fun showAccount(activity: Activity, account: Account) {
        val plan =
            if (account.planActive) PangramStrings.pangramPlan(account.planName.orEmpty(), account.creditsRemaining)
            else PangramStrings.pangramNoPlan
        ReseamPatches.bundle().ui(activity).dialog()
            .title(PangramStrings.pangramSignedInAs(account.email))
            .message(plan)
            .dismissAction(PangramStrings.pangramClose)
            .action(PangramStrings.pangramSignOut) { signOut() }
            .show()
    }
}
