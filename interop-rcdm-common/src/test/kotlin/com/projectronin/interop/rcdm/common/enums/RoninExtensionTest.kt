package com.projectronin.interop.rcdm.common.enums

import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test

class RoninExtensionTest {
    @Test
    fun `codecov for enum values`() {
        RoninExtension.values().forEach { enum ->
            assertNotNull(RoninExtension.values().firstOrNull { it.value == enum.value })
        }
    }
}
