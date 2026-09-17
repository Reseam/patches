// SPDX-FileCopyrightText: 2026 AunAli K. <hello@auna.li>
// SPDX-FileCopyrightText: 2026 ReVanced contributors
// SPDX-License-Identifier: GPL-3.0-or-later

package app.reseam.youtube.quality;

import java.lang.reflect.Array;

import app.reseam.youtube.core.Logger;
import app.reseam.youtube.core.Settings;
import app.reseam.youtube.video.VideoInformation;

/** Removes Premium formats without a stream or array lambda. */
public final class HidePremiumVideoQuality {
    private HidePremiumVideoQuality() {}

    public static VideoInformation.VideoQualityInterface[] hidePremiumVideoQuality(
            VideoInformation.VideoQualityInterface[] qualities) {
        if (!Settings.getBoolean("hide_premium_video_quality", true) || qualities == null) return qualities;
        try {
            int count = 0;
            for (VideoInformation.VideoQualityInterface quality : qualities) {
                if (quality != null && !VideoInformation.isPremiumVideoQuality(quality)) count++;
            }
            if (count == qualities.length) return qualities;
            // The array goes back into YouTube's own VideoQuality[] field, so it keeps that runtime type.
            VideoInformation.VideoQualityInterface[] filtered = (VideoInformation.VideoQualityInterface[])
                    Array.newInstance(qualities.getClass().getComponentType(), count);
            int index = 0;
            for (VideoInformation.VideoQualityInterface quality : qualities) {
                if (quality != null && !VideoInformation.isPremiumVideoQuality(quality)) filtered[index++] = quality;
            }
            return filtered;
        } catch (Exception exception) {
            Logger.error(() -> "Hide Premium quality failure: " + exception);
            return qualities;
        }
    }
}
