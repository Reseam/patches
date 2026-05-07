// SPDX-FileCopyrightText: 2026 AunAli K. <hello@auna.li>
// SPDX-License-Identifier: GPL-3.0-or-later

extra["dexOutputName"] = "telegram-settings.dex"
extra["dexExcludeClasses"] = listOf(
    "org/telegram/ui/ActionBar/BaseFragment.class",
    "org/telegram/ui/Cells/TextCheckCell.class",
    "org/telegram/ui/Cells/HeaderCell.class",
    "org/telegram/ui/Cells/TextCell.class",
    "org/telegram/ui/Components/UItem.class",
    "org/telegram/ui/Components/UniversalAdapter.class",
    "org/telegram/messenger/FileLoader.class",
    "org/telegram/messenger/MessageObject.class",
    "org/telegram/messenger/MessagesController.class",
    "org/telegram/messenger/SendMessagesHelper.class",
    "org/telegram/messenger/SendMessagesHelper\$SendMessageParams.class",
    "org/telegram/messenger/MessageSuggestionParams.class",
    "org/telegram/tgnet/TLRPC.class",
    "org/telegram/tgnet/TLRPC\$Message.class",
    "org/telegram/tgnet/TLRPC\$Chat.class",
    "org/telegram/tgnet/TLRPC\$TL_photo.class",
    "org/telegram/tgnet/TLRPC\$TL_document.class",
).joinToString(",")

apply(from = rootDir.resolve("android-extension-module.gradle.kts"))

dependencies {
    add("compileOnly", project(":shared-settings-runtime"))
}
