// SPDX-FileCopyrightText: 2026 Reseam contributors <https://reseam.app>
// SPDX-License-Identifier: AGPL-3.0-or-later

@file:JvmName("AiLabels")

package app.reseam.ailabels

import android.app.Activity
import android.app.Application
import app.reseam.ailabels.settings.AiLabelsSettings
import app.reseam.bundle.showToast
import app.reseam.bundle.trackResumedActivity
import app.reseam.runtime.Export
import java.util.concurrent.ConcurrentHashMap

private lateinit var app: Application
private val detectors = ConcurrentHashMap<String, Detector>()

/** The detector chosen in settings, once its service registered it. */
internal val detector: Detector? get() = detectors[AiLabelsSettings.aiDetector.value]

val labelsEnabled: Boolean get() = AiLabelsSettings.aiLabels.value

/** The checks [post]'s text gets, when labels are on and it is long enough to check. */
internal fun textChecks(post: Post): Checks<Post, TextVerdict>? =
    detector?.takeIf { labelsEnabled && post.wordCount >= it.minWords }?.text

internal fun imageChecks(): Checks<PostImage, ImageVerdict>? =
    detector?.takeIf { labelsEnabled && AiLabelsSettings.aiLabelImages.value }?.images

@Export
fun init(application: Application) {
    app = application
    trackResumedActivity(application)
}

/** [key] is the detector's value in the detector setting. */
fun register(key: String, detector: Detector) {
    detectors[key] = detector
}

/** Starts checking [post] and its images before they show, so their labels are ready when they do. */
fun prefetch(post: Post) {
    post.images.forEach(PostImages::add)
    textChecks(post)?.prefetch(post)
    imageChecks()?.let { checks -> post.images.forEach(checks::prefetch) }
}

@Export
fun openAccount(activity: Activity) {
    detector?.openAccount(activity)
}

internal fun toast(message: String) = app.showToast(message)
