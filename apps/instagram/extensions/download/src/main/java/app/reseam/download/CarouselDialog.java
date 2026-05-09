// SPDX-FileCopyrightText: 2026 AunAli K. <hello@auna.li>
// SPDX-License-Identifier: GPL-3.0-or-later


package app.reseam.instagram.download;

import android.app.AlertDialog;
import android.content.Context;

final class CarouselDialog {
    interface Callbacks {
        void onCurrent();
        void onAll();
    }

    private CarouselDialog() {}

    static void show(Context context, int totalSlides, int currentSlide, Callbacks callbacks) {
        if (context == null) {
            callbacks.onAll();
            return;
        }

        boolean hasCurrent = currentSlide >= 0 && currentSlide < totalSlides;
        String currentLabel = hasCurrent
                ? "Current (" + (currentSlide + 1) + "/" + totalSlides + ")"
                : null;

        AlertDialog.Builder builder = new AlertDialog.Builder(context)
                .setTitle("Carousel post")
                .setMessage("This post has " + totalSlides + " items.")
                .setPositiveButton("All " + totalSlides, (d, w) -> callbacks.onAll())
                .setNegativeButton("Cancel", null);

        if (hasCurrent) {
            builder.setNeutralButton(currentLabel, (d, w) -> callbacks.onCurrent());
        }

        builder.show();
    }
}
