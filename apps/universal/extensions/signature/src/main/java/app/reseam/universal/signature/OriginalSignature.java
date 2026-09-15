// SPDX-FileCopyrightText: 2026 AunAli K. <hello@auna.li>
// SPDX-License-Identifier: GPL-3.0-or-later

package app.reseam.universal.signature;

import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.Signature;
import android.content.pm.SigningInfo;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.Collections;
import java.util.Set;
import java.util.WeakHashMap;

public final class OriginalSignature {
    private static final Set<SigningInfo> OWN = Collections.synchronizedSet(Collections.newSetFromMap(new WeakHashMap<SigningInfo, Boolean>()));
    private static volatile Signature[] signatures;

    private OriginalSignature() {}

    public static String ownPackage() { return null; }

    public static String certificates() { return null; }

    public static PackageInfo getPackageInfo(PackageManager manager, String packageName, int flags) throws PackageManager.NameNotFoundException {
        return spoofed(manager.getPackageInfo(packageName, flags));
    }

    public static PackageInfo getPackageInfo(PackageManager manager, String packageName, PackageManager.PackageInfoFlags flags) throws PackageManager.NameNotFoundException {
        return spoofed(manager.getPackageInfo(packageName, flags));
    }

    public static boolean hasSigningCertificate(PackageManager manager, String packageName, byte[] certificate, int type) {
        if (!packageName.equals(ownPackage())) return manager.hasSigningCertificate(packageName, certificate, type);
        for (Signature signature : signatures()) {
            byte[] candidate = type == PackageManager.CERT_INPUT_SHA256 ? sha256(signature.toByteArray()) : signature.toByteArray();
            if (Arrays.equals(candidate, certificate)) return true;
        }
        return false;
    }

    public static Signature[] getApkContentsSigners(SigningInfo info) {
        return OWN.contains(info) ? signatures().clone() : info.getApkContentsSigners();
    }

    public static Signature[] getSigningCertificateHistory(SigningInfo info) {
        return OWN.contains(info) ? signatures().clone() : info.getSigningCertificateHistory();
    }

    public static boolean hasMultipleSigners(SigningInfo info) {
        return OWN.contains(info) ? signatures().length > 1 : info.hasMultipleSigners();
    }

    private static PackageInfo spoofed(PackageInfo info) {
        if (!info.packageName.equals(ownPackage())) return info;
        if (info.signatures != null) info.signatures = signatures().clone();
        if (info.signingInfo != null) OWN.add(info.signingInfo);
        return info;
    }

    private static Signature[] signatures() {
        if (signatures == null) {
            String[] encoded = certificates().split(",");
            Signature[] decoded = new Signature[encoded.length];
            for (int i = 0; i < encoded.length; i++) decoded[i] = new Signature(encoded[i]);
            signatures = decoded;
        }
        return signatures;
    }

    private static byte[] sha256(byte[] data) {
        try {
            return MessageDigest.getInstance("SHA-256").digest(data);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}
