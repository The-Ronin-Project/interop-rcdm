package com.projectronin.interop.rcdm.validate.util

import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class ValidationHelpersTest {
    @Test
    fun `isNullOrDayFormat returns true for null string`() {
        val value: String? = null
        assertTrue(value.isNullOrDayFormat())
    }

    @Test
    fun `isNullOrDayFormat returns true when length of 10 or more`() {
        assertTrue("2023-12-01".isNullOrDayFormat())
        assertTrue("2023-12-01T15:19:00.0000".isNullOrDayFormat())
    }

    @Test
    fun `isNullOrDayFormat returns false when length less than 10`() {
        assertFalse("2023".isNullOrDayFormat())
        assertFalse("2023-05".isNullOrDayFormat())
    }
}
