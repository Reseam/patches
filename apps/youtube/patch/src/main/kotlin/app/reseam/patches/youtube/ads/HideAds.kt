// SPDX-FileCopyrightText: 2026 AunAli K. <hello@auna.li>
// SPDX-FileCopyrightText: 2026 ReVanced contributors
// SPDX-License-Identifier: GPL-3.0-or-later

package app.reseam.patches.youtube.ads

import app.reseam.patch.ExtClass
import app.reseam.patch.Type
import app.reseam.patch.klass
import app.reseam.patch.method
import app.reseam.patch.patch
import app.reseam.patch.point
import app.reseam.patch.points
import app.reseam.patch.settings.gate
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
import app.reseam.patches.youtubecommon.hideById
import app.reseam.patches.youtubecommon.lithoFilter
import app.reseam.patches.youtube.internal.overrideClientContextOsName
import app.reseam.patches.youtubecommon.registerLithoFilter
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
        gate(YouTubeSettings.hideFullscreenAds) {
            dialogBuilder.points("fullscreenDialogShown") {
                invokeVirtual {
                    ownerAssignableTo("android.app.Dialog")
                    name("show")
                    params()
                    returns(Type.Void)
                }
            }.single().captureArgumentAs("dialog", 0).after {
                call(Ads.closeFullscreenAd, capture("dialog"), param(0))
            }
        }

        gate(YouTubeSettings.hideYouTubePremiumPromotions) {
            premiumViewMeasured.after {
                thisObject.callVirtual("android.view.View", "setMeasuredDimension", "(II)V", int(0), int(0))
            }
        }

        // Newer R8 builds merge this callback into a dispatcher for unrelated player events.
        // Return only from the shelf branch, never from the shared method's entry.
        gate(YouTubeSettings.hideGeneralAds) {
            playerOverlayTimelyShelf
                .point("timelyShelfBranch") { string("player_overlay_timely_shelf") }
                .after { returnVoid() }
        }

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

        hideById("ad_attribution", YouTubeSettings.hideGeneralAds)
        hideById("paid_promotion_label_text_view", YouTubeSettings.hidePaidPromotionLabel)

        gate(YouTubeSettings.hidePlayerPopupAds) {
            engagementPanelShown.before {
                whenTrue(call(Ads.isPlayerPopupAd, capture("panel").field(engagementPanelId))) {
                    returnNull()
                }
            }
        }
    }
}

private object AdsFilter : ExtClass("app.reseam.youtube.ads.AdsFilter")

private object Ads : ExtClass("app.reseam.youtube.ads.Ads") {
    val closeFullscreenAd by static("android.app.Dialog", "[B")
    val isStoreBanner by static("[B", returns = Type.Boolean)
    val isPlayerPopupAd by static(Type.String, returns = Type.Boolean)
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
