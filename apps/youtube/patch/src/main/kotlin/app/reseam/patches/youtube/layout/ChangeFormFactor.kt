// SPDX-FileCopyrightText: 2026 AunAli K. <hello@auna.li>
// SPDX-FileCopyrightText: 2026 ReVanced contributors
// SPDX-License-Identifier: GPL-3.0-or-later

package app.reseam.patches.youtube.layout

import app.reseam.patch.ExtClass
import app.reseam.patch.Type
import app.reseam.patch.after
import app.reseam.patch.klass
import app.reseam.patch.method
import app.reseam.patch.patch
import app.reseam.patch.point
import app.reseam.patch.settings.section
import app.reseam.patches.youtube.core.YOUTUBE
import app.reseam.patches.youtube.core.YouTubeSettings
import app.reseam.patches.youtube.core.YouTubeSettingsPages
import app.reseam.patches.youtube.core.youTubeSettings

val changeFormFactor = patch("Change form factor") {
    description("Serves the phone, tablet or wearable layout regardless of the device.")
    compatibleWith(YOUTUBE)
    settings(youTubeSettings, section(YouTubeSettingsPages.Appearance, "Form factor", YouTubeSettings.changeFormFactor))

    execute {
        // The enum carries the wire value in its only instance field, and the client info builder
        // is the one place that field is read.
        clientInfoBuilder
            .point { field { owner(formFactorEnum.descriptor); type(Type.Int) } }
            .captureAs("formFactor", Type.Int)
            .after { capture("formFactor").assign(call(FormFactor.formFactor, capture("formFactor"))) }
    }
}

val formFactorEnum = klass("formFactorEnum") {
    strings("AUTOMOTIVE_FORM_FACTOR")
}

// The class that assembles the client info the app sends with every request.
val clientInfoProvider = klass("clientInfoProvider") {
    strings("Failed to read the client side experiments map from the disk")
}

// It builds one client info; the time zone is what separates it from the class's other getters.
val clientInfoBuilder = method("clientInfoBuilder") {
    inClass(clientInfoProvider)
    params()
    calls { owner("Ljava/util/TimeZone;"); name("getDefault") }
}

object FormFactor : ExtClass("app.reseam.youtube.misc.FormFactor") {
    val formFactor = static("formFactor", Type.Int, returns = Type.Int)
}
