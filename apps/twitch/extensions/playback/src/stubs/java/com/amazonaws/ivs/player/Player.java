// SPDX-FileCopyrightText: 2026 Reseam contributors <https://reseam.app>
// SPDX-License-Identifier: AGPL-3.0-or-later

package com.amazonaws.ivs.player;

import java.util.Set;

public interface Player {
    Set<Quality> getQualities();

    String getSessionId();

    boolean isAutoQualityMode();

    void setAutoQualityMode(boolean enabled);

    void setQuality(Quality quality);
}
