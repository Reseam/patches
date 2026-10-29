// SPDX-FileCopyrightText: 2026 Cossale <hello@auna.li>
// SPDX-License-Identifier: AGPL-3.0-or-later

package app.reseam.patches.pinterest.prompts

import app.reseam.patch.Type
import app.reseam.patch.after
import app.reseam.patch.enumConstant
import app.reseam.patch.dex.ref
import app.reseam.patch.fieldTarget
import app.reseam.patch.klass
import app.reseam.patch.method
import app.reseam.patch.methods
import app.reseam.patch.patch
import app.reseam.patch.point
import app.reseam.patch.settings.gate
import app.reseam.patch.settings.section
import app.reseam.patches.pinterest.core.PINTEREST
import app.reseam.patches.pinterest.core.pinterestSettings
import app.reseam.pinterest.settings.PinterestStrings
import app.reseam.pinterest.settings.PromptSettings

val hideSurveys = patch("Hide survey prompts") {
    description("Hides survey prompts.")
    compatibleWith(PINTEREST)
    settings(pinterestSettings, section(PinterestStrings.sectionPrompts, PromptSettings.hideSurveys))

    execute {
        val experience = experienceCheck.parameterTypes[1]
        val brandSurvey = experienceKind.enumConstant("ANDROID_IN_APP_BRAND_SURVEY")
        val surveyTakeover = placement.enumConstant("ANDROID_SURVEY_TAKEOVER")
        gate(PromptSettings.hideSurveys) {
            experienceLookups.forEach {
                after {
                    whenNotNull(result) {
                        val shown = result.cast(experience)
                        whenEqual(shown.field(experiencePlacement), staticField(surveyTakeover)) { returnNull() }
                        val kind = call(kindOf, shown.field(experienceKindValue))
                        whenEqual(kind, staticField(brandSurvey)) { returnNull() }
                    }
                }
            }
        }
    }
}

private val placement = klass("experiencePlacement") { enumConstants("ANDROID_SURVEY_TAKEOVER", "ANDROID_DOWNLOAD_FEEDBACK") }
private val experienceKind = klass("experienceKind") { enumConstants("ANDROID_IN_APP_BRAND_SURVEY", "SEE_MORE_SIGNAL_ON_DOWNLOAD") }

private val experienceCheck = method("experienceCheck") {
    param(0, placement.descriptor)
    paramCount(2)
    returns(Type.Void)
    calls { returns(experienceKind.descriptor); paramCount(1) }
}

private val kindOf = experienceCheck.point { invokeStatic { returns(experienceKind.descriptor) } }.callee("experienceKindOf")
private val experienceKindValue = experienceCheck.point { calls(kindOf) }.previous { field { type(Type.Int) } }.field("experienceKindValue")
private val experiencePlacement = fieldTarget("experiencePlacement") {
    bytecode.findClass(experienceCheck.parameterTypes[1])!!.instanceFields.single { it.fieldType == placement.descriptor }.ref
}

private val experienceLookups = methods("experienceLookups") {
    params(placement.descriptor)
    returns(experienceCheck.parameterTypes[1])
}
