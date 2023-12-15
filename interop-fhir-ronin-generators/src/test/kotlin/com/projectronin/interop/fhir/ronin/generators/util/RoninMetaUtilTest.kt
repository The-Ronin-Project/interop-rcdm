package com.projectronin.interop.fhir.ronin.generators.util

import com.projectronin.interop.fhir.r4.datatype.primitive.Id
import com.projectronin.interop.fhir.ronin.generators.resource.TENANT_MNEMONIC
import com.projectronin.interop.rcdm.common.enums.RoninProfile
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class RoninMetaUtilTest {
    @Test
    fun `generate meta`() {
        val roninMeta = rcdmMeta(RoninProfile.PATIENT, TENANT_MNEMONIC) {}
        assertEquals("http://projectronin.io/fhir/StructureDefinition/ronin-patient", roninMeta.profile[0].value)
        assertEquals(TENANT_MNEMONIC, roninMeta.source!!.value)
    }

    @Test
    fun `generates meta with additional values`() {
        val roninMeta =
            rcdmMeta(RoninProfile.APPOINTMENT, TENANT_MNEMONIC) {
                versionId of Id("x")
            }
        assertEquals("http://projectronin.io/fhir/StructureDefinition/ronin-appointment", roninMeta.profile[0].value)
        assertEquals(TENANT_MNEMONIC, roninMeta.source!!.value)
        assertEquals("x", roninMeta.versionId!!.value)
    }
}
