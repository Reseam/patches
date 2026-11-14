// SPDX-FileCopyrightText: 2026 Reseam contributors <https://reseam.app>
// SPDX-License-Identifier: AGPL-3.0-or-later

package app.reseam.ailabels

import android.app.Activity
import java.io.IOException

enum class Label { Human, Mixed, AiAssisted, Ai }

/** One stretch of a text the detector scored on its own. */
class Segment(val words: Int, val label: Label)

/** How much of a text is AI, AI-assisted and human; the three add up to one. */
class Shares(val ai: Double, val assisted: Double, val human: Double)

/** Where in a text the AI content sits, in the detector's words. */
class Distribution(val title: String, val description: String)

/** [id] names the result at the detector; [source] names the detector and model, such as `Pangram 4.1`. */
sealed interface Verdict {
    val id: String
    val label: Label
    val title: String
    val source: String
}

class TextVerdict(
    override val id: String,
    override val label: Label,
    override val title: String,
    override val source: String,
    val summary: String,
    val wordCount: Int,
    val shares: Shares?,
    val distribution: Distribution?,
    val segments: List<Segment>,
) : Verdict

/** [confidence] is the detector's own wording, such as `Medium`. */
class ImageVerdict(
    override val id: String,
    override val label: Label,
    override val title: String,
    override val source: String,
    val aiLikelihood: Double,
    val confidence: String,
) : Verdict

sealed class DetectorException(message: String) : IOException(message) {
    class SignedOut : DetectorException("Not signed in")

    class DailyLimit : DetectorException("Daily limit reached")

    class TooFast : DetectorException("Checking too quickly")

    /** The user has not agreed to this kind of check with the detector yet. */
    class NeedsConsent : DetectorException("Consent needed")
}

/** The user's account at a detector, which checks of new items run under. */
interface Login {
    val isSignedIn: Boolean

    fun signOut()
}

/** A service that tells human from AI content. */
interface Detector {
    /** Shorter texts are not sent. */
    val minWords: Int

    /** Texts shorter than this get a less certain verdict. */
    val shortTextWords: Int

    /** Where the detector explains its accuracy on short texts. */
    val shortTextUrl: String?

    val text: Checks<Post, TextVerdict>

    val images: Checks<PostImage, ImageVerdict>

    fun openAccount(activity: Activity)

    /** Asks the user to allow image checks, for detectors that need their agreement first. */
    fun requestImageConsent(activity: Activity)
}
