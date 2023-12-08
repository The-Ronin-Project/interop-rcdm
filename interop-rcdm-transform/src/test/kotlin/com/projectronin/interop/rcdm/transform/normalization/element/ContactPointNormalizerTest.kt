package com.projectronin.interop.rcdm.transform.normalization.element

import com.projectronin.interop.fhir.r4.datatype.ContactPoint
import com.projectronin.interop.fhir.r4.datatype.primitive.Code
import com.projectronin.interop.fhir.r4.datatype.primitive.FHIRString
import com.projectronin.interop.tenant.config.model.Tenant
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class ContactPointNormalizerTest {
    private val normalizer = ContactPointNormalizer()

    private val tenant = mockk<Tenant>()

    @Test
    fun `returns proper element type`() {
        assertEquals(ContactPoint::class, normalizer.elementType)
    }

    @Test
    fun `purges a ContactPoint with no value`() {
        val contactPoint =
            ContactPoint(
                system = Code("system"),
            )

        val response = normalizer.normalize(contactPoint, tenant)
        assertNull(response.element)
        assertTrue(response.removeFromElement)
    }

    @Test
    fun `purges a ContactPoint with no system`() {
        val contactPoint =
            ContactPoint(
                value = FHIRString("value"),
            )

        val response = normalizer.normalize(contactPoint, tenant)
        assertNull(response.element)
        assertTrue(response.removeFromElement)
    }

    @Test
    fun `returns original element if it has a system and value`() {
        val contactPoint =
            ContactPoint(
                system = Code("system"),
                value = FHIRString("value"),
            )

        val response = normalizer.normalize(contactPoint, tenant)
        assertEquals(contactPoint, response.element)
        assertFalse(response.removeFromElement)
    }
}
