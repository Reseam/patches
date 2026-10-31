// SPDX-FileCopyrightText: 2026 Reseam contributors <https://reseam.app>
// SPDX-License-Identifier: AGPL-3.0-or-later

// The profile's Nitro card puts each button in its own slot: Get Nitro, then Shop.
const isShopSlot = (slot: any) => slot?.props?.children?.props?.icon?.type?.name === "ShopIcon";

export function withoutShop(node: any): any {
    if (Array.isArray(node)) return node.filter(child => !isShopSlot(child)).map(withoutShop);
    if (node === null || typeof node !== "object" || node.props === undefined) return node;
    return { ...node, props: { ...node.props, children: withoutShop(node.props.children) } };
}

export function withoutShopRoute(config: any) {
    return config.screen?.route === "Shop" ? { ...config, usePredicate: () => false } : config;
}
