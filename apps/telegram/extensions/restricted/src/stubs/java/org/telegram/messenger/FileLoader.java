// SPDX-FileCopyrightText: 2026 Reseam contributors <https://reseam.app>
// SPDX-License-Identifier: AGPL-3.0-or-later

package org.telegram.messenger;

import java.io.File;
import java.util.ArrayList;

import org.telegram.tgnet.TLObject;
import org.telegram.tgnet.TLRPC;

public class FileLoader {
    public static FileLoader getInstance(int num) { return null; }
    public static String getAttachFileName(TLObject attach) { return null; }
    public static TLRPC.PhotoSize getClosestPhotoSizeWithSize(ArrayList<TLRPC.PhotoSize> sizes, int side, boolean byMinSide, TLRPC.PhotoSize toIgnore, boolean ignoreStripped) { return null; }
    public File getPathToMessage(TLRPC.Message message) { return null; }
    public void loadFile(TLRPC.Document document, Object parentObject, int priority, int cacheType) {}
    public void loadFile(ImageLocation imageLocation, Object parentObject, String ext, int priority, int cacheType) {}
}
