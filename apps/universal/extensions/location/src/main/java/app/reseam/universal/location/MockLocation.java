// SPDX-FileCopyrightText: 2026 Cossale <hello@auna.li>
// SPDX-License-Identifier: AGPL-3.0-or-later

package app.reseam.universal.location;

import android.location.Location;

public final class MockLocation {
    private MockLocation() {}

    public static boolean isFromMockProvider(Location location) {
        return false;
    }

    public static boolean isMock(Location location) {
        return false;
    }
}
