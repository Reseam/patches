// SPDX-FileCopyrightText: 2026 AunAli K. <hello@auna.li>
// SPDX-License-Identifier: GPL-3.0-or-later
package app.reseam.patches.youtube.internal

import app.reseam.patch.*

private const val AD_PROGRESS = "com.google.android.libraries.youtube.ads.player.ui.AdProgressTextView"
private val adVisibility = methods("ad progress visibility") {
    calls { owner(AD_PROGRESS); name("setVisibility"); params(Type.Int) }
    params(Type.Boolean)
    returns(Type.Void)
}.points("ad progress text") {
    invokeVirtual { owner(AD_PROGRESS); name("setVisibility"); params(Type.Int) }
}

/** Resolve the shared indexed platform-call target once for all interested features. */
fun hookAdVisibility(callback: ExtMethod) {
    adVisibility.all.also { check(it.isNotEmpty()) { "Ad visibility hook is missing" } }.forEach {
        it.captureArgumentAs("visibility", 1, Type.Int).before { call(callback, capture("visibility")) }
    }
}
