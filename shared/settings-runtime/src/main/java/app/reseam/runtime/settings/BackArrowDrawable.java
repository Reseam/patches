// SPDX-FileCopyrightText: 2026 AunAli K. <hello@auna.li>
// SPDX-License-Identifier: GPL-3.0-or-later

package app.reseam.runtime.settings;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PixelFormat;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;

/**
 * Material's "arrow back" glyph, drawn from its path. The framework's own arrow is not a public
 * resource and the host app's is obfuscated, so the runtime cannot rely on finding one.
 */
final class BackArrowDrawable extends Drawable {

    private static final float GRID = 24f;

    private static final Path GLYPH = new Path();

    static {
        GLYPH.moveTo(20f, 11f);
        GLYPH.lineTo(7.83f, 11f);
        GLYPH.lineTo(13.42f, 5.41f);
        GLYPH.lineTo(12f, 4f);
        GLYPH.lineTo(4f, 12f);
        GLYPH.lineTo(12f, 20f);
        GLYPH.lineTo(13.41f, 18.59f);
        GLYPH.lineTo(7.83f, 13f);
        GLYPH.lineTo(20f, 13f);
        GLYPH.close();
    }

    private final int size;
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Path scaled = new Path();

    BackArrowDrawable(int size) {
        this.size = size;
        paint.setColor(Color.WHITE);
        paint.setStyle(Paint.Style.FILL);
    }

    @Override
    public void draw(Canvas canvas) {
        RectF bounds = new RectF(getBounds());
        Matrix matrix = new Matrix();
        matrix.setRectToRect(new RectF(0f, 0f, GRID, GRID), bounds, Matrix.ScaleToFit.CENTER);
        GLYPH.transform(matrix, scaled);
        canvas.drawPath(scaled, paint);
    }

    @Override
    public int getIntrinsicWidth() {
        return size;
    }

    @Override
    public int getIntrinsicHeight() {
        return size;
    }

    @Override
    public void setAlpha(int alpha) {
        paint.setAlpha(alpha);
        invalidateSelf();
    }

    @Override
    public void setColorFilter(ColorFilter colorFilter) {
        paint.setColorFilter(colorFilter);
        invalidateSelf();
    }

    @Override
    public int getOpacity() {
        return PixelFormat.TRANSLUCENT;
    }
}
