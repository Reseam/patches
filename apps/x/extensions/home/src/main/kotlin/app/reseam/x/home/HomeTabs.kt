// SPDX-FileCopyrightText: 2026 Reseam contributors <https://reseam.app>
// SPDX-License-Identifier: AGPL-3.0-or-later

@file:JvmName("HomeTabs")

package app.reseam.x.home

import app.reseam.runtime.Export

@Export external fun isForYou(tab: Any): Boolean

@Export
fun withoutForYou(tabs: Array<Any>): Array<Any> = tabs.filterNot(::isForYou).toTypedArray()
