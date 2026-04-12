/*
 * Stitch — part of the Stitch Android patcher.
 * Copyright (C) 2026 Aunali321 <accounts@auna.li>
 *
 * This file is part of Stitch.
 *
 * Stitch is free software: you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Stitch is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 *
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package dev.stitch.instagram.download;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.IdentityHashMap;
import java.util.Set;

final class Reflect {
    interface Match {
        boolean test(Object object);
    }

    private Reflect() {}

    static String className(Object object) {
        return object == null ? "null" : object.getClass().getName();
    }

    static Object find(Object root, Match match, int maxDepth) {
        return find(root, match, maxDepth, 0, java.util.Collections.newSetFromMap(new IdentityHashMap<>()));
    }

    static String stringField(Object owner, String fieldName) {
        Object value = fieldValue(owner, fieldName);
        return value instanceof String ? (String) value : null;
    }

    static Object fieldValue(Object owner, String fieldName) {
        if (owner == null) return null;

        for (Field field : fields(owner.getClass())) {
            if (!fieldName.equals(field.getName())) continue;
            return fieldValue(field, owner);
        }
        return null;
    }

    static Object fieldValue(Field field, Object owner) {
        try {
            field.setAccessible(true);
            return field.get(owner);
        } catch (Throwable ignored) {
            return null;
        }
    }

    static Object invoke(Method method, Object owner, Object... args) {
        try {
            method.setAccessible(true);
            return method.invoke(owner, args);
        } catch (Throwable ignored) {
            return null;
        }
    }

    static Field[] fields(Class<?> cls) {
        java.util.ArrayList<Field> fields = new java.util.ArrayList<>();
        for (Class<?> current = cls; current != null && current != Object.class; current = current.getSuperclass()) {
            for (Field field : current.getDeclaredFields()) fields.add(field);
        }
        return fields.toArray(new Field[0]);
    }

    static Method[] methods(Class<?> cls) {
        java.util.ArrayList<Method> methods = new java.util.ArrayList<>();
        java.util.HashSet<Class<?>> interfaces = new java.util.HashSet<>();
        for (Class<?> current = cls; current != null && current != Object.class; current = current.getSuperclass()) {
            for (Method method : current.getDeclaredMethods()) methods.add(method);
            addInterfaceMethods(current, methods, interfaces);
        }
        return methods.toArray(new Method[0]);
    }

    private static Object find(Object object, Match match, int maxDepth, int depth, Set<Object> seen) {
        if (object == null || depth > maxDepth) return null;
        if (match.test(object)) return object;
        if (!seen.add(object)) return null;

        Class<?> cls = object.getClass();
        if (object instanceof Iterable<?>) return findIterable((Iterable<?>) object, match, maxDepth, depth, seen);
        if (cls.isArray() && !cls.getComponentType().isPrimitive()) {
            return findArray((Object[]) object, match, maxDepth, depth, seen);
        }
        if (!isInspectable(cls)) return null;

        for (Field field : fields(cls)) {
            Object found = find(fieldValue(field, object), match, maxDepth, depth + 1, seen);
            if (found != null) return found;
        }
        return null;
    }

    private static Object findIterable(Iterable<?> items, Match match, int maxDepth, int depth, Set<Object> seen) {
        for (Object item : items) {
            Object found = find(item, match, maxDepth, depth + 1, seen);
            if (found != null) return found;
        }
        return null;
    }

    private static Object findArray(Object[] items, Match match, int maxDepth, int depth, Set<Object> seen) {
        for (Object item : items) {
            Object found = find(item, match, maxDepth, depth + 1, seen);
            if (found != null) return found;
        }
        return null;
    }

    private static boolean isInspectable(Class<?> cls) {
        if (cls.isPrimitive() || cls.isArray() || cls.isEnum()) return false;

        String name = cls.getName();
        if (name.startsWith("java.") || name.startsWith("android.") || name.startsWith("androidx.")) return false;
        return true;
    }

    private static void addInterfaceMethods(
            Class<?> cls,
            java.util.ArrayList<Method> methods,
            java.util.HashSet<Class<?>> seen
    ) {
        for (Class<?> iface : cls.getInterfaces()) {
            if (!seen.add(iface)) continue;

            for (Method method : iface.getDeclaredMethods()) methods.add(method);
            addInterfaceMethods(iface, methods, seen);
        }
    }
}
