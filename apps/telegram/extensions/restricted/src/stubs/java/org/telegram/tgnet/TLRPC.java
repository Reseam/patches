// SPDX-FileCopyrightText: 2026 Reseam contributors <https://reseam.app>
// SPDX-License-Identifier: AGPL-3.0-or-later

package org.telegram.tgnet;

import java.util.ArrayList;

public class TLRPC {
    public static class Message extends TLObject {
        public boolean noforwards;
    }
    public static class Chat extends TLObject {
        public boolean noforwards;
    }
    public abstract static class Photo extends TLObject {
        public byte[] file_reference;
        public int date;
        public ArrayList<PhotoSize> sizes;
        public ArrayList<Object> video_sizes;
        public boolean has_stickers;
    }
    public abstract static class PhotoSize extends TLObject {}
    public abstract static class Document extends TLObject {
        public byte[] file_reference;
        public int date;
        public String mime_type;
        public long size;
        public ArrayList<PhotoSize> thumbs;
        public ArrayList<Object> video_thumbs;
        public ArrayList<Object> attributes;
        public String file_name;
    }
    public static class TL_photo extends Photo {}
    public static class TL_document extends Document {}
}
