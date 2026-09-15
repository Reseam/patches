// SPDX-FileCopyrightText: 2026 AunAli K. <hello@auna.li>
// SPDX-License-Identifier: GPL-3.0-or-later

package app.reseam.patches.universal

import app.reseam.patch.BytecodeScope
import app.reseam.patch.ExtClass
import app.reseam.patch.native.MethodRef
import app.reseam.patch.Type
import app.reseam.patch.descriptor
import app.reseam.patch.dex.parameterTypes
import app.reseam.patch.patch
import app.reseam.patch.proto

private const val GMA = "com.google.android.gms.ads"

private object AdMobLoader : ExtClass("app.reseam.universal.googleads.AdMobLoader") {
    val load = static("load", Type.Context, Type.String, "$GMA.AdRequest", "$GMA.AdLoadCallback")
    val loadManager = static("loadManager", Type.Context, Type.String, "$GMA.admanager.AdManagerAdRequest", "$GMA.AdLoadCallback")
}

/** Static full-screen loaders whose fourth argument is the load callback, by request type. */
private val ADMOB_CALLBACK_LOADERS = mapOf(
    AdMobLoader.load to listOf(
        "$GMA.interstitial.InterstitialAd" to "$GMA.interstitial.InterstitialAdLoadCallback",
        "$GMA.rewarded.RewardedAd" to "$GMA.rewarded.RewardedAdLoadCallback",
        "$GMA.rewardedinterstitial.RewardedInterstitialAd" to "$GMA.rewardedinterstitial.RewardedInterstitialAdLoadCallback",
        "$GMA.appopen.AppOpenAd" to "$GMA.appopen.AppOpenAd\$AppOpenAdLoadCallback",
    ),
    AdMobLoader.loadManager to listOf(
        "$GMA.admanager.AdManagerInterstitialAd" to "$GMA.admanager.AdManagerInterstitialAdLoadCallback",
        "$GMA.rewarded.RewardedAd" to "$GMA.rewarded.RewardedAdLoadCallback",
        "$GMA.rewardedinterstitial.RewardedInterstitialAd" to "$GMA.rewardedinterstitial.RewardedInterstitialAdLoadCallback",
    ),
)

/**
 * Public loaders that report through a listener set earlier, so returning without loading leaves
 * the ad slot empty. Each entry is neutered in place; a class or method a given app lacks is
 * skipped, so listing every network and every SDK generation is safe.
 */
private val LOADERS = listOf(
    // AppLovin MAX
    "com.applovin.mediation.ads.MaxAdView" to "loadAd",
    "com.applovin.mediation.ads.MaxInterstitialAd" to "loadAd",
    "com.applovin.mediation.ads.MaxRewardedAd" to "loadAd",
    "com.applovin.mediation.ads.MaxAppOpenAd" to "loadAd",
    "com.applovin.mediation.nativeAds.MaxNativeAdLoader" to "loadAd",
    // Meta Audience Network
    "com.facebook.ads.InterstitialAd" to "loadAd",
    "com.facebook.ads.AdView" to "loadAd",
    "com.facebook.ads.RewardedVideoAd" to "loadAd",
    "com.facebook.ads.RewardedInterstitialAd" to "loadAd",
    "com.facebook.ads.NativeAdBase" to "loadAd",
    // Unity Ads
    "com.unity3d.ads.UnityAds" to "load",
    "com.unity3d.services.banners.BannerView" to "load",
    "com.unity3d.ads.InterstitialAd" to "load",
    "com.unity3d.ads.RewardedAd" to "load",
    "com.unity3d.ads.BannerAd" to "load",
    // ironSource / LevelPlay
    "com.unity3d.mediation.interstitial.LevelPlayInterstitialAd" to "loadAd",
    "com.unity3d.mediation.rewarded.LevelPlayRewardedAd" to "loadAd",
    "com.unity3d.mediation.banner.LevelPlayBannerAdView" to "loadAd",
    "com.unity3d.ironsourceads.interstitial.InterstitialAdLoader" to "loadAd",
    "com.unity3d.ironsourceads.rewarded.RewardedAdLoader" to "loadAd",
    "com.unity3d.ironsourceads.banner.BannerAdLoader" to "loadAd",
    // Pangle
    "com.bytedance.sdk.openadsdk.api.interstitial.PAGInterstitialAd" to "loadAd",
    "com.bytedance.sdk.openadsdk.api.reward.PAGRewardedAd" to "loadAd",
    "com.bytedance.sdk.openadsdk.api.banner.PAGBannerAd" to "loadAd",
    "com.bytedance.sdk.openadsdk.api.open.PAGAppOpenAd" to "loadAd",
    "com.bytedance.sdk.openadsdk.api.nativeAd.PAGNativeAd" to "loadAd",
    // Vungle / Liftoff
    "com.vungle.ads.BaseAd" to "load",
    // InMobi
    "com.inmobi.ads.InMobiInterstitial" to "load",
    "com.inmobi.ads.InMobiBanner" to "load",
    "com.inmobi.ads.InMobiNative" to "load",
    // Mintegral. Newer SDKs route reward/interstitial through the strategy base classes; older
    // ones define load on each concrete handler, so both are listed. Native loaders return
    // boolean, neutered to false.
    "com.mbridge.msdk.out.strategy.base.NonBidAdHandler" to "load",
    "com.mbridge.msdk.out.strategy.base.NonBidAdHandler" to "loadFormSelfFilling",
    "com.mbridge.msdk.out.strategy.base.BidAdHandler" to "loadFromBid",
    "com.mbridge.msdk.out.MBRewardVideoHandler" to "load",
    "com.mbridge.msdk.out.MBRewardVideoHandler" to "loadFormSelfFilling",
    "com.mbridge.msdk.out.MBBidRewardVideoHandler" to "loadFromBid",
    "com.mbridge.msdk.newinterstitial.out.MBNewInterstitialHandler" to "load",
    "com.mbridge.msdk.newinterstitial.out.MBNewInterstitialHandler" to "loadFormSelfFilling",
    "com.mbridge.msdk.newinterstitial.out.MBBidNewInterstitialHandler" to "loadFormSelfFilling",
    "com.mbridge.msdk.newinterstitial.out.MBBidNewInterstitialHandler" to "loadFromBid",
    "com.mbridge.msdk.newinterstitial.out.MBBidInterstitialVideoHandler" to "loadFromBid",
    "com.mbridge.msdk.interstitialvideo.out.MBInterstitialVideoHandler" to "load",
    "com.mbridge.msdk.interstitialvideo.out.MBInterstitialVideoHandler" to "loadFormSelfFilling",
    "com.mbridge.msdk.interstitialvideo.out.MBBidInterstitialVideoHandler" to "loadFromBid",
    "com.mbridge.msdk.out.MBBannerView" to "load",
    "com.mbridge.msdk.out.MBBannerView" to "loadFromBid",
    "com.mbridge.msdk.out.MBNativeAdvancedHandler" to "load",
    "com.mbridge.msdk.out.MBNativeAdvancedHandler" to "loadByToken",
    "com.mbridge.msdk.out.MBSplashHandler" to "preLoad",
    "com.mbridge.msdk.out.MBSplashHandler" to "preLoadByToken",
    "com.mbridge.msdk.out.MBSplashHandler" to "loadAndShow",
    "com.mbridge.msdk.out.MBSplashHandler" to "loadAndShowByToken",
    "com.mbridge.msdk.out.MBInterstitialHandler" to "preload",
    "com.mbridge.msdk.out.MBNativeHandler" to "load",
    "com.mbridge.msdk.out.MBNativeHandler" to "loadFrame",
    "com.mbridge.msdk.out.MBNativeHandler" to "loadMB",
    "com.mbridge.msdk.out.MBNativeHandler" to "loadMBFrame",
    "com.mbridge.msdk.out.MBBidNativeHandler" to "bidLoad",
    "com.mbridge.msdk.out.MBBidNativeHandler" to "loadMB",
    "com.mbridge.msdk.out.MBCommonHandler" to "load",
    // Chartboost
    "com.chartboost.sdk.ads.Interstitial" to "cache",
    "com.chartboost.sdk.ads.Rewarded" to "cache",
    "com.chartboost.sdk.ads.Banner" to "cache",
)

private fun BytecodeScope.neuter(className: String, methodName: String): Int {
    val klass = findClass(className) ?: return 0
    val methods = klass.methods.filter { it.name == methodName }
    for (m in methods) when (m.returnType) {
        Type.Void -> m.alwaysReturn()
        Type.Boolean -> m.alwaysReturn(false)
        else -> m.alwaysReturnNull()
    }
    return methods.size
}

val removeAds = patch("Remove ads") {
    description("Stops the app from loading ads from common ad networks.")

    execute {
        var loaders = 0

        if (bytecode.findClass("$GMA.interstitial.InterstitialAd") != null) {
            for ((target, sources) in ADMOB_CALLBACK_LOADERS) {
                for ((adClass, callback) in sources) {
                    val from = MethodRef(descriptor(adClass), "load", proto(Type.Void, Type.Context, Type.String, target.ref.parameterTypes[2], descriptor(callback)))
                    loaders += bytecode.redirectCalls(from, target)
                }
            }
            loaders += bytecode.neuter("$GMA.BaseAdView", "loadAd")
            loaders += bytecode.neuter("$GMA.AdLoader", "loadAd")
            loaders += bytecode.neuter("$GMA.AdLoader", "loadAds")
        }

        for ((className, methodName) in LOADERS) loaders += bytecode.neuter(className, methodName)

        log.info("Neutralized $loaders ad loader call sites and methods")
    }
}
