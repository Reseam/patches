// SPDX-FileCopyrightText: 2026 Reseam contributors <https://reseam.app>
// SPDX-License-Identifier: AGPL-3.0-or-later

package app.reseam.pangram

import android.app.Activity
import android.app.Application
import app.reseam.ailabels.Checks
import app.reseam.ailabels.Detector
import app.reseam.ailabels.ImageVerdict
import app.reseam.ailabels.Post
import app.reseam.ailabels.PostImage
import app.reseam.ailabels.TextVerdict
import app.reseam.ailabels.register
import app.reseam.runtime.Export

internal const val NAME = "Pangram"

@Export
object Pangram : Detector {
    override val minWords = 50
    override val shortTextWords = 150
    override val shortTextUrl = "https://www.pangram.com/blog/is-pangram-accurate#short-text"
    override val text: Checks<Post, TextVerdict> = TextChecks
    override val images: Checks<PostImage, ImageVerdict> = ImageChecks

    @Export
    @JvmStatic
    fun init(application: Application) {
        Session.init(application)
        register("pangram", this)
    }

    override fun openAccount(activity: Activity) = PangramAccount.open(activity)

    override fun requestImageConsent(activity: Activity) = PangramAccount.askImageConsent(activity)
}
