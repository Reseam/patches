// SPDX-FileCopyrightText: 2026 Cossale <hello@auna.li>
// SPDX-License-Identifier: AGPL-3.0-or-later


package app.reseam.instagram.download;

import android.content.Context;

final class StoryOwnerResolver {
    private StoryOwnerResolver() {}

    static Object reelItem(Object owner) {
        return owner == null ? null : MediaMeta.storyOwnerReelItem(owner);
    }

    static Context context(Object owner) {
        if (owner == null) return null;
        Object ctx = MediaMeta.storyOwnerContext(owner);
        return ctx instanceof Context ? (Context) ctx : null;
    }
}
