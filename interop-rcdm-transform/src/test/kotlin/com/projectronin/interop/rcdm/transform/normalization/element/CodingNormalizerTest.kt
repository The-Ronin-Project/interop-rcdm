package com.projectronin.interop.rcdm.transform.normalization.element

import com.projectronin.interop.fhir.r4.datatype.Coding
import com.projectronin.interop.fhir.r4.datatype.primitive.Uri
import com.projectronin.interop.rcdm.transform.normalization.normalizeCoding
import com.projectronin.interop.tenant.config.model.Tenant
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class CodingNormalizerTest {
    private val normalizer = CodingNormalizer()

    private val tenant = mockk<Tenant>()

    @BeforeEach
    fun setUp() {
        mockkStatic(Uri::normalizeCoding)
    }

    @AfterEach
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun `element type is correct`() {
        assertEquals(Coding::class, normalizer.elementType)
    }

    @Test
    fun `null system`() {
        val coding = Coding(system = null)
        val response = normalizer.normalize(coding, tenant)

        assertEquals(coding, response.element)
        assertFalse(response.removeFromElement)
    }

    @Test
    fun `system not normalized`() {
        val system = Uri("already-normalized")
        val coding = Coding(system = system)

        every { system.normalizeCoding() } returns system

        val response = normalizer.normalize(coding, tenant)

        assertEquals(coding, response.element)
        assertFalse(response.removeFromElement)
    }

    @Test
    fun `system normalized`() {
        val system = Uri("original")
        val coding = Coding(system = system)

        val normalizedSystem = Uri("normalized")
        every { system.normalizeCoding() } returns normalizedSystem

        val response = normalizer.normalize(coding, tenant)

        val expectedCoding = Coding(system = normalizedSystem)

        assertEquals(expectedCoding, response.element)
        assertFalse(response.removeFromElement)
    }
}
