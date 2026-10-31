// SPDX-FileCopyrightText: 2026 Reseam contributors <https://reseam.app>
// SPDX-License-Identifier: AGPL-3.0-or-later

import { useEffect, useState, useSyncExternalStore } from "react";
import { FlatList, ScrollView, View } from "react-native";
import Tokens from "discord:../discord_common/js/packages/tokens/native.tsx";
import { CheckmarkLargeIcon } from "discord:design/components/Icon/native/redesign/generated/CheckmarkLargeIcon.tsx";
import { CopyIcon } from "discord:design/components/Icon/native/redesign/generated/CopyIcon.tsx";
import { PencilIcon } from "discord:design/components/Icon/native/redesign/generated/PencilIcon.tsx";
import { PlusMediumIcon } from "discord:design/components/Icon/native/redesign/generated/PlusMediumIcon.tsx";
import { ShareIcon } from "discord:design/components/Icon/native/redesign/generated/ShareIcon.tsx";
import { TrashIcon } from "discord:design/components/Icon/native/redesign/generated/TrashIcon.tsx";
import {
    createStyles,
    IconButton,
    SearchField,
    showSimpleActionSheet,
    Stack,
    TableRadioGroup,
    TableRadioRow,
    TableRow,
    TableRowGroup,
    TextInput,
} from "discord:design/native.tsx";
import showColorPicker from "discord:modules/color_picker/native/showCustomColorPickerActionSheet.tsx";
import { type Color, formatColor, fromRgbInt, opaqueHex, paletteAt, paletteFromSeed, rgbInt, withAlpha } from "./color";
import { shareTheme } from "./share";
import {
    addTheme,
    allThemes,
    appliedTheme,
    applyTheme,
    type BackgroundStyle,
    backgroundStyle,
    deleteTheme,
    findTheme,
    librarySnapshot,
    setBackgroundStyle,
    setPreview,
    subscribe,
    updateTheme,
    withoutKey,
} from "./store";
import {
    type Background,
    DEFAULT_BACKGROUND_MIX,
    MAX_BACKGROUND_COLORS,
    PALETTE_KEYS,
    type PaletteKey,
    type Theme,
    THEME_BASES,
    type ThemeBase,
    type ThemeEntry,
} from "./theme";
import { compiledColors, discordPaletteColor, semanticTokens, themeSwatch } from "./tokens";

export const THEMES_ROUTE = "RESEAM_THEMES";
export const THEME_EDITOR_ROUTE = "RESEAM_THEME_EDITOR";

const BASE_NAMES: Record<ThemeBase, string> = { darker: "Dark", midnight: "Midnight", light: "Light" };
const PALETTE_NAMES: Record<PaletteKey, string> = {
    neutral: "Neutral",
    brand: "Brand",
    red: "Red",
    green: "Green",
    yellow: "Yellow",
    blue: "Blue",
};
const PALETTE_SAMPLE_TONE = 50;
const BACKGROUND_ANGLES = [0, 45, 90, 135, 180, 225, 270, 315];

interface Navigation {
    navigate(route: string, params: { themeId: string }): void;
    replace(route: string, params: { themeId: string }): void;
    goBack(): void;
}

let styles: (() => Record<"screen" | "swatch" | "swatchHalf" | "colorSample" | "search" | "trailing", object>) | null =
    null;

const useStyles = () =>
    (styles ??= createStyles({
        screen: { padding: Tokens.space.PX_16, paddingBottom: Tokens.space.PX_24 },
        swatch: {
            width: 32,
            height: 32,
            borderRadius: 16,
            overflow: "hidden",
            flexDirection: "row",
            borderWidth: 1,
            borderColor: Tokens.colors.BORDER_SUBTLE,
        },
        swatchHalf: { flex: 1 },
        colorSample: { width: 28, height: 28, borderRadius: 8, borderWidth: 1, borderColor: Tokens.colors.BORDER_SUBTLE },
        search: { marginBottom: Tokens.space.PX_8 },
        trailing: { flexDirection: "row", alignItems: "center", gap: Tokens.space.PX_12 },
    }))();

const newTheme = (): Theme => ({
    name: "New theme",
    author: null,
    base: appliedTheme()?.theme.base ?? "darker",
    palettes: {},
    raw: {},
    semantic: {},
    background: null,
});

function tokenLabel(token: string): string {
    const [first = "", ...rest] = token.toLowerCase().split("_");
    return [first.charAt(0).toUpperCase() + first.slice(1), ...rest].join(" ");
}

const themeSummary = (entry: ThemeEntry) =>
    [BASE_NAMES[entry.theme.base], entry.builtIn ? "Built in" : entry.theme.author].filter(Boolean).join(" · ");

// Discord's picker reports the color on Select and again as it closes.
function pickColor(color: Color, onPick: (color: Color) => void, adding = false) {
    let last = adding ? null : rgbInt(color);
    showColorPicker({
        color: rgbInt(color),
        onSelect: (value: number) => {
            if (value === last) return;
            last = value;
            onPick(fromRgbInt(value, color.a));
        },
    });
}

function Swatch({ theme }: { theme: Theme }) {
    const styles = useStyles();
    return (
        <View style={styles.swatch}>
            {themeSwatch(theme).map((color, index) => (
                <View key={index} style={[styles.swatchHalf, { backgroundColor: color }]} />
            ))}
        </View>
    );
}

function ColorSample({ color }: { color: Color }) {
    const styles = useStyles();
    return <View style={[styles.colorSample, { backgroundColor: formatColor(color) }]} />;
}

function ThemeTrailing({ applied, onEdit, name }: { applied: boolean; onEdit: (() => void) | null; name: string }) {
    const styles = useStyles();
    return (
        <View style={styles.trailing}>
            {applied ? <TableRow.Icon IconComponent={CheckmarkLargeIcon} /> : null}
            {onEdit === null ? null : (
                <IconButton
                    icon={<PencilIcon size="sm" />}
                    variant="secondary"
                    size="sm"
                    accessibilityLabel={`Edit ${name}`}
                    onPress={onEdit}
                />
            )}
        </View>
    );
}

export function ThemesScreen({ navigation }: { navigation: Navigation }) {
    const styles = useStyles();
    useSyncExternalStore(subscribe, librarySnapshot);
    const applied = appliedTheme();
    const open = (entry: ThemeEntry) => navigation.navigate(THEME_EDITOR_ROUTE, { themeId: entry.id });
    return (
        <ScrollView contentContainerStyle={styles.screen}>
            <Stack spacing={24}>
                <TableRowGroup
                    title="Themes"
                    description="Tap a theme to use it. Themes stay on this phone; share one to send its file."
                >
                    <TableRow
                        label="Discord's theme"
                        subLabel="The theme picked in Appearance"
                        trailing={<ThemeTrailing applied={applied === null} onEdit={null} name="" />}
                        onPress={() => applyTheme(null)}
                    />
                    {allThemes().map(entry => (
                        <TableRow
                            key={entry.id}
                            label={entry.theme.name}
                            subLabel={themeSummary(entry)}
                            icon={<Swatch theme={entry.theme} />}
                            trailing={
                                <ThemeTrailing applied={entry === applied} onEdit={() => open(entry)} name={entry.theme.name} />
                            }
                            onPress={() => applyTheme(entry.id)}
                        />
                    ))}
                </TableRowGroup>
                <TableRowGroup>
                    <TableRow
                        label="New theme"
                        icon={<TableRow.Icon IconComponent={PlusMediumIcon} />}
                        onPress={() => open(addTheme(newTheme()))}
                    />
                </TableRowGroup>
            </Stack>
        </ScrollView>
    );
}

function NameField({ entry }: { entry: ThemeEntry }) {
    const [name, setName] = useState(entry.theme.name);
    const save = () => {
        const trimmed = name.trim();
        if (trimmed !== "" && trimmed !== entry.theme.name) updateTheme(entry.id, { ...entry.theme, name: trimmed });
    };
    return <TextInput label="Name" value={name} onChange={setName} onBlur={save} onSubmitEditing={save} returnKeyType="done" />;
}

function BackgroundSection({ entry }: { entry: ThemeEntry }) {
    const { theme } = entry;
    const { background } = theme;
    const setBackground = (next: Background | null) => updateTheme(entry.id, { ...theme, background: next });
    const withColors = (colors: Color[]) =>
        setBackground({ angle: background?.angle ?? 0, mix: background?.mix ?? DEFAULT_BACKGROUND_MIX, colors });
    const colors = background?.colors ?? [];
    const editColor = (index: number) =>
        showSimpleActionSheet({
            key: "ReseamThemeBackgroundColor",
            header: { title: `Background color ${index + 1}` },
            options: [
                {
                    label: "Pick a color",
                    onPress: () =>
                        pickColor(colors[index]!, color => withColors(colors.map((other, at) => (at === index ? color : other)))),
                },
                ...(colors.length > 1
                    ? [
                          {
                              label: "Remove color",
                              isDestructive: true,
                              onPress: () => withColors(colors.filter((_, at) => at !== index)),
                          },
                      ]
                    : []),
            ],
        });
    const addColor = () =>
        pickColor(colors.at(-1) ?? compiledColors(entry).BACKGROUND_BRAND!, color => withColors([...colors, withAlpha(color, 1)]), true);
    const presets: Record<number, string> = { 50: "Soft", [DEFAULT_BACKGROUND_MIX]: "Default", 100: "Vivid" };
    const strengths =
        background === null ? presets : { ...presets, [background.mix]: presets[background.mix] ?? `${background.mix}%` };
    const styleChoice =
        background === null ? null : (
            <TableRadioGroup
                title="Show background as"
                description="Applies on this phone; a shared file keeps the theme's background."
                value={backgroundStyle(entry.id)}
                onChange={(style: BackgroundStyle) => setBackgroundStyle(entry.id, style)}
                hasIcons={false}
            >
                <TableRadioRow key="gradient" value="gradient" label="Gradient" />
                <TableRadioRow key="solid" value="solid" label="Solid" subLabel="The first color only" />
                <TableRadioRow key="none" value="none" label="None" subLabel="The theme's colors without a background" />
            </TableRadioGroup>
        );
    if (entry.builtIn) return styleChoice;
    return (
        <Stack spacing={24}>
            {styleChoice}
            <TableRowGroup
                title="Background"
                description="Drawn behind the app like a Nitro theme, with see-through surfaces over it."
            >
                {colors.map((color, index) => (
                    <TableRow
                        key={index}
                        label={`Color ${index + 1}`}
                        subLabel={opaqueHex(color)}
                        trailing={<ColorSample color={color} />}
                        arrow
                        onPress={() => editColor(index)}
                    />
                ))}
                {colors.length < MAX_BACKGROUND_COLORS ? (
                    <TableRow label="Add color" icon={<TableRow.Icon IconComponent={PlusMediumIcon} />} onPress={addColor} />
                ) : null}
                {background === null ? null : (
                    <TableRow label="Remove background" variant="danger" onPress={() => setBackground(null)} />
                )}
            </TableRowGroup>
            {background === null ? null : (
                <TableRadioGroup
                    title="Direction"
                    value={background.angle}
                    onChange={(angle: number) => setBackground({ ...background, angle })}
                    hasIcons={false}
                >
                    {BACKGROUND_ANGLES.map(angle => (
                        <TableRadioRow key={angle} value={angle} label={`${angle}°`} />
                    ))}
                </TableRadioGroup>
            )}
            {background === null ? null : (
                <TableRadioGroup
                    title="Strength"
                    value={background.mix}
                    onChange={(mix: number) => setBackground({ ...background, mix })}
                    hasIcons={false}
                >
                    {Object.entries(strengths).map(([mix, label]) => (
                        <TableRadioRow key={mix} value={Number(mix)} label={label} />
                    ))}
                </TableRadioGroup>
            )}
        </Stack>
    );
}

function EditorHeader({
    entry,
    navigation,
    search,
    setSearch,
}: {
    entry: ThemeEntry;
    navigation: Navigation;
    search: string;
    setSearch: (search: string) => void;
}) {
    const styles = useStyles();
    const { theme } = entry;
    const update = (changes: Partial<Theme>) => updateTheme(entry.id, { ...theme, ...changes });
    const choosePalette = (key: PaletteKey) => {
        const stops = theme.palettes[key];
        const pick = () =>
            pickColor(stops === undefined ? discordPaletteColor(key) : paletteAt(stops, PALETTE_SAMPLE_TONE, 1), seed =>
                update({ palettes: { ...theme.palettes, [key]: paletteFromSeed(seed) } }),
            );
        if (stops === undefined) return pick();
        showSimpleActionSheet({
            key: "ReseamThemePalette",
            header: { title: `${PALETTE_NAMES[key]} palette` },
            options: [
                { label: "Pick a color", onPress: pick },
                { label: "Use Discord's colors", onPress: () => update({ palettes: withoutKey(theme.palettes, key) }) },
            ],
        });
    };
    return (
        <Stack spacing={24} style={styles.search}>
            {entry.builtIn ? null : <NameField entry={entry} />}
            {entry.builtIn ? null : (
                <TableRadioGroup
                    title="Base theme"
                    value={theme.base}
                    onChange={(base: ThemeBase) => update({ base })}
                    hasIcons={false}
                >
                    {THEME_BASES.map(base => (
                        <TableRadioRow key={base} value={base} label={BASE_NAMES[base]} />
                    ))}
                </TableRadioGroup>
            )}
            {entry.builtIn ? null : (
                <TableRowGroup title="Palettes" description="A palette replaces one family of Discord's colors, tone for tone.">
                    {PALETTE_KEYS.map(key => {
                        const stops = theme.palettes[key];
                        return (
                            <TableRow
                                key={key}
                                label={PALETTE_NAMES[key]}
                                subLabel={stops === undefined ? "Discord's colors" : undefined}
                                trailing={
                                    stops === undefined ? null : <ColorSample color={paletteAt(stops, PALETTE_SAMPLE_TONE, 1)} />
                                }
                                arrow
                                onPress={() => choosePalette(key)}
                            />
                        );
                    })}
                </TableRowGroup>
            )}
            <BackgroundSection entry={entry} />
            <TableRowGroup>
                <TableRow label="Share" icon={<TableRow.Icon IconComponent={ShareIcon} />} onPress={() => shareTheme(theme)} />
                <TableRow
                    label="Duplicate"
                    icon={<TableRow.Icon IconComponent={CopyIcon} />}
                    onPress={() =>
                        navigation.replace(THEME_EDITOR_ROUTE, {
                            themeId: addTheme({ ...theme, name: `${theme.name} copy`, author: null }).id,
                        })
                    }
                />
                {entry.builtIn ? null : (
                    <TableRow
                        label="Delete"
                        variant="danger"
                        icon={<TableRow.Icon IconComponent={TrashIcon} variant="danger" />}
                        onPress={() =>
                            showSimpleActionSheet({
                                key: "ReseamThemeDelete",
                                header: { title: `Delete ${theme.name}?` },
                                options: [{ label: "Delete theme", isDestructive: true, onPress: () => deleteTheme(entry.id) }],
                            })
                        }
                    />
                )}
            </TableRowGroup>
            <TableRowGroup
                title="Colors"
                description={
                    entry.builtIn
                        ? "Duplicate the theme to change its colors."
                        : "Every color Discord draws with in this base theme."
                }
            />
            <SearchField size="md" placeholder="Search colors" onChange={setSearch} defaultValue={search} />
        </Stack>
    );
}

function TokenRow({ entry, token, color, start, end }: { entry: ThemeEntry; token: string; color: Color; start: boolean; end: boolean }) {
    const { theme } = entry;
    const edited = token in theme.semantic;
    const setColor = (picked: Color) => updateTheme(entry.id, { ...theme, semantic: { ...theme.semantic, [token]: picked } });
    const reset = () => updateTheme(entry.id, { ...theme, semantic: withoutKey(theme.semantic, token) });
    const onPress = entry.builtIn
        ? undefined
        : () => {
              if (!edited) return pickColor(color, setColor);
              showSimpleActionSheet({
                  key: "ReseamThemeColor",
                  header: { title: tokenLabel(token), subtitle: token },
                  options: [
                      { label: "Pick a color", onPress: () => pickColor(color, setColor) },
                      { label: "Use the palette's color", onPress: reset },
                  ],
              });
          };
    return (
        <TableRow
            label={tokenLabel(token)}
            subLabel={edited ? `${formatColor(color)} · Edited` : formatColor(color)}
            trailing={<ColorSample color={color} />}
            start={start}
            end={end}
            onPress={onPress}
        />
    );
}

export function ThemeEditorScreen({ navigation, route }: { navigation: Navigation; route: { params: { themeId: string } } }) {
    const styles = useStyles();
    useSyncExternalStore(subscribe, librarySnapshot);
    const [search, setSearch] = useState("");
    const entry = findTheme(route.params.themeId);
    useEffect(() => {
        if (entry === null) navigation.goBack();
    }, [entry === null]);
    useEffect(() => setPreview(entry), [entry]);
    useEffect(() => () => setPreview(null), []);
    if (entry === null) return null;
    const compiled = compiledColors(entry);
    const query = search.trim().toUpperCase().replace(/\s+/g, "_");
    const tokens = Object.keys(compiled)
        .filter(token => token in semanticTokens() && token.includes(query))
        .sort();
    return (
        <FlatList
            contentContainerStyle={styles.screen}
            data={tokens}
            keyExtractor={token => token}
            keyboardShouldPersistTaps="handled"
            ListHeaderComponent={<EditorHeader entry={entry} navigation={navigation} search={search} setSearch={setSearch} />}
            renderItem={({ item, index }) => (
                <TokenRow entry={entry} token={item} color={compiled[item]!} start={index === 0} end={index === tokens.length - 1} />
            )}
        />
    );
}
