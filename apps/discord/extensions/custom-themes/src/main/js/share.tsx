// SPDX-FileCopyrightText: 2026 Reseam contributors <https://reseam.app>
// SPDX-License-Identifier: AGPL-3.0-or-later

import { useEffect } from "react";
import { NativeModules } from "react-native";
import Tokens from "discord:../discord_common/js/packages/tokens/native.tsx";
import { BottomSheet, BottomSheetTitleHeader, Button, createStyles, Stack } from "discord:design/native.tsx";
import ChannelMessages from "discord:lib/ChannelMessages.tsx";
import ActionSheets from "discord:modules/action_sheet/native/ActionSheetActionCreators.tsx";
import MessageStore from "discord:stores/MessageStore.tsx";
import { addTheme, applyTheme, findSameTheme, setPreview } from "./store";
import { parseTheme, serializeTheme, type Theme, THEME_FILE_SUFFIX, type ThemeEntry, themeFileName } from "./theme";
import { themeSwatch } from "./tokens";

const MAX_THEME_FILE_SIZE = 1024 * 1024;
const SHARED_THEME_SHEET = "ReseamSharedTheme";

interface Attachment {
    id: string;
    url: string;
    filename?: string;
    size: number;
}

interface SharedMessage {
    id: string;
    channel_id: string;
    attachments?: Attachment[];
    set(key: string, value: unknown): SharedMessage;
}

/** A theme file shared in a chat: loading, loaded, or failed. */
const sharedThemes = new Map<string, { entry?: ThemeEntry; error?: unknown }>();

const themeAttachment = (message: SharedMessage) =>
    message.attachments?.find(
        attachment => attachment.filename?.endsWith(THEME_FILE_SUFFIX) && attachment.size <= MAX_THEME_FILE_SIZE,
    ) ?? null;

const sharedEntry = (message: SharedMessage) => {
    const attachment = themeAttachment(message);
    return attachment === null ? undefined : sharedThemes.get(attachment.id)?.entry;
};

function withThemeCard(message: SharedMessage, attachment: Attachment) {
    const shared = sharedThemes.get(attachment.id)?.entry;
    if (shared === undefined) return message;
    // Discord's native card adds the "#" itself.
    const colors = themeSwatch(shared.theme).map(color => color.slice(1));
    return message.set("sharedClientTheme", { colors, gradient_angle: shared.theme.background?.angle ?? 0 });
}

function fetchSharedTheme(message: SharedMessage, attachment: Attachment) {
    fetch(attachment.url)
        .then(response => {
            if (!response.ok) throw new Error(`The theme file could not be downloaded (${response.status})`);
            return response.json();
        })
        .then(document => {
            const theme = parseTheme(document);
            sharedThemes.set(attachment.id, { entry: { id: `shared-${attachment.id}`, revision: 0, builtIn: false, theme } });
            const channel = ChannelMessages.getOrCreate(message.channel_id);
            if (!channel.has(message.id)) return;
            ChannelMessages.commit(channel.update(message.id, (stored: SharedMessage) => withThemeCard(stored, attachment)));
            // A commit outside a dispatch reports no change, so the store emits one for the redraw.
            MessageStore.emitChange();
        })
        .catch(error => sharedThemes.set(attachment.id, { error }));
}

/** A message with a theme file shows Discord's theme card, once the file has loaded. */
export function withSharedTheme<Args extends { message: SharedMessage }>(args: Args): Args {
    const attachment = themeAttachment(args.message);
    if (attachment === null) return args;
    if (!sharedThemes.has(attachment.id)) {
        sharedThemes.set(attachment.id, {});
        fetchSharedTheme(args.message, attachment);
    }
    return { ...args, message: withThemeCard(args.message, attachment) };
}

export function withSharedThemeHeading<Data>(data: Data, message: SharedMessage) {
    const shared = sharedEntry(message);
    return shared === undefined ? data : { ...data, previewHeading: shared.theme.name };
}

let sheetStyles: (() => { actions: object }) | null = null;

const useSheetStyles = () =>
    (sheetStyles ??= createStyles({
        actions: { paddingHorizontal: Tokens.space.PX_16, paddingBottom: Tokens.space.PX_16 },
    }))();

// The app shows the theme while the sheet is open, uncovered like Discord's own theme preview;
// closing the sheet any way ends the preview.
function SharedThemeSheet({ shared }: { shared: ThemeEntry }) {
    const styles = useSheetStyles();
    useEffect(() => {
        setPreview(shared);
        return () => setPreview(null);
    }, []);
    const saved = () => findSameTheme(shared.theme) ?? addTheme(shared.theme);
    const close = (then: () => void) => () => {
        then();
        ActionSheets.hideActionSheet(SHARED_THEME_SHEET);
    };
    return (
        <BottomSheet backdropOpacity={0}>
            <BottomSheetTitleHeader title={shared.theme.name} subtitle={shared.theme.author ?? undefined} />
            <Stack direction="horizontal" spacing={8} style={styles.actions}>
                <Button variant="secondary" size="md" grow text="Add to my themes" onPress={close(saved)} />
                <Button variant="primary" size="md" grow text="Apply" onPress={close(() => applyTheme(saved().id))} />
            </Stack>
        </BottomSheet>
    );
}

/** Opens a shared theme's sheet; true when the message holds one. Discord can store the message
 * again without the card's data, so the theme is looked up by its attachment. */
export function previewSharedTheme(props: { message: SharedMessage }): boolean {
    const shared = sharedEntry(props.message);
    if (shared === undefined) return false;
    ActionSheets.openLazy(
        Promise.resolve({ default: () => <SharedThemeSheet shared={shared} /> }),
        SHARED_THEME_SHEET,
        { backdropKind: "none" },
    );
    return true;
}

export function shareTheme(theme: Theme) {
    NativeModules.ReseamDiscordThemes.share(themeFileName(theme), JSON.stringify(serializeTheme(theme), null, 2), "Share theme");
}
