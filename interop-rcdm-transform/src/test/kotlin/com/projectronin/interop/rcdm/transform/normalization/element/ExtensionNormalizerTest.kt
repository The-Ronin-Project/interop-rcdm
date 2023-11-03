package com.projectronin.interop.rcdm.transform.normalization.element

import com.projectronin.interop.fhir.r4.datatype.DynamicValue
import com.projectronin.interop.fhir.r4.datatype.DynamicValueType
import com.projectronin.interop.fhir.r4.datatype.Extension
import com.projectronin.interop.fhir.r4.datatype.primitive.FHIRBoolean
import com.projectronin.interop.fhir.r4.datatype.primitive.Uri
import com.projectronin.interop.rcdm.common.enums.RoninExtension
import com.projectronin.interop.tenant.config.model.Tenant
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class ExtensionNormalizerTest {
    private val normalizer = ExtensionNormalizer()

    private val tenant = mockk<Tenant>()

    @Test
    fun `element type is correct`() {
        assertEquals(Extension::class, normalizer.elementType)
    }

    @Test
    fun `url is null`() {
        val extension = Extension(
            url = null
        )

        val response = normalizer.normalize(extension, tenant)

        assertNull(response.element)
        assertTrue(response.removeFromElement)
    }

    @Test
    fun `url value is null`() {
        val extension = Extension(
            url = Uri(null)
        )

        val response = normalizer.normalize(extension, tenant)

        assertNull(response.element)
        assertTrue(response.removeFromElement)
    }

    @Test
    fun `url is recognized by Ronin`() {
        val extension = Extension(
            url = RoninExtension.RONIN_DATA_AUTHORITY_EXTENSION.uri
        )

        val response = normalizer.normalize(extension, tenant)

        assertEquals(extension, response.element)
        assertFalse(response.removeFromElement)
    }

    @Test
    fun `value is null and no extensions`() {
        val extension = Extension(
            url = Uri("http://example.org/fake-extension"),
            value = null,
            extension = listOf()
        )

        val response = normalizer.normalize(extension, tenant)

        assertNull(response.element)
        assertTrue(response.removeFromElement)
    }

    @Test
    fun `value is not null`() {
        val extension = Extension(
            url = Uri("http://example.org/fake-extension"),
            value = DynamicValue(DynamicValueType.BOOLEAN, FHIRBoolean.TRUE)
        )

        val response = normalizer.normalize(extension, tenant)

        assertEquals(extension, response.element)
        assertFalse(response.removeFromElement)
    }

    @Test
    fun `extensions exist`() {
        val extension = Extension(
            url = Uri("http://example.org/fake-extension"),
            extension = listOf(
                Extension(
                    url = Uri("http://example.org/fake-sub-extension"),
                    value = DynamicValue(DynamicValueType.BOOLEAN, FHIRBoolean.TRUE)
                )
            )
        )

        val response = normalizer.normalize(extension, tenant)

        assertEquals(extension, response.element)
        assertFalse(response.removeFromElement)
    }
}
