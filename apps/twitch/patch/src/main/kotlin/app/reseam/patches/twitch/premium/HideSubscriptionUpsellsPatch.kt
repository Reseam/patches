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
import app.reseam.patches.twitch.core.settingsRowCall
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
        // Twitch's settings list badges the Turbo row GO AD FREE for accounts without Turbo, and subscribers keep it to manage Turbo.
        // A gate can only skip unconditionally, so this skip reads the toggle itself.
        turboSettingsRow().skipWhen {
            val hide = bool(false)
            whenEnabled(SubscriptionSettings.hideSubscriptionUpsells) { hide.assign(param(0)) }
            hide
        }

        gate(SubscriptionSettings.hideSubscriptionUpsells) {
            // Every Subscribe and Gift a Sub button asks these, while the Subscribed badge reads the subscription itself.
            subscriptionEnabled.alwaysReturn(true)
            giftingEnabled.alwaysReturn(true)
            subscribedEnabled.alwaysReturn(true)

            val turboButtons = turboButtonChecks.all
            require(turboButtons.isNotEmpty()) { "The Following and profile headers no longer show a Turbo button" }
            turboButtons.forEach { it.captureAs("shown").after { capture("shown").assign(bool(false)) } }
            turboOfferNavigation.skip()

            // The React Native home, theatre and profile screens draw their own buttons.
            turboUpsellButton.alwaysReturnNull()
            theatreTurboUpsell.alwaysReturnNull()
            giftButton.alwaysReturnNull()
            listOf(subscribeButton, promoSubscribeButton).forEach { it.before { whenFalse(Upsells.isSubscribed(param(0))) { returnNull() } } }
            ownProfileActions.setArgument(0, "profile.turboStatus.hasActiveTurbo", true)
            log.info("Hid Subscribe, Gift a Sub, the Turbo offers at app start and in settings, and ${turboButtons.size} native Turbo buttons")
        }
    }
}

// R8 renamed this enum but kept the names of its getters.
private val subscriptionState = klass("subscriptionState") { enumConstants("NOT_SUBSCRIBED", "SUBSCRIBED", "GIFTING_ENABLED") }
private val subscriptionEnabled = subscriptionState.method("isSubscriptionEnabled")
private val giftingEnabled = subscriptionState.method("isGiftingEnabled")
private val subscribedEnabled = subscriptionState.method("isSubscribed")

// R8 renamed the enum of Turbo button texts but kept the name of its getter.
private val turboButtonChecks = methods("turboButtons") { calls { name("getButtonTextResId") } }
    .points("turboButtonChecks") {
        opcode(Opcode.INSTANCE_OF)
        // The Try Ad-Free and Go Ad-Free buttons show only for this state.
        type(klass("turboButtonShown") { strings("Visible(ingressType=") }.descriptor)
    }

private val turboOfferScreen = klass("turboOfferScreen") { strings("TurboAppStartUpsell") }

// At app start, Twitch opens the Turbo trial offer when the account can still take it.
private val turboOfferNavigation = method("appStartTurboOffer") {
    accesses { owner(APP_START_TURBO_OFFER); name("INSTANCE") }
    accesses { owner(turboOfferScreen.descriptor) }
}
    .point("turboOfferNavigation") {
        invokeVirtual { returns(Type.Void) }
        argument(2) { field { owner(turboOfferScreen.descriptor) } }
    }

// The Try Ad-Free button in the Following page's Live Now header.
private val turboUpsellButton = function("turboUpsellButton") {
    name("TurboUpsellButton")
    strings("useTurboUpsell")
    paramCount(1)
}

// The Turbo offer the theatre shows over stream display ads.
private val theatreTurboUpsell = function("theatreTurboUpsell") {
    name("TheatreSDAUpsell")
    strings("useTurboUpsell")
    paramCount(1)
}

private val giftButton = function("giftButton") {
    name("GiftButton")
    strings("onOpenPanel")
    paramCount(1)
}

private val subscribeButton = function("subscribeButton") {
    name("SubscribeButton")
    strings("isSubscribed", "onOpenPanel")
    paramCount(1)
}

private val promoSubscribeButton = function("promoSubscribeButton") {
    name("SubscribeButtonWithPromo")
    strings("useSubscribeButtonPromoImpression")
    paramCount(1)
}

// Your own profile page offers Turbo unless the profile reports an active Turbo subscription.
private val ownProfileActions = function("ownProfileActions") {
    name("OwnProfileActions")
    strings("turboStatus", "hasActiveTurbo")
    paramCount(1)
}

// Resource ids exist only at patch time. The section draws the Subscriptions, Drops and Turbo rows, and its first parameter says the account has no Turbo.
private fun PatchRuntime.turboSettingsRow(): PointTarget {
    val turboTitle = resources.id("string", "viewer_account_menu_turbo")?.toLong() ?: error("The app has no string/viewer_account_menu_turbo")
    val turboIcon = resources.id("drawable", "turbo")?.toLong() ?: error("The app has no drawable/turbo")
    return method("turboSettingsSection") { literals(turboTitle, turboIcon) }
        .point("turboIcon") { literal(turboIcon) }
        .settingsRowCall()
}
