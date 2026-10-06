// SPDX-FileCopyrightText: 2026 Cossale <hello@auna.li>
// SPDX-License-Identifier: AGPL-3.0-or-later

package com.airbnb.lottie;

import java.io.InputStream;

/** Compile-time view of the Lottie class already shipped by YouTube. */
public class LottieAnimationView {
    public void patch_setAnimation(int resourceId) {}

    public void patch_setAnimation(InputStream stream, String cacheKey) {}
}
