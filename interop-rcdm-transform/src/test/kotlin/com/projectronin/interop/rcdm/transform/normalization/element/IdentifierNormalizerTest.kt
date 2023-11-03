package com.projectronin.interop.rcdm.transform.normalization.element

import com.projectronin.interop.fhir.r4.datatype.Identifier
import com.projectronin.interop.fhir.r4.datatype.primitive.Uri
import com.projectronin.interop.rcdm.transform.normalization.normalizeIdentifier
import com.projectronin.interop.tenant.config.model.Tenant
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class IdentifierNormalizerTest {
    private val normalizer = IdentifierNormalizer()

    private val tenant = mockk<Tenant>()

    @BeforeEach
    fun setUp() {
        mockkStatic(Uri::normalizeIdentifier)
    }

    @AfterEach
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun `element type is correct`() {
        assertEquals(Identifier::class, normalizer.elementType)
    }

    @Test
    fun `null system`() {
        val identifier = Identifier(system = null)
        val response = normalizer.normalize(identifier, tenant)

        assertEquals(identifier, response.element)
        Assertions.assertFalse(response.removeFromElement)
    }

    @Test
    fun `system not normalized`() {
        val system = Uri("already-normalized")
        val identifier = Identifier(system = system)

        every { system.normalizeIdentifier() } returns system

        val response = normalizer.normalize(identifier, tenant)

        assertEquals(identifier, response.element)
        Assertions.assertFalse(response.removeFromElement)
    }

    @Test
    fun `system normalized`() {
        val system = Uri("original")
        val identifier = Identifier(system = system)

        val normalizedSystem = Uri("normalized")
        every { system.normalizeIdentifier() } returns normalizedSystem

        val response = normalizer.normalize(identifier, tenant)

        val expectedIdentifier = Identifier(system = normalizedSystem)

        assertEquals(expectedIdentifier, response.element)
        Assertions.assertFalse(response.removeFromElement)
    }
}
