// SPDX-FileCopyrightText: 2026 AunAli K. <hello@auna.li>
// SPDX-License-Identifier: GPL-3.0-or-later

package app.reseam.instagram.settings;

import android.content.Context;
import android.util.Log;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.TextView;

import java.lang.reflect.Method;

import app.reseam.runtime.settings.ReseamSettingsScreen;

/**
 * Toggle rows built from Instagram's IgdsListCell so the settings screen looks native. Only
 * non-obfuscated names are used: the class, setTextCellType, setChecked, getTitleView,
 * getSubtitleView, and the TYPE_SWITCH enum constant; the listener setter is found by its
 * parameter type.
 */
public final class IgdsToggleRows implements ReseamSettingsScreen.ToggleRowFactory {
    private static final String TAG = "ReseamSettings";
    private static final String IGDS_LIST_CELL_CLASS = "com.instagram.igds.components.textcell.IgdsListCell";

    private final Class<?> cellClass;
    private final Method setTextCellType;
    private final Object typeSwitch;
    private final Method setChecked;
    private final Method getTitleView;
    private final Method getSubtitleView;
    private final Method setOnCheckedChangeListener;

    private IgdsToggleRows() throws ReflectiveOperationException {
        cellClass = Class.forName(IGDS_LIST_CELL_CLASS);
        Method typeSetter = null;
        Method listenerSetter = null;
        for (Method m : cellClass.getDeclaredMethods()) {
            Class<?>[] params = m.getParameterTypes();
            if (params.length != 1) continue;
            if ("setTextCellType".equals(m.getName()) && params[0].isEnum()) typeSetter = m;
            if (CompoundButton.OnCheckedChangeListener.class.isAssignableFrom(params[0])) listenerSetter = m;
        }
        if (typeSetter == null) throw new NoSuchMethodException("IgdsListCell.setTextCellType(enum)");
        if (listenerSetter == null) throw new NoSuchMethodException("IgdsListCell listener setter");
        listenerSetter.setAccessible(true);
        setTextCellType = typeSetter;
        setOnCheckedChangeListener = listenerSetter;
        typeSwitch = enumConstant(typeSetter.getParameterTypes()[0], "TYPE_SWITCH");
        setChecked = cellClass.getMethod("setChecked", boolean.class);
        getTitleView = cellClass.getMethod("getTitleView");
        getSubtitleView = cellClass.getMethod("getSubtitleView");
    }

    /** Installs IGDS rows on the shared screen; keeps its plain rows when Instagram's cell is missing. */
    public static void install() {
        try {
            ReseamSettingsScreen.setToggleRowFactory(new IgdsToggleRows());
        } catch (ReflectiveOperationException | RuntimeException e) {
            Log.w(TAG, "IgdsListCell unavailable, using plain toggle rows", e);
        }
    }

    @Override
    public View create(Context ctx, String title, String summary, boolean checked, CompoundButton.OnCheckedChangeListener listener) {
        try {
            View cell = (View) cellClass.getConstructor(Context.class).newInstance(ctx);
            setTextCellType.invoke(cell, typeSwitch);
            ((TextView) getTitleView.invoke(cell)).setText(title);
            if (summary != null && !summary.isEmpty()) {
                TextView subtitle = (TextView) getSubtitleView.invoke(cell);
                subtitle.setText(summary);
                subtitle.setVisibility(View.VISIBLE);
            }
            setChecked.invoke(cell, checked);
            setOnCheckedChangeListener.invoke(cell, listener);
            return cell;
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("IgdsListCell changed shape", e);
        }
    }

    private static Object enumConstant(Class<?> enumClass, String name) throws NoSuchFieldException {
        for (Object constant : enumClass.getEnumConstants()) {
            if (name.equals(((Enum<?>) constant).name())) return constant;
        }
        throw new NoSuchFieldException(enumClass.getName() + "." + name);
    }
}
