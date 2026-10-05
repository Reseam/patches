// SPDX-FileCopyrightText: 2026 AunAli K. <hello@auna.li>
// SPDX-License-Identifier: GPL-3.0-or-later

// The profile's Nitro card puts each button in its own slot: Get Nitro, then Shop.
function isShopSlot(slot) {
    return slot?.props?.children?.props?.icon?.type?.name === "ShopIcon";
}

function withoutShopSlot(node) {
    if (Array.isArray(node)) return node.filter(child => !isShopSlot(child)).map(withoutShopSlot);
    if (node === null || typeof node !== "object" || node.props === undefined) return node;
    return { ...node, props: { ...node.props, children: withoutShopSlot(node.props.children) } };
}

exports.nitroCard = function (original, props) {
    return withoutShopSlot(original(props));
};

exports.createRoute = function (original, config) {
    const isShop = config.screen && config.screen.route === "Shop";
    return original(isShop ? { ...config, usePredicate: () => false } : config);
};
