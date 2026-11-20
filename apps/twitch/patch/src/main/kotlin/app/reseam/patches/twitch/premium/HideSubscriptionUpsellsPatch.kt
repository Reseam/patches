// SPDX-FileCopyrightText: 2026 Reseam contributors <https://reseam.app>
// SPDX-License-Identifier: AGPL-3.0-or-later

package app.reseam.patches.twitch.premium

import app.reseam.patch.PatchRuntime
import app.reseam.patch.PointTarget
import app.reseam.patch.Type
import app.reseam.patch.dex.Opcode
import app.reseam.patch.function
import app.reseam.patch.klass
import app.reseam.patch.method
import app.reseam.patch.methods
import app.reseam.patch.patch
import app.reseam.patch.point
import app.reseam.patch.points
import app.reseam.patch.settings.gate
import app.reseam.patch.settings.section
import app.reseam.patch.settings.whenEnabled
import app.reseam.patch.skipWhen
import app.reseam.patches.twitch.TwitchPatchStrings
import app.reseam.patches.twitch.core.TWITCH
import app.reseam.patches.twitch.core.TwitchSettingsPages
import app.reseam.patches.twitch.core.overlayActionButtons
import app.reseam.patches.twitch.core.settingsRowCall
import app.reseam.patches.twitch.core.resourceId
import app.reseam.patches.twitch.core.twitchSettings
import app.reseam.twitch.bindings.Upsells
import app.reseam.twitch.settings.SubscriptionSettings
import app.reseam.twitch.settings.TwitchStrings

// Twitch's R8 rules keep the names of its models.
private const val APP_START_TURBO_OFFER = $$"tv.twitch.android.models.commerce.TurboPurchaseScreen$APP_START_UPSELL"

val hideSubscriptionUpsells = patch(TwitchPatchStrings.hideSubscriptionUpsells) {
    description(TwitchPatchStrings.hideSubscriptionUpsellsDescription)
    compatibleWith(TWITCH)
    settings(twitchSettings, section(TwitchSettingsPages.Ads, TwitchStrings.sectionPromotions, SubscriptionSettings.hideSubscriptionUpsells))

    execute {
        // Turbo subscribers keep the row to manage Turbo, so this skips it only for accounts without Turbo.
        turboSettingsRow().skipWhen {
            val hide = bool(false)
            whenEnabled(SubscriptionSettings.hideSubscriptionUpsells) { hide.assign(param(0)) }
            hide
        }

        gate(SubscriptionSettings.hideSubscriptionUpsells) {
            // The Subscribed badge reads the subscription itself, not these.
            subscriptionEnabled.alwaysReturn(true)
            giftingEnabled.alwaysReturn(true)
            subscribedEnabled.alwaysReturn(true)

            val turboButtons = turboButtonChecks.all
            require(turboButtons.isNotEmpty()) { "The Following and profile headers no longer show a Turbo button" }
            turboButtons.forEach { it.captureAs("shown").after { capture("shown").assign(bool(false)) } }
            turboOfferNavigation.skip()

            turboUpsellButton.alwaysReturnNull()
            theatreTurboUpsell.alwaysReturnNull()
            giftButton.alwaysReturnNull()
            listOf(subscribeButton, promoSubscribeButton).forEach { it.before { whenFalse(Upsells.isSubscribed(param(0))) { returnNull() } } }
            ownProfileActions.setArgument(0, "profile.turboStatus.hasActiveTurbo", true)
            overlayActionButtons.setArgument(0, "hasSubProducts", false)
            log.info("Hid Subscribe, Gift a Sub, the Turbo offers at app start and in settings, and ${turboButtons.size} native Turbo buttons")
        }
    }
}

// R8 renamed this enum but kept the names of its getters.
private val subscriptionState = klass("subscriptionState") { enumConstants("NOT_SUBSCRIBED", "SUBSCRIBED", "GIFTING_ENABLED") }
private val subscriptionEnabled = subscriptionState.method("isSubscriptionEnabled")
private val giftingEnabled = subscriptionState.method("isGiftingEnabled")
private val subscribedEnabled = subscriptionState.method("isSubscribed")

// R8 kept the getter's name on the renamed enum of Turbo button texts.
private val turboButtonChecks = methods("turboButtons") { calls { name("getButtonTextResId") } }
    .points("turboButtonChecks") {
        opcode(Opcode.INSTANCE_OF)
        type(klass("turboButtonShown") { strings("Visible(ingressType=") }.descriptor)
    }

private val turboOfferScreen = klass("turboOfferScreen") { strings("TurboAppStartUpsell") }

// The Turbo trial offer Twitch opens at app start.
private val turboOfferNavigation = method("appStartTurboOffer") {
    accesses { owner(APP_START_TURBO_OFFER); name("INSTANCE") }
    accesses { owner(turboOfferScreen.descriptor) }
}
    .point("turboOfferNavigation") {
        invokeVirtual { returns(Type.Void) }
        argument(2) { field { owner(turboOfferScreen.descriptor) } }
    }

private val turboUpsellButton = function {
    name("TurboUpsellButton")
    paramCount(1)
}

private val theatreTurboUpsell = function {
    name("TheatreSDAUpsell")
    paramCount(1)
}

private val giftButton = function {
    name("GiftButton")
    paramCount(1)
}

private val subscribeButton = function {
    name("SubscribeButton")
    paramCount(1)
}

private val promoSubscribeButton = function {
    name("SubscribeButtonWithPromo")
    paramCount(1)
}

private val ownProfileActions = function {
    name("OwnProfileActions")
    paramCount(1)
}

// The section's first parameter says the account has no Turbo.
private fun PatchRuntime.turboSettingsRow(): PointTarget {
    val turboTitle = resourceId("string", "viewer_account_menu_turbo")
    val turboIcon = resourceId("drawable", "turbo")
    return method("turboSettingsSection") { literals(turboTitle, turboIcon) }
        .point("turboIcon") { literal(turboIcon) }
        .settingsRowCall()
}
