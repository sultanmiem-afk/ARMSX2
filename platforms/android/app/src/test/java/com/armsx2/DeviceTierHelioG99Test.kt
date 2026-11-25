package com.armsx2

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DeviceTierHelioG99Test {
    @Test
    fun recognizesTheMediaTekSocCode() {
        assertTrue(DeviceTier.isHelioG99Model("MT6789"))
        assertTrue(DeviceTier.isHelioG99Model("mediatek MT6789"))
    }

    @Test
    fun recognizesTheMarketingNameWithoutCaseSensitivity() {
        assertTrue(DeviceTier.isHelioG99Model("MediaTek Helio G99"))
        assertTrue(DeviceTier.isHelioG99Model("HELIO G99"))
    }

    @Test
    fun doesNotGuessOtherSocModelsAreHelioG99() {
        for (model in listOf("", "unknown", "MT6877", "Helio G96", "Snapdragon 680", "Mali-G57 MC2")) {
            assertFalse(model, DeviceTier.isHelioG99Model(model))
        }
    }
}
