// SPDX-FileCopyrightText: 2026 AunAli K. <hello@auna.li>
// SPDX-FileCopyrightText: 2026 ReVanced contributors
// SPDX-License-Identifier: GPL-3.0-or-later

package app.reseam.youtube.navigation;

import android.app.Activity;
import android.graphics.drawable.Drawable;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import java.lang.ref.WeakReference;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import app.reseam.youtube.core.Logger;

/** Runtime state shared by layout filters that need to know which YouTube surface is visible. */
public final class NavigationBar {

    /** Implemented by YouTube's support Toolbar through the patch-added bridge method. */
    public interface AppCompatToolbarPatchInterface {
        Drawable patch_getNavigationIcon();
    }

    private static final long LATCH_TIMEOUT_MILLISECONDS = 120;
    private static volatile WeakReference<View> searchBarResults = new WeakReference<>(null);
    private static volatile WeakReference<AppCompatToolbarPatchInterface> toolbar = new WeakReference<>(null);
    private static volatile String lastNavigationEnum;
    private static volatile CountDownLatch navigationLatch;
    private static final Map<View, NavigationButton> viewToButton = new WeakHashMap<>();

    static {
        createNavigationLatch();
    }

    private NavigationBar() {}

    public static void searchBarResultsViewLoaded(View view) {
        searchBarResults = new WeakReference<>(view);
        Logger.debug(() -> "Navigation search bar created");
    }

    public static void setToolbar(FrameLayout layout) {
        AppCompatToolbarPatchInterface found = findToolbar(layout);
        if (found == null) {
            Logger.debug(() -> "Navigation toolbar was not found");
            return;
        }
        toolbar = new WeakReference<>(found);
        Logger.debug(() -> "Navigation toolbar created");
    }

    private static AppCompatToolbarPatchInterface findToolbar(View view) {
        if (view instanceof AppCompatToolbarPatchInterface) {
            return (AppCompatToolbarPatchInterface) view;
        }
        if (!(view instanceof ViewGroup)) return null;
        ViewGroup group = (ViewGroup) view;
        for (int i = 0; i < group.getChildCount(); i++) {
            AppCompatToolbarPatchInterface found = findToolbar(group.getChildAt(i));
            if (found != null) return found;
        }
        return null;
    }

    public static boolean isSearchBarActive() {
        View view = searchBarResults.get();
        return view != null && view.isShown();
    }

    public static boolean isBackButtonVisible() {
        AppCompatToolbarPatchInterface current = toolbar.get();
        return current != null && current.patch_getNavigationIcon() != null;
    }

    public static void setLastAppNavigationEnum(Object navigationEnum) {
        if (navigationEnum instanceof Enum<?>) {
            lastNavigationEnum = ((Enum<?>) navigationEnum).name();
            Logger.debug(() -> "Navigation tab created: " + lastNavigationEnum);
        }
    }

    public static void navigationTabLoaded(View tabView) {
        if (tabView == null) return;
        String enumName = lastNavigationEnum;
        for (NavigationButton button : NavigationButton.values()) {
            if (button.matches(enumName)) {
                viewToButton.put(tabView, button);
                Logger.debug(() -> "Navigation tab mapped: " + button);
                return;
            }
        }
        Logger.debug(() -> "Navigation tab unmapped: " + enumName);
    }

    /** Returns the stable button assigned by {@link #navigationTabLoaded(View)}. */
    public static NavigationButton getNavigationButton(View tabView) {
        return viewToButton.get(tabView);
    }

    /** The Create and You tabs are built from image resources rather than the navigation enum. */
    public static void navigationImageResourceTabLoaded(View tabView) {
        Logger.debug(() -> "Navigation image-resource tab created");
        if (NavigationButton.CREATE.matches(lastNavigationEnum)) {
            navigationTabLoaded(tabView);
        } else {
            lastNavigationEnum = NavigationButton.LIBRARY.enumNames.get(0);
            navigationTabLoaded(tabView);
        }
    }

    public static void navigationTabSelected(View view, boolean selected) {
        if (!selected) {
            Logger.debug(() -> "Navigation tab deselected: " + view);
            return;
        }
        NavigationButton button = viewToButton.get(view);
        if (button == null) {
            Logger.debug(() -> "Navigation tab selected but unmapped: " + view);
            NavigationButton.selected = null;
            return;
        }
        NavigationButton.selected = button;
        Logger.debug(() -> "Navigation tab selected: " + button);
        releaseNavigationLatch();
    }

    public static void onBackPressed(Activity activity) {
        Logger.debug(() -> "Navigation back pressed");
        createNavigationLatch();
    }

    private static synchronized void createNavigationLatch() {
        if (navigationLatch != null) navigationLatch.countDown();
        navigationLatch = new CountDownLatch(1);
    }

    private static void releaseNavigationLatch() {
        releaseNavigationLatch(navigationLatch);
    }

    private static synchronized void releaseNavigationLatch(CountDownLatch latch) {
        if (latch != null && latch == navigationLatch) {
            navigationLatch = null;
            latch.countDown();
        }
    }

    private static void awaitNavigationLatch() {
        CountDownLatch latch = navigationLatch;
        if (latch == null) return;
        if (isMainThread()) return;
        try {
            if (!latch.await(LATCH_TIMEOUT_MILLISECONDS, TimeUnit.MILLISECONDS)) {
                releaseNavigationLatch(latch);
                Logger.debug(() -> "Navigation tab update wait timed out");
            }
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            Logger.error(() -> "Navigation tab update wait interrupted: " + exception);
        }
    }

    private static boolean isMainThread() {
        return android.os.Looper.myLooper() == android.os.Looper.getMainLooper();
    }

    public enum NavigationButton {
        HOME("PIVOT_HOME", "TAB_HOME_CAIRO"),
        SHORTS("TAB_SHORTS", "TAB_SHORTS_CAIRO"),
        CREATE("CREATION_TAB_LARGE", "CREATION_TAB_LARGE_CAIRO"),
        EXPLORE("TAB_EXPLORE"),
        SUBSCRIPTIONS("PIVOT_SUBSCRIPTIONS", "TAB_SUBSCRIPTIONS_CAIRO"),
        NOTIFICATIONS("TAB_ACTIVITY", "TAB_ACTIVITY_CAIRO"),
        LIBRARY(
                "YOU_LIBRARY_DUMMY_PLACEHOLDER_NAME",
                "ACCOUNT_CIRCLE",
                "ACCOUNT_CIRCLE_CAIRO",
                "INCOGNITO_CIRCLE",
                "INCOGNITO_CAIRO",
                "VIDEO_LIBRARY_WHITE",
                "PIVOT_LIBRARY"
        );

        private final List<String> enumNames;
        private static volatile NavigationButton selected;

        NavigationButton(String... enumNames) {
            this.enumNames = Arrays.asList(enumNames);
        }

        private boolean matches(String name) {
            return enumNames.contains(name);
        }

        /** Returns the last selected tab, or null until YouTube selects a mapped tab. */
        public static NavigationButton getSelectedNavigationButton() {
            awaitNavigationLatch();
            return selected;
        }
    }
}
