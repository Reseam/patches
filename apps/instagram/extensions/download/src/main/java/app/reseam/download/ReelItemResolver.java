// SPDX-FileCopyrightText: 2026 Cossale <hello@auna.li>
// SPDX-License-Identifier: AGPL-3.0-or-later


package app.reseam.instagram.download;

final class ReelItemResolver {
    private ReelItemResolver() {}

    static Object media(Object reelItem) {
        return reelItem == null ? null : MediaMeta.reelItemMedia(reelItem);
    }
}
