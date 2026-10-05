// SPDX-FileCopyrightText: 2026 AunAli K. <hello@auna.li>
// SPDX-License-Identifier: GPL-3.0-or-later

const values = new Map();

// The settings screen offers a restart after changes, so one read per process is enough.
function isEnabled(key, defaultValue) {
    if (!values.has(key)) {
        values.set(key, globalThis.nativeModuleProxy.ReseamSettings.getBoolean(key, defaultValue));
    }
    return values.get(key);
}

exports.skipWhen = function (key, defaultValue, original, ...args) {
    return isEnabled(key, defaultValue) ? undefined : original(...args);
};

exports.returnNullWhen = function (key, defaultValue, original, ...args) {
    return isEnabled(key, defaultValue) ? null : original(...args);
};

exports.returnWhen = function (key, defaultValue, value, original, ...args) {
    return isEnabled(key, defaultValue) ? value : original(...args);
};

exports.wrapWhen = function (key, defaultValue, wrapper, original, ...args) {
    return isEnabled(key, defaultValue) ? wrapper.call(this, original, ...args) : original(...args);
};

function withProperty(object, names, value) {
    const [name, ...rest] = names;
    return { ...object, [name]: rest.length === 0 ? value : withProperty(object[name], rest, value) };
}

exports.setArgumentWhen = function (key, defaultValue, index, path, value, original, ...args) {
    if (!isEnabled(key, defaultValue)) return original(...args);
    const changed = [...args];
    changed[index] = withProperty(args[index], path.split("."), value);
    return original(...changed);
};
