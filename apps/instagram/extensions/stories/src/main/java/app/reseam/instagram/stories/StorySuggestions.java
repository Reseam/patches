// SPDX-FileCopyrightText: 2026 AunAli K. <hello@auna.li>
// SPDX-License-Identifier: GPL-3.0-or-later

package app.reseam.instagram.stories;

public final class StorySuggestions {
    private StorySuggestions() {}

    /** The server's reel_type names survive obfuscation; the enum's field names do not. */
    public static boolean isSuggested(Enum<?> reelType) {
        if (reelType == null) return false;
        switch (reelType.name()) {
            case "SUGGESTED_USER":
            case "SUGGESTED_USER_REEL":
            case "SUGGESTED_CREATOR_REEL":
                return true;
            default:
                return false;
        }
    }
}
