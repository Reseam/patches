// SPDX-FileCopyrightText: 2026 AunAli K. <hello@auna.li>
// SPDX-FileCopyrightText: 2026 ReVanced contributors
// SPDX-License-Identifier: GPL-3.0-or-later

package app.reseam.patches.youtube.shorts

import app.reseam.patch.Type
import app.reseam.patch.dex.Opcode
import app.reseam.patch.method
import app.reseam.patch.methods
import app.reseam.patch.patch
import app.reseam.patch.point
import app.reseam.patch.points
import app.reseam.patch.settings.returnFalseWhen
import app.reseam.patch.settings.section
import app.reseam.patches.youtube.core.YOUTUBE
import app.reseam.patches.youtube.core.YouTubeSettings
import app.reseam.patches.youtube.core.YouTubeSettingsPages
import app.reseam.patches.youtube.core.youTubeSettings

private val eligibilityDescription = method("Shorts eligibility description") {
    strings("ShortsFirstEligibility(resumeToShortsEligibility=")
}
private val resumeEligibility = eligibilityDescription.point {
    opcode(Opcode.IGET_OBJECT)
    field { owner(eligibilityDescription.owner) }
}.field()
private val startupResolver = method("Shorts startup resolver") { strings("resolveShortsStartup") }
private val resumeAllowed = methods("Shorts startup eligibility checks") {
    calledBy(startupResolver)
    params(eligibilityDescription.owner)
    returns(Type.Boolean)
}.points("resume eligibility read") {
    opcode(Opcode.IGET_OBJECT)
    field { owner(resumeEligibility.owner); name(resumeEligibility.name) }
}.single().method

val disableResumingShortsOnStartup = patch("Disable resuming Shorts on startup") {
    description("Stops YouTube reopening the last Short when the app starts.")
    compatibleWith(YOUTUBE)
    dependsOn(youTubeSettings)
    settings(youTubeSettings, section(YouTubeSettingsPages.Shorts, "Shorts", YouTubeSettings.disableResumingShortsOnStartup))

    execute { resumeAllowed.returnFalseWhen(YouTubeSettings.disableResumingShortsOnStartup) }
}
