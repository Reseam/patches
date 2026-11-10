// SPDX-FileCopyrightText: 2026 Reseam contributors <https://reseam.app>
// SPDX-License-Identifier: AGPL-3.0-or-later

package app.reseam.patches.universal

import app.reseam.patch.patch

// A base config that overrides the app's pins and adds the user store, so a proxy's CA installed on
// the device is trusted everywhere the app connects. overridePins drops any <pin-set> the app ships.
private val NETWORK_SECURITY_CONFIG = """
    <?xml version="1.0" encoding="utf-8"?>
    <network-security-config>
        <base-config cleartextTrafficPermitted="true">
            <trust-anchors>
                <certificates src="system" />
                <certificates src="user" overridePins="true" />
            </trust-anchors>
        </base-config>
    </network-security-config>
""".trimIndent()

val overrideCertificatePinning = patch(UniversalPatchStrings.overrideCertificatePinning) {
    description(UniversalPatchStrings.overrideCertificatePinningDescription)

    execute {
        resources.addFile(
            "xml",
            "reseam_network_security_config",
            "res/xml/reseam_network_security_config.xml",
            NETWORK_SECURITY_CONFIG.toByteArray(),
        )
        // Replaces any config the app declares, so its own trust anchors and pins no longer apply.
        manifest.setAttributeString("application", "networkSecurityConfig", "@xml/reseam_network_security_config")
        log.info("Installed a permissive network security config")
    }
}
