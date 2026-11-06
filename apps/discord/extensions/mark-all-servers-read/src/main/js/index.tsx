// SPDX-FileCopyrightText: 2026 Reseam contributors <https://reseam.app>
// SPDX-License-Identifier: AGPL-3.0-or-later

import { cloneElement, type ComponentType, type ReactElement, type ReactNode } from "react";
import { AnalyticsSections } from "discord:Constants.tsx";
import { DoubleCheckmarkIcon } from "discord:design/components/Icon/native/redesign/generated/DoubleCheckmarkIcon.tsx";
import { ActionSheetRow } from "discord:design/native.tsx";
import ActionSheets from "discord:modules/action_sheet/native/ActionSheetActionCreators.tsx";
import markGuildsAsRead from "discord:modules/guild/markGuildsAsRead.tsx";
import GuildStore from "discord:stores/GuildStore.tsx";

const LABEL = "Mark All Servers As Read";

interface MenuItem {
    IconComponent: ComponentType;
    label: string;
    action(): void;
}

const markAllServersRead = () => markGuildsAsRead(GuildStore.getGuildIds(), AnalyticsSections.GUILD_POPOUT);

// Both menus start with Mark As Read.
export function withMarkAllItem(items: MenuItem[]) {
    const [markAsRead, ...rest] = items;
    return [markAsRead, { IconComponent: DoubleCheckmarkIcon, label: LABEL, action: markAllServersRead }, ...rest];
}

export function withMarkAllRow(group: ReactElement<{ children: ReactNode[] }>) {
    const [markAsRead, ...rest] = group.props.children;
    const onPress = () => {
        ActionSheets.hideActionSheet();
        markAllServersRead();
    };
    return cloneElement(group, {}, markAsRead, <ActionSheetRow label={LABEL} onPress={onPress} />, ...rest);
}
