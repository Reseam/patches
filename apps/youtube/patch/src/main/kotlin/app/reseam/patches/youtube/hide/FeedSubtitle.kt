// SPDX-FileCopyrightText: 2026 Reseam contributors <https://reseam.app>
// SPDX-FileCopyrightText: 2026 ReVanced contributors
// SPDX-License-Identifier: GPL-3.0-or-later

package app.reseam.patches.youtube.hide

import app.reseam.patch.PatchRuntime
import app.reseam.patch.Type
import app.reseam.patch.after
import app.reseam.patch.before
import app.reseam.patch.dex.AccessFlags
import app.reseam.patch.method
import app.reseam.patch.point
import app.reseam.patch.reserveLocal
import app.reseam.youtube.hidelayout.bindings.LayoutFeedSubtitle

private const val TYPED_VALUE = "Landroid/util/TypedValue;"
private const val DISPLAY_METRICS = "Landroid/util/DisplayMetrics;"

internal fun PatchRuntime.hideFeedSubtitle() {
    val subtitle = hideViewCountMethod
    val subtitleDimension = subtitle.reserveLocal("subtitleDimension", Type.Float)
    subtitle.point("subtitleDimensionSource") { string("Has attachmentRuns but drawableRequester is missing.") }
        .next { invokeStatic { owner(TYPED_VALUE); name("applyDimension"); params(Type.Int, Type.Float, DISPLAY_METRICS); returns(Type.Float) } }
        .captureArgumentAs("dimension", 1, Type.Float)
        .before { local(subtitleDimension).assign(capture("dimension")) }
    subtitle.after {
        result.assign(LayoutFeedSubtitle.modifyFeedSubtitleSpan(result, local(subtitleDimension)))
    }
}

private val hideViewCountMethod = method("hideViewCountMethod") {
    flags(AccessFlags.PUBLIC or AccessFlags.STATIC)
    returns(Type.CharSequence)
    strings("Has attachmentRuns but drawableRequester is missing.")
    hasParam(Type.Boolean)
    calls {
        owner("Landroid/util/TypedValue;")
        name("applyDimension")
        params(Type.Int, Type.Float, DISPLAY_METRICS)
        returns(Type.Float)
    }
}
