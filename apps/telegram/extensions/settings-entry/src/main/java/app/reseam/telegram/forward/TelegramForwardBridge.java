// SPDX-FileCopyrightText: 2026 AunAli K. <hello@auna.li>
// SPDX-License-Identifier: GPL-3.0-or-later

package app.reseam.telegram.forward;

import android.util.Log;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import app.reseam.runtime.settings.ReseamSettings;
import org.telegram.messenger.FileLoader;
import org.telegram.messenger.MessageObject;
import org.telegram.messenger.MessageSuggestionParams;
import org.telegram.messenger.MessagesController;
import org.telegram.messenger.SendMessagesHelper;
import org.telegram.tgnet.TLRPC;

public final class TelegramForwardBridge {
    private static final String TAG = "ReseamForward";
    private TelegramForwardBridge() {}

    /**
     * Hook target: invoked at the head of {@code SendMessagesHelper.sendMessage(ArrayList, ...)}.
     * If the source message is from a no-forwards chat, re-sends each item via the helper's
     * {@code processForwardFromMyName} (which uploads as new messages, no server-side block).
     * Returns true when this path handled the call so the patched method can early-return.
     */
    public static boolean tryFakeForward(
            ArrayList<?> messages,
            long peer,
            long payStars,
            long monoForumPeerId,
            MessageSuggestionParams suggestionParams) {
        boolean settingOn = ReseamSettings.getBoolean("privacy.save_from_restricted", true);
        int size = messages == null ? -1 : messages.size();
        Log.i(TAG, "tryFakeForward called: settingOn=" + settingOn + " size=" + size + " peer=" + peer);
        if (!settingOn) return false;
        if (messages == null || messages.isEmpty()) return false;

        Object first = messages.get(0);
        if (!(first instanceof MessageObject)) {
            Log.i(TAG, "first is not MessageObject: " + (first == null ? "null" : first.getClass().getName()));
            return false;
        }
        MessageObject m0 = (MessageObject) first;
        if (m0.messageOwner == null) {
            Log.i(TAG, "m0.messageOwner is null");
            return false;
        }

        boolean perMsg = m0.messageOwner.noforwards;
        long dialogId = m0.getDialogId();
        boolean chatLevel = false;
        if (dialogId < 0) {
            MessagesController mc = MessagesController.getInstance(m0.currentAccount);
            if (mc != null) {
                TLRPC.Chat chat = mc.getChat(Long.valueOf(-dialogId));
                chatLevel = chat != null && chat.noforwards;
            }
        }
        Log.i(TAG, "noforwards: perMsg=" + perMsg + " chatLevel=" + chatLevel + " dialogId=" + dialogId);
        if (!perMsg && !chatLevel) return false;

        SendMessagesHelper helper = SendMessagesHelper.getInstance(m0.currentAccount);
        if (helper == null) {
            Log.i(TAG, "helper is null");
            return false;
        }
        Log.i(TAG, "redirecting " + size + " message(s) through processForwardFromMyName");
        List<?> snapshot = new ArrayList<>(messages);
        for (Object item : snapshot) {
            if (item instanceof MessageObject) {
                helper.processForwardFromMyName(
                        (MessageObject) item, peer, payStars, monoForumPeerId, suggestionParams);
            }
        }
        return true;
    }

    /**
     * Hook target: invoked at the head of {@code SendMessagesHelper.sendMessage(SendMessageParams)}.
     * If the params describe a media re-send for a no-forwards source, point
     * {@code params.path} at the locally-cached file AND replace
     * {@code params.photo} / {@code params.document} with shallow clones whose
     * {@code access_hash} is zero. The send dispatch picks {@code inputMediaUploadedX}
     * over {@code inputMediaX} only when {@code access_hash == 0}, so this is what
     * actually flips the path from "reference a no-forwards source" (server-reject)
     * to "fresh upload from disk" (server-accept). Cloning leaves the source
     * MessageObject's media intact for normal display.
     */
    public static void fixPathForNoForwards(SendMessagesHelper.SendMessageParams params) {
        if (params == null) return;
        if (params.path != null && !params.path.isEmpty()) return;
        if (params.photo == null && params.document == null) return;
        if (!(params.parentObject instanceof MessageObject)) return;
        if (!ReseamSettings.getBoolean("privacy.save_from_restricted", true)) return;

        MessageObject m = (MessageObject) params.parentObject;
        if (m.messageOwner == null) return;
        if (!isFromNoForwardsSource(m)) return;

        FileLoader fl = FileLoader.getInstance(m.currentAccount);
        if (fl == null) return;
        File f = fl.getPathToMessage(m.messageOwner);
        if (f == null || !f.exists() || f.length() <= 0) {
            Log.i(TAG, "noforwards forward: local cache missing for parentObject; server reject likely");
            return;
        }

        try {
            if (params.photo != null) {
                params.photo = cloneAndDetach(params.photo);
            }
            if (params.document != null) {
                params.document = cloneAndDetach(params.document);
            }
        } catch (Throwable t) {
            Log.w(TAG, "noforwards forward: clone+detach failed; skipping", t);
            return;
        }

        params.path = f.getAbsolutePath();
        Log.i(TAG, "noforwards forward: detached media + re-upload path = " + params.path);
    }

    @SuppressWarnings("unchecked")
    private static <T> T cloneAndDetach(T src) throws Exception {
        Class<?> cls = src.getClass();
        java.lang.reflect.Constructor<?> ctor = cls.getDeclaredConstructor();
        ctor.setAccessible(true);
        T dst = (T) ctor.newInstance();
        for (java.lang.reflect.Field f : cls.getFields()) {
            int mods = f.getModifiers();
            if (java.lang.reflect.Modifier.isStatic(mods) || java.lang.reflect.Modifier.isFinal(mods)) continue;
            f.set(dst, f.get(src));
        }
        // Zero out access_hash so the send dispatch chooses inputMediaUploadedX.
        try {
            java.lang.reflect.Field ah = cls.getField("access_hash");
            ah.setLong(dst, 0L);
        } catch (NoSuchFieldException ignored) {}
        return dst;
    }

    private static boolean isFromNoForwardsSource(MessageObject m) {
        if (m.messageOwner.noforwards) return true;
        long dialogId = m.getDialogId();
        if (dialogId >= 0) return false;
        MessagesController mc = MessagesController.getInstance(m.currentAccount);
        if (mc == null) return false;
        TLRPC.Chat chat = mc.getChat(Long.valueOf(-dialogId));
        return chat != null && chat.noforwards;
    }
}
