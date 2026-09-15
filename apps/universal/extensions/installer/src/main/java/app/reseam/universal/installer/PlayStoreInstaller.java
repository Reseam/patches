// SPDX-FileCopyrightText: 2026 AunAli K. <hello@auna.li>
// SPDX-License-Identifier: GPL-3.0-or-later

package app.reseam.universal.installer;

import android.content.pm.InstallSourceInfo;
import android.content.pm.PackageManager;

import java.util.Collections;
import java.util.Set;
import java.util.WeakHashMap;

public final class PlayStoreInstaller {
    private static final String PLAY_STORE = "com.android.vending";
    private static final Set<InstallSourceInfo> OWN = Collections.synchronizedSet(Collections.newSetFromMap(new WeakHashMap<InstallSourceInfo, Boolean>()));

    private PlayStoreInstaller() {}

    public static String ownPackage() { return null; }

    public static String getInstallerPackageName(PackageManager manager, String packageName) {
        String installer = manager.getInstallerPackageName(packageName);
        return packageName.equals(ownPackage()) ? PLAY_STORE : installer;
    }

    public static InstallSourceInfo getInstallSourceInfo(PackageManager manager, String packageName) throws PackageManager.NameNotFoundException {
        InstallSourceInfo info = manager.getInstallSourceInfo(packageName);
        if (packageName.equals(ownPackage())) OWN.add(info);
        return info;
    }

    public static String getInstallingPackageName(InstallSourceInfo info) {
        return OWN.contains(info) ? PLAY_STORE : info.getInstallingPackageName();
    }

    public static String getInitiatingPackageName(InstallSourceInfo info) {
        return OWN.contains(info) ? PLAY_STORE : info.getInitiatingPackageName();
    }
}
