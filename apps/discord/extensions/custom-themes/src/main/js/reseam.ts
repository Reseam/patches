// SPDX-FileCopyrightText: 2026 Reseam contributors <https://reseam.app>
// SPDX-License-Identifier: AGPL-3.0-or-later

import type { ThemeDocument } from "./theme";

export const RESEAM_THEME_ID = "reseam";

const RESEAM_MINT = {
    "0": "#000000", "10": "#002111", "20": "#003920", "30": "#075232", "40": "#296A48", "50": "#43845F",
    "60": "#5D9E78", "70": "#77B991", "80": "#92D5AB", "90": "#AEF2C6", "95": "#C0FFD6", "99": "#F5FFF5", "100": "#FFFFFF",
};

export const RESEAM_THEME_DOCUMENT: ThemeDocument = {
    format: "reseam.discord-theme",
    version: 1,
    name: "Reseam",
    author: "Reseam",
    base: "darker",
    palettes: {
        neutral: {
            "0": "#000000", "4": "#0E0E0E", "6": "#131313", "10": "#1B1B1B", "12": "#1F1F1F", "17": "#2A2A2A",
            "20": "#303030", "22": "#353535", "24": "#393939", "30": "#474747", "40": "#5E5E5E", "50": "#777777",
            "60": "#919191", "70": "#ABABAB", "80": "#C6C6C6", "87": "#DADADA", "90": "#E2E2E2", "92": "#E8E8E8",
            "94": "#EEEEEE", "95": "#F1F1F1", "96": "#F3F3F3", "98": "#F9F9F9", "99": "#FCFCFC", "100": "#FFFFFF",
        },
        brand: RESEAM_MINT,
        green: RESEAM_MINT,
        red: {
            "0": "#000000", "10": "#410004", "20": "#67040D", "30": "#871F20", "40": "#A73735", "50": "#C84F4B",
            "60": "#E96862", "70": "#FF8982", "80": "#FFB3AE", "90": "#FFDAD7", "95": "#FFEDEB", "99": "#FFFBFF", "100": "#FFFFFF",
        },
        yellow: {
            "0": "#000000", "10": "#271900", "20": "#422C00", "30": "#5F4100", "40": "#7D5700", "50": "#9D6E00",
            "60": "#BD8711", "70": "#DBA12F", "80": "#F9BC49", "90": "#FFDEAA", "95": "#FFEED9", "99": "#FFFBFF", "100": "#FFFFFF",
        },
        blue: {
            "0": "#000000", "10": "#001E2B", "20": "#003547", "30": "#004D66", "40": "#006686", "50": "#2780A3",
            "60": "#489ABE", "70": "#65B5DA", "80": "#82D0F7", "90": "#C0E8FF", "95": "#E1F3FF", "99": "#FAFCFF", "100": "#FFFFFF",
        },
    },
    // Discord draws white text and icons on brand fills, so fills take mint 40, not the light primary.
    raw: {
        NEUTRAL_92: "#0A0A0A",
        NEUTRAL_86: "#111111",
        NEUTRAL_82: "#171717",
        NEUTRAL_79: "#1F1F1F",
        NEUTRAL_73: "#262626",
        NEUTRAL_5: "#EEEEEE",
        NEUTRAL_23: "#A1A1A1",
        BLURPLE_50: "#296A48",
        BLURPLE_65: "#075232",
        BLURPLE_36: "#A2EBBD",
        BRAND_500: "#296A48",
        BRAND_560: "#075232",
        BRAND_360: "#A2EBBD",
    },
    background: { colors: ["#43845F", "#296A48", "#075232"], angle: 0, mix: 100 },
    semantic: {
        MENTION_FOREGROUND: "#AEF2C6",
    },
};
