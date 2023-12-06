package com.projectronin.interop.rcdm.common.enums

import com.projectronin.interop.fhir.r4.datatype.primitive.Code
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class MedicationEnumsTest {
    @Test
    fun `OriginalDynamicType works`() {
        val enums = OriginalDynamicType.values()
        assertEquals(1, enums.size)
    }

    @Test
    fun `OriginalMedDataType works`() {
        val enums = OriginalMedDataType.values()
        assertEquals(4, enums.size)
    }

    @Test
    fun `OriginalMedDataType from works`() {
        val enum = OriginalMedDataType.from(Code("literal reference"))
        assertEquals(OriginalMedDataType.LiteralReference, enum)
    }
}
