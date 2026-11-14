// SPDX-FileCopyrightText: 2026 Reseam contributors <https://reseam.app>
// SPDX-License-Identifier: AGPL-3.0-or-later

package app.reseam.ailabels

import android.util.LruCache
import app.reseam.ailabels.settings.AiLabelsStrings
import app.reseam.bundle.Logger
import app.reseam.crowdcache.Crowdcache
import app.reseam.crowdcache.CrowdcacheLookups
import java.time.LocalDate
import java.time.ZoneId
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import org.json.JSONObject

sealed interface CheckState<out V : Verdict> {
    data object Checking : CheckState<Nothing>

    class Done<out V : Verdict>(val verdict: V) : CheckState<V>

    data object SignedOut : CheckState<Nothing>

    data object DailyLimit : CheckState<Nothing>

    data object NeedsConsent : CheckState<Nothing>

    data object Failed : CheckState<Nothing>
}

/**
 * One kind of check by one detector. An item's verdict comes from memory, then crowdcache under
 * [namespace], then the detector under the user's [login]; the detector's result goes to crowdcache as it
 * returned it. The detector's daily limit resets at midnight in [limitZone].
 */
abstract class Checks<I, V : Verdict>(
    private val detector: String,
    private val namespace: String,
    private val login: Login,
    private val limitZone: ZoneId,
    parallel: Int,
) {
    private val verdicts = LruCache<String, V>(REMEMBERED)
    private val queued = ConcurrentHashMap.newKeySet<String>()
    private val failed = ConcurrentHashMap.newKeySet<String>()
    private val notCached = ConcurrentHashMap.newKeySet<String>()
    private val lookups = CrowdcacheLookups(namespace)
    private val network = Executors.newScheduledThreadPool(parallel)

    @Volatile private var limitedOn: LocalDate? = null
    @Volatile private var needsConsent = false

    protected abstract fun key(item: I): String

    protected abstract fun check(item: I): JSONObject

    protected abstract fun verdict(result: JSONObject): V

    /** A public link to [verdict] of [item]. Runs off the main thread. */
    abstract fun share(item: I, verdict: V): String

    /** The detector's page with [verdict] of [item] in full. Runs off the main thread. */
    abstract fun fullAnalysis(item: I, verdict: V): String

    fun state(item: I): CheckState<V> {
        val key = key(item)
        verdicts[key]?.let { return CheckState.Done(it) }
        if (key in failed) return CheckState.Failed
        if (key in notCached) blocked()?.let { return it }
        request(key, item)
        return CheckState.Checking
    }

    fun prefetch(item: I) {
        state(item)
    }

    protected fun consentGiven() {
        needsConsent = false
        CheckEvents.notifyChanged()
    }

    fun retry(item: I) {
        failed.remove(key(item))
        CheckEvents.notifyChanged()
    }

    private fun blocked(): CheckState<Nothing>? = when {
        !login.isSignedIn -> CheckState.SignedOut
        needsConsent -> CheckState.NeedsConsent
        limitedOn == LocalDate.now(limitZone) -> CheckState.DailyLimit
        else -> null
    }

    private fun request(key: String, item: I) {
        if (!queued.add(key)) return
        if (key in notCached) return network.execute { scan(key, item) }
        lookups.lookup(key) { cached ->
            val verdict = cached?.let { readCached(key, it) }
            if (verdict != null) return@lookup finish(key, verdict)
            notCached.add(key)
            if (blocked() == null) network.execute { scan(key, item) } else finish(key, null)
        }
    }

    private fun readCached(key: String, cached: JSONObject): V? =
        runCatching { verdict(cached) }
            .onFailure { Logger.warn(it) { "Crowdcache $namespace entry $key is unreadable" } }
            .getOrNull()

    private fun scan(key: String, item: I) {
        try {
            val result = check(item)
            finish(key, verdict(result))
            runCatching { Crowdcache.submit(namespace, key, result) }
                .onFailure { Logger.warn(it) { "Crowdcache $namespace submit failed" } }
        } catch (e: DetectorException.TooFast) {
            network.schedule({ scan(key, item) }, TOO_FAST_RETRY_SECONDS, TimeUnit.SECONDS)
        } catch (e: DetectorException.DailyLimit) {
            val today = LocalDate.now(limitZone)
            if (limitedOn != today) toast(AiLabelsStrings.aiLabelDailyLimit(detector))
            limitedOn = today
            finish(key, null)
        } catch (e: DetectorException.NeedsConsent) {
            needsConsent = true
            finish(key, null)
        } catch (e: DetectorException.SignedOut) {
            if (login.isSignedIn) toast(AiLabelsStrings.aiLabelSessionExpired(detector))
            login.signOut()
            finish(key, null)
        } catch (e: Exception) {
            Logger.warn(e) { "$detector $namespace check of $key failed" }
            failed.add(key)
            finish(key, null)
        }
    }

    private fun finish(key: String, verdict: V?) {
        verdict?.let { verdicts.put(key, it) }
        queued.remove(key)
        CheckEvents.notifyChanged()
    }

    private companion object {
        const val REMEMBERED = 1_000
        const val TOO_FAST_RETRY_SECONDS = 30L
    }
}
