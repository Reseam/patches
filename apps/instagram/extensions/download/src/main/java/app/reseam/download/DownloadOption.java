// SPDX-FileCopyrightText: 2026 AunAli K. <hello@auna.li>
// SPDX-License-Identifier: GPL-3.0-or-later


package app.reseam.instagram.download;

@SuppressWarnings({"rawtypes", "unchecked"})
final class DownloadOption {
    private static final String OPTION_CLASS = "com.instagram.feed.media.mediaoption.MediaOption$Option";

    private static Object download;
    private static boolean initialized;

    private DownloadOption() {}

    static boolean isDownload(Object option) {
        ensureInitialized();
        return download != null && download.equals(option);
    }

    private static void ensureInitialized() {
        if (initialized) return;

        try {
            Class optionClass = Class.forName(OPTION_CLASS);
            download = Enum.valueOf(optionClass, "DOWNLOAD");
        } catch (Throwable t) {
            Logcat.e("Failed to resolve DOWNLOAD enum", t);
        }
        initialized = true;
    }
}
