// SPDX-FileCopyrightText: 2026 Reseam contributors <https://reseam.app>
// SPDX-License-Identifier: AGPL-3.0-or-later

package app.reseam.pangram

import app.reseam.ailabels.Distribution
import app.reseam.ailabels.ImageVerdict
import app.reseam.ailabels.Label
import app.reseam.ailabels.Segment
import app.reseam.ailabels.Shares
import app.reseam.ailabels.TextVerdict
import app.reseam.bundle.objects
import org.json.JSONObject

internal data class Account(
    val email: String,
    val planName: String?,
    val planActive: Boolean,
    val creditsRemaining: Int,
    val feedConsentAnswered: Boolean,
)

internal data class ImageAccess(val available: Boolean, val consentAccepted: Boolean, val consentVersion: String, val maxUploadBytes: Long)

/**
 * [result] is Pangram's whole result for the image, set once a check succeeded. [ownerPath] and [sharePath] are
 * where a saved or shared image result shows on Pangram's site.
 */
internal data class ImageOperation(
    val id: String,
    val state: String,
    val result: JSONObject?,
    val reason: String?,
    val ownerPath: String?,
    val sharePath: String?,
) {
    val finished: Boolean get() = state in FINISHED

    /** Pangram no longer has the image and needs it uploaded. */
    val needsUpload: Boolean get() = state == "upload_required"

    private companion object {
        val FINISHED = setOf("succeeded", "failed", "expired", "retryable", "upload_required")
    }
}

internal fun JSONObject.toAccount(): Account {
    val plan = optJSONObject("plan")
    return Account(
        email = getString("email"),
        planName = plan?.optString("name"),
        planActive = plan?.optBoolean("active") == true,
        creditsRemaining = optInt("creditsRemaining"),
        feedConsentAnswered = optBoolean("consentAnswered"),
    )
}

internal fun JSONObject.toImageAccess(): ImageAccess {
    val consent = getJSONObject("consent")
    return ImageAccess(
        available = optBoolean("feature_enabled") && optBoolean("available"),
        consentAccepted = consent.getBoolean("accepted"),
        consentVersion = consent.getString("required_version"),
        maxUploadBytes = getLong("max_upload_bytes"),
    )
}

internal fun JSONObject.toImageOperation(): ImageOperation {
    val state = getString("state")
    return ImageOperation(
        id = getString("operation_id"),
        state = state,
        result = optJSONObject("result")?.takeIf { state == "succeeded" },
        reason = optString("reason").ifEmpty { null },
        ownerPath = optString("owner_path").ifEmpty { null },
        sharePath = optString("share_path").ifEmpty { null },
    )
}

internal fun JSONObject.toTextVerdict(): TextVerdict {
    val uuid = getString("uuid")
    return TextVerdict(
        id = uuid,
        label = when (val category = getString("badge_category")) {
            "human" -> Label.Human
            "mixed" -> Label.Mixed
            "ai-assisted" -> Label.AiAssisted
            "ai" -> Label.Ai
            else -> error("Unknown Pangram category $category")
        },
        title = getString("headline"),
        source = "$NAME ${getString("version")}",
        summary = getString("prediction"),
        wordCount = getInt("word_count"),
        shares = Shares(getDouble("fraction_ai"), getDouble("fraction_ai_assisted"), getDouble("fraction_human")),
        distribution = optJSONObject("ai_distribution")?.let { Distribution(it.getString("title"), it.getString("description")) },
        segments = optJSONArray("windows")?.objects().orEmpty().map { Segment(it.getInt("word_count"), windowLabel(it.getString("label"))) },
    )
}

internal fun JSONObject.toImageVerdict(): ImageVerdict {
    val model = getJSONObject("model")
    return ImageVerdict(
        id = getLong("scan_id").toString(),
        label = if (getString("category_label").equals("human", ignoreCase = true)) Label.Human else Label.Ai,
        title = getString("prediction"),
        source = NAME,
        aiLikelihood = model.getDouble("ai_likelihood"),
        confidence = model.getString("confidence"),
    )
}

/** Window labels are phrases such as `AI-Generated` or `Moderately AI-Assisted`. */
private fun windowLabel(text: String): Label = text.lowercase().let {
    when {
        "human" in it -> Label.Human
        "assist" in it -> Label.AiAssisted
        "mixed" in it -> Label.Mixed
        else -> Label.Ai
    }
}
