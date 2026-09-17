// SPDX-FileCopyrightText: 2026 AunAli K. <hello@auna.li>
// SPDX-FileCopyrightText: 2026 ReVanced contributors
// SPDX-License-Identifier: GPL-3.0-or-later

package app.reseam.youtube.player;

import java.util.concurrent.atomic.AtomicReference;

import app.reseam.youtube.core.Logger;

/**
 * Which engagement panel, the sheet that slides up under the player, is open. The panels are built
 * from the same litho components as the feed, so a filter can only tell them apart by asking here.
 */
public final class EngagementPanel {
    private static final AtomicReference<String> openPanelId = new AtomicReference<>("");

    private EngagementPanel() {}

    /** Injection point, from the controller, once the panel it was asked for exists. */
    public static void open(String panelId) {
        if (panelId == null || panelId.isEmpty()) return;
        openPanelId.set(panelId);
        Logger.debug(() -> "Engagement panel opened: " + panelId);
    }

    /** Injection point, from the controller, as a panel is torn down. */
    public static void close() {
        String panelId = openPanelId.getAndSet("");
        if (!panelId.isEmpty()) Logger.debug(() -> "Engagement panel closed: " + panelId);
    }

    /** The identifier of the open panel, or "" when none is. */
    public static String openPanelId() {
        return openPanelId.get();
    }
}
