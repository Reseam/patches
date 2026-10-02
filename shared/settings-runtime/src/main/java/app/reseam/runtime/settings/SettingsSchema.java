// SPDX-FileCopyrightText: 2026 AunAli K. <hello@auna.li>
// SPDX-License-Identifier: GPL-3.0-or-later

package app.reseam.runtime.settings;

import android.content.Context;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

/** The settings schema the patcher writes to {@code assets/reseam/settings.json}. */
final class SettingsSchema {
    private static final String PATH = "reseam/settings.json";
    private static volatile SettingsSchema loaded;

    final List<Page> pages;
    final List<Section> sections;

    private SettingsSchema(JSONObject json) throws JSONException {
        pages = new ArrayList<>();
        JSONArray pageArray = json.getJSONArray("pages");
        for (int i = 0; i < pageArray.length(); i++) pages.add(new Page(pageArray.getJSONObject(i)));
        sections = new ArrayList<>();
        JSONArray sectionArray = json.getJSONArray("sections");
        for (int i = 0; i < sectionArray.length(); i++) sections.add(new Section(sectionArray.getJSONObject(i)));
    }

    /** The asset is fixed for the life of the process, so it is parsed once. */
    static SettingsSchema load(Context ctx) {
        SettingsSchema schema = loaded;
        if (schema != null) return schema;
        try {
            return loaded = new SettingsSchema(new JSONObject(readAsset(ctx)));
        } catch (IOException | JSONException e) {
            throw new IllegalStateException("Could not read " + PATH, e);
        }
    }

    Setting setting(String key) {
        for (Section section : sections) {
            for (Setting setting : section.settings) {
                if (setting.key.equals(key)) return setting;
            }
        }
        throw new IllegalArgumentException("Unknown setting: " + key);
    }

    /** {@code ""} is the root page. */
    static final class Page {
        final String id;
        final String title;
        final String parent;

        private Page(JSONObject json) throws JSONException {
            id = json.getString("id");
            title = json.getString("title");
            parent = destination(json, "parent");
        }
    }

    static final class Section {
        final String page;
        final String title;
        final List<Setting> settings = new ArrayList<>();

        private Section(JSONObject json) throws JSONException {
            page = destination(json, "page");
            title = json.getString("title");
            JSONArray array = json.getJSONArray("settings");
            for (int i = 0; i < array.length(); i++) settings.add(Setting.parse(array.getJSONObject(i)));
        }
    }

    abstract static class Setting {
        final String key;
        final String title;
        final String summary;

        private Setting(JSONObject json) throws JSONException {
            key = json.getString("key");
            title = json.getString("title");
            summary = json.isNull("summary") ? null : json.getString("summary");
        }

        private static Setting parse(JSONObject json) throws JSONException {
            String type = json.getString("type");
            switch (type) {
                case "toggle": return new Toggle(json);
                case "text": return new Text(json);
                case "folder": return new Folder(json);
                case "choice": return new Choice(json);
                default: throw new JSONException("Unknown setting type: " + type);
            }
        }
    }

    static final class Toggle extends Setting {
        final boolean defaultValue;

        private Toggle(JSONObject json) throws JSONException {
            super(json);
            defaultValue = json.getBoolean("default");
        }
    }

    static final class Text extends Setting {
        final String defaultValue;
        final boolean multiline;

        private Text(JSONObject json) throws JSONException {
            super(json);
            defaultValue = json.getString("default");
            multiline = json.optBoolean("multiline", false);
        }
    }

    static final class Folder extends Setting {
        final String defaultValue;

        private Folder(JSONObject json) throws JSONException {
            super(json);
            defaultValue = json.getString("default");
        }
    }

    static final class Choice extends Setting {
        final String defaultValue;
        final String[] values;
        final String[] titles;

        private Choice(JSONObject json) throws JSONException {
            super(json);
            defaultValue = json.getString("default");
            JSONArray choices = json.getJSONArray("choices");
            values = new String[choices.length()];
            titles = new String[choices.length()];
            for (int i = 0; i < choices.length(); i++) {
                JSONObject choice = choices.getJSONObject(i);
                values[i] = choice.getString("value");
                titles[i] = choice.getString("title");
            }
        }

        /** A stored value the schema no longer lists, left by a patch that offered it, reads as the default. */
        int index(String stored) {
            int fallback = 0;
            for (int i = 0; i < values.length; i++) {
                if (values[i].equals(stored)) return i;
                if (values[i].equals(defaultValue)) fallback = i;
            }
            return fallback;
        }
    }

    private static String destination(JSONObject json, String key) throws JSONException {
        return json.isNull(key) ? "" : json.getString(key);
    }

    private static String readAsset(Context ctx) throws IOException {
        try (InputStream in = ctx.getAssets().open(PATH)) {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            byte[] buf = new byte[4096];
            int read;
            while ((read = in.read(buf)) != -1) out.write(buf, 0, read);
            return out.toString("UTF-8");
        }
    }
}
