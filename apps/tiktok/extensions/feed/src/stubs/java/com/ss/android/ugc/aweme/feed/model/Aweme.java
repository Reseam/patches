// SPDX-FileCopyrightText: 2026 Reseam contributors <https://reseam.app>
// SPDX-License-Identifier: AGPL-3.0-or-later

package com.ss.android.ugc.aweme.feed.model;

import com.ss.android.ugc.aweme.commerce.AwemeCommerceStruct;
import com.ss.android.ugc.aweme.feed.AIGCInfo;
import com.ss.android.ugc.aweme.profile.model.User;

public abstract class Aweme {
    public abstract boolean isAd();
    public abstract boolean isSoftAd();
    public abstract int getAwemeType();
    public abstract boolean isPaidContent();
    public abstract boolean isSubOnlyVideo();
    public abstract boolean getIsTikTokStory();
    public abstract boolean getIsLiveHasProduct();
    public abstract int getProductsCount();
    public abstract AIGCInfo getAigcInfo();
    public abstract AwemeCommerceStruct getCommerceVideoAuthInfo();
    public abstract MixStruct getMixInfo();
    public abstract String getDesc();
    public abstract String getRegion();
    public abstract long getCreateTime();
    public abstract AwemeStatistics getStatistics();
    public abstract Video getVideo();
    public abstract User getAuthor();
}
