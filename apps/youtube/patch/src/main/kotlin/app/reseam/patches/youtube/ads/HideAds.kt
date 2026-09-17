// SPDX-FileCopyrightText: 2026 AunAli K. <hello@auna.li>
// SPDX-FileCopyrightText: 2026 ReVanced contributors
// SPDX-License-Identifier: GPL-3.0-or-later

package app.reseam.patches.youtube.ads

import app.reseam.patch.ExtClass
import app.reseam.patch.PatchRuntime
import app.reseam.patch.Type
import app.reseam.patch.klass
import app.reseam.patch.method
import app.reseam.patch.methods
import app.reseam.patch.patch
import app.reseam.patch.point
import app.reseam.patch.points
import app.reseam.patch.settings.ToggleSetting
import app.reseam.patch.settings.after
import app.reseam.patch.settings.before
import app.reseam.patch.settings.section
import app.reseam.patch.settings.whenEnabled
import app.reseam.patch.skipWhen
import app.reseam.patches.youtube.core.YOUTUBE
import app.reseam.patches.youtube.core.YouTubeSettings
import app.reseam.patches.youtube.core.YouTubeSettingsPages
import app.reseam.patches.youtube.core.youTubeSettings
import app.reseam.patches.youtube.internal.ClientContextEndpoint
import app.reseam.patches.youtube.internal.clientContextHook
import app.reseam.patches.youtube.internal.engagementPanelHook
import app.reseam.patches.youtube.internal.engagementPanelId
import app.reseam.patches.youtube.internal.engagementPanelShown
import app.reseam.patches.youtube.internal.fixBackToExitGesture
import app.reseam.patches.youtube.internal.lithoFilter
import app.reseam.patches.youtube.internal.overrideClientContextOsName
import app.reseam.patches.youtube.internal.registerLithoFilter
import app.reseam.patches.youtube.internal.verticalScrollFix

val hideAds = patch("Hide ads") {
    description("Removes general ads and promotional components.")
    compatibleWith(YOUTUBE)
    dependsOn(
        lithoFilter,
        engagementPanelHook,
        clientContextHook,
        verticalScrollFix,
        fixBackToExitGesture,
    )
    settings(
        youTubeSettings,
        section(
            YouTubeSettingsPages.Ads,
            "Ads",
            YouTubeSettings.hideEndScreenStoreBanner,
            YouTubeSettings.hideFullscreenAds,
            YouTubeSettings.hideGeneralAds,
            YouTubeSettings.hideMerchandiseBanners,
            YouTubeSettings.hidePaidPromotionLabel,
            YouTubeSettings.hidePlayerPopupAds,
            YouTubeSettings.hideSelfSponsorAds,
            YouTubeSettings.hideShoppingLinks,
            YouTubeSettings.hideViewProductsBanner,
            YouTubeSettings.hideYouTubePremiumPromotions,
        ),
    )

    execute {
        registerLithoFilter(AdsFilter)
        overrideClientContextOsName(ClientContextEndpoint.BROWSE, YouTubeSettings.hideGeneralAds, "Android Automotive")
        overrideClientContextOsName(ClientContextEndpoint.SEARCH, YouTubeSettings.hideGeneralAds, "Android Automotive")

        val dialogStyle = resources.id("style", "SlidingDialogAnimation")?.toLong()
            ?: error("style/SlidingDialogAnimation is missing")
        val dialogBuilder = method("fullscreenDialogBuilder") {
            param(0, "[B")
            paramCount(2)
            returns(Type.Void)
            literals(dialogStyle)
            calls { owner("android.view.Window"); name("setWindowAnimations"); params(Type.Int); returns(Type.Void) }
        }
        dialogBuilder.points("fullscreenDialogShown") {
            invokeVirtual {
                ownerAssignableTo("android.app.Dialog")
                name("show")
                params()
                returns(Type.Void)
            }
        }.single().captureArgumentAs("dialog", 0).after(YouTubeSettings.hideFullscreenAds) {
            call(Ads.closeFullscreenAd, capture("dialog"), param(0))
        }

        premiumViewMeasured.after(YouTubeSettings.hideYouTubePremiumPromotions) {
            thisObject.callVirtual("android.view.View", "setMeasuredDimension", "(II)V", int(0), int(0))
        }

        // Newer R8 builds merge this callback into a dispatcher for unrelated player events.
        // Return only from the shelf branch, never from the shared method's entry.
        playerOverlayTimelyShelf
            .point("timelyShelfBranch") { string("player_overlay_timely_shelf") }
            .after(YouTubeSettings.hideGeneralAds) { returnVoid() }

        val adContainerId = resources.id("id", "fullscreen_engagement_ad_container")?.toLong()
            ?: error("id/fullscreen_engagement_ad_container is missing")
        val adContainer = method("fullscreenEngagementAdContainer") {
            params()
            returns(Type.Void)
            literals(adContainerId)
        }
        // The store item comes from this controller's fields; the other additions are decoded renderers.
        val storeBannerAdd = adContainer.points("storeBannerAdd") {
            invokeVirtual {
                name("add")
                params(Type.Object)
                returns(Type.Boolean)
            }
            argument(1) { field { owner(adContainer.owner) } }
        }.single()
        val storeItemType = storeBannerAdd.writer(1).field().type
        val serializeStoreItem = klass(storeItemType).method("toByteArray", inherited = true) {
            params()
            returns("[B")
        }
        storeBannerAdd.captureArgumentAs("item", 1).skipWhen {
            val hide = bool(false)
            whenEnabled(YouTubeSettings.hideEndScreenStoreBanner) {
                hide.assign(call(Ads.isStoreBanner, capture("item").cast(storeItemType).call(serializeStoreItem)))
            }
            hide
        }

        hideResourceViews("ad_attribution", YouTubeSettings.hideGeneralAds)
        hideResourceViews("paid_promotion_label_text_view", YouTubeSettings.hidePaidPromotionLabel)

        engagementPanelShown.before(YouTubeSettings.hidePlayerPopupAds) {
            whenTrue(call(Ads.isPlayerPopupAd, capture("panel").field(engagementPanelId))) {
                returnNull()
            }
        }
    }
}

private object AdsFilter : ExtClass("app.reseam.youtube.ads.AdsFilter")

private object Ads : ExtClass("app.reseam.youtube.ads.Ads") {
    val closeFullscreenAd = static("closeFullscreenAd", "android.app.Dialog", "[B")
    val isStoreBanner = static("isStoreBanner", "[B", returns = Type.Boolean)
    val hideView = static("hideView", Type.View)
    val isPlayerPopupAd = static("isPlayerPopupAd", Type.String, returns = Type.Boolean)
}

/** Resource arguments identify the view; the same hook handles attribution and paid labels. */
private fun PatchRuntime.hideResourceViews(resourceName: String, setting: ToggleSetting) {
    val id = resources.id("id", resourceName)?.toLong() ?: error("id/$resourceName is missing")
    val lookups = methods("$resourceName views") { literals(id) }.points("$resourceName lookup") {
        invokeVirtual { name("findViewById"); params(Type.Int); returns(Type.View) }
        argument(1) { literal(id) }
    }
    check(lookups.all.isNotEmpty()) { "No $resourceName view lookups found" }
    lookups.forEach {
        next { resultOf(Type.View) }.captureAs("view").after(setting) {
            call(Ads.hideView, capture("view"))
        }
    }
    log.debug("$resourceName: hooked ${lookups.all.size} resource-backed lookups")
}

private val premiumViewMeasured = method("premiumViewMeasured") {
    inClass(klass("com.google.android.apps.youtube.app.red.presenter.CompactYpcOfferModuleView"))
    name("onMeasure")
    params(Type.Int, Type.Int)
    returns(Type.Void)
}

private val playerOverlayTimelyShelf = method("playerOverlayTimelyShelf") {
    params(Type.Object)
    returns(Type.Void)
    strings("player_overlay_timely_shelf")
}
