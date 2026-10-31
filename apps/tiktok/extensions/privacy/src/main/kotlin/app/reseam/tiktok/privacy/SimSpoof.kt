// SPDX-FileCopyrightText: 2026 Reseam contributors <https://reseam.app>
// SPDX-License-Identifier: AGPL-3.0-or-later

@file:JvmName("SimSpoof")

package app.reseam.tiktok.privacy

import android.telephony.TelephonyManager
import app.reseam.runtime.Export
import app.reseam.tiktok.settings.PrivacySettings

private class Carrier(val iso: String, val mccMnc: String, val name: String)

private val carriers =
    mapOf(
        "ae" to Carrier("ae", "42402", "du"),
        "au" to Carrier("au", "50501", "Telstra"),
        "br" to Carrier("br", "72402", "TIM"),
        "ca" to Carrier("ca", "302220", "TELUS"),
        "ch" to Carrier("ch", "22801", "Swisscom"),
        "de" to Carrier("de", "26201", "Telekom.de"),
        "es" to Carrier("es", "21407", "Movistar"),
        "fr" to Carrier("fr", "20801", "Orange F"),
        "gb" to Carrier("gb", "23410", "O2 - UK"),
        "hk" to Carrier("hk", "45400", "CSL"),
        "id" to Carrier("id", "51010", "Telkomsel"),
        "ie" to Carrier("ie", "27201", "Vodafone IE"),
        "it" to Carrier("it", "22201", "TIM"),
        "jp" to Carrier("jp", "44010", "DOCOMO"),
        "kr" to Carrier("kr", "45005", "SKTelecom"),
        "mx" to Carrier("mx", "334020", "Telcel"),
        "my" to Carrier("my", "50212", "Maxis"),
        "nl" to Carrier("nl", "20408", "KPN"),
        "no" to Carrier("no", "24201", "Telenor"),
        "nz" to Carrier("nz", "53005", "Spark NZ"),
        "ph" to Carrier("ph", "51502", "Globe"),
        "pl" to Carrier("pl", "26003", "Orange"),
        "pt" to Carrier("pt", "26806", "MEO"),
        "sa" to Carrier("sa", "42001", "STC"),
        "se" to Carrier("se", "24001", "Telia"),
        "sg" to Carrier("sg", "52501", "Singtel"),
        "th" to Carrier("th", "52003", "AIS"),
        "tr" to Carrier("tr", "28601", "Turkcell"),
        "tw" to Carrier("tw", "46692", "Chunghwa"),
        "us" to Carrier("us", "310260", "T-Mobile"),
        "vn" to Carrier("vn", "45204", "Viettel"),
        "za" to Carrier("za", "65501", "Vodacom"),
    )

private fun carrier() = carriers[PrivacySettings.spoofSimCountry.value] ?: carriers.getValue("us")

@Export
fun getSimCountryIso(manager: TelephonyManager): String =
    if (PrivacySettings.spoofSim.value) carrier().iso else manager.getSimCountryIso()

@Export
fun getNetworkCountryIso(manager: TelephonyManager): String =
    if (PrivacySettings.spoofSim.value) carrier().iso else manager.getNetworkCountryIso()

@Export
fun getSimOperator(manager: TelephonyManager): String =
    if (PrivacySettings.spoofSim.value) carrier().mccMnc else manager.getSimOperator()

@Export
fun getNetworkOperator(manager: TelephonyManager): String =
    if (PrivacySettings.spoofSim.value) carrier().mccMnc else manager.getNetworkOperator()

@Export
fun getSimOperatorName(manager: TelephonyManager): String =
    if (PrivacySettings.spoofSim.value) carrier().name else manager.getSimOperatorName()

@Export
fun getNetworkOperatorName(manager: TelephonyManager): String =
    if (PrivacySettings.spoofSim.value) carrier().name else manager.getNetworkOperatorName()
