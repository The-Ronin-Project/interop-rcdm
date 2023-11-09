package com.projectronin.interop.rcdm.transform

import com.projectronin.interop.fhir.r4.resource.Organization
import com.projectronin.interop.fhir.r4.resource.Patient
import com.projectronin.interop.fhir.r4.resource.Resource
import com.projectronin.interop.fhir.validate.Validation
import com.projectronin.interop.fhir.validate.ValidationIssue
import com.projectronin.interop.fhir.validate.ValidationIssueSeverity
import com.projectronin.interop.rcdm.common.validation.ValidationClient
import com.projectronin.interop.rcdm.transform.localization.Localizer
import com.projectronin.interop.rcdm.transform.map.MapResponse
import com.projectronin.interop.rcdm.transform.map.MappingService
import com.projectronin.interop.rcdm.transform.model.TransformResponse
import com.projectronin.interop.rcdm.transform.normalization.Normalizer
import com.projectronin.interop.rcdm.transform.profile.ProfileTransformer
import com.projectronin.interop.tenant.config.model.Tenant
import io.mockk.Called
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import java.time.LocalDateTime
import java.util.UUID

class TransformManagerTest {
    private val tenant = mockk<Tenant> {
        every { mnemonic } returns "tenant"
    }

    private val mappingService = mockk<MappingService> {
        every { map(any<Patient>(), tenant, any()) } answers { MapResponse(firstArg(), Validation()) }
    }

    private val passingValidation = mockk<Validation> {
        every { hasIssues() } returns false
    }
    private val failingValidation = mockk<Validation> {
        every { hasIssues() } returns true
        every { issues() } returns listOf(ValidationIssue(ValidationIssueSeverity.ERROR, "ERR", "Error"))
    }

    private val validationClient = mockk<ValidationClient> {
        every { reportIssues(any(), any<Patient>(), "tenant") } returns UUID.randomUUID()
    }

    private val original = mockk<Patient>()
    private val normalized = mockk<Patient>()
    private val mapped = mockk<Patient>()
    private val transformed = mockk<Patient>()
    private val localized = mockk<Patient>()

    @Test
    fun `mapping service returns validation error with no mapped resource`() {
        val normalizer = mockk<Normalizer> {
            every { normalize(original, tenant) } returns normalized
        }

        every { mappingService.map(normalized, tenant, null) } returns MapResponse(null, failingValidation)

        val transformer = mockk<ProfileTransformer<Patient>> {
            every { supportedResource } returns Patient::class
        }
        val localizer = mockk<Localizer> { }

        val manager = TransformManager(normalizer, mappingService, validationClient, listOf(transformer), localizer)
        val transformResponse = manager.transformResource(original, tenant)
        assertNull(transformResponse)

        verify(exactly = 1) { validationClient.reportIssues(failingValidation, normalized, "tenant") }
        verify(exactly = 0) { transformer.transform(any(), any()) }
        verify { localizer wasNot Called }
    }

    @Test
    fun `mapping resource returns validation error with mapped resource`() {
        val normalizer = mockk<Normalizer> {
            every { normalize(original, tenant) } returns normalized
        }

        every { mappingService.map(normalized, tenant, null) } returns MapResponse(mapped, failingValidation)

        val transformer = mockk<ProfileTransformer<Patient>> {
            every { supportedResource } returns Patient::class
            every { isDefault } returns true
            every { transform(mapped, tenant) } returns TransformResponse(transformed)
        }

        val localizer = mockk<Localizer> {
            every { localize(transformed, tenant) } returns localized
        }

        val manager = TransformManager(normalizer, mappingService, validationClient, listOf(transformer), localizer)
        val transformResponse = manager.transformResource(original, tenant)

        transformResponse!!
        assertEquals(localized, transformResponse.resource)
        assertEquals(listOf<Resource<*>>(), transformResponse.embeddedResources)

        verify(exactly = 1) { validationClient.reportIssues(failingValidation, mapped, "tenant") }
    }

    @Test
    fun `force cache reload is sent to mapper`() {
        val forceCacheReload = LocalDateTime.now()

        val normalizer = mockk<Normalizer> {
            every { normalize(original, tenant) } returns normalized
        }

        every { mappingService.map(normalized, tenant, forceCacheReload) } returns MapResponse(
            mapped,
            passingValidation
        )

        val transformer = mockk<ProfileTransformer<Patient>> {
            every { supportedResource } returns Patient::class
            every { isDefault } returns true
            every { transform(mapped, tenant) } returns TransformResponse(transformed)
        }

        val localizer = mockk<Localizer> {
            every { localize(transformed, tenant) } returns localized
        }

        val manager = TransformManager(normalizer, mappingService, validationClient, listOf(transformer), localizer)
        val transformResponse = manager.transformResource(original, tenant, forceCacheReload)

        transformResponse!!
        assertEquals(localized, transformResponse.resource)
        assertEquals(listOf<Resource<*>>(), transformResponse.embeddedResources)
    }

    @Test
    fun `no qualified or default transformer found for resource`() {
        val normalizer = mockk<Normalizer> {
            every { normalize(original, tenant) } returns normalized
        }
        val transformer = mockk<ProfileTransformer<Patient>> {
            every { supportedResource } returns Patient::class
            every { isDefault } returns false
            every { qualifies(normalized) } returns false
        }

        val localizer = mockk<Localizer> { }

        val manager = TransformManager(normalizer, mappingService, validationClient, listOf(transformer), localizer)
        val transformResponse = manager.transformResource(original, tenant)
        assertNull(transformResponse)

        verify { localizer wasNot Called }
    }

    @Test
    fun `no qualified transformer found but default transformer found`() {
        val normalizer = mockk<Normalizer> {
            every { normalize(original, tenant) } returns normalized
        }
        val transformer1 = mockk<ProfileTransformer<Patient>> {
            every { supportedResource } returns Patient::class
            every { isDefault } returns true
            every { transform(normalized, tenant) } returns TransformResponse(transformed)
        }
        val transformer2 = mockk<ProfileTransformer<Patient>> {
            every { supportedResource } returns Patient::class
            every { isDefault } returns false
            every { qualifies(normalized) } returns false
        }
        val transformer3 = mockk<ProfileTransformer<Patient>> {
            every { supportedResource } returns Patient::class
            every { isDefault } returns false
            every { qualifies(normalized) } returns false
        }

        val localizer = mockk<Localizer> {
            every { localize(transformed, tenant) } returns localized
        }

        val manager = TransformManager(
            normalizer,
            mappingService,
            validationClient,
            listOf(transformer1, transformer2, transformer3),
            localizer
        )
        val transformResponse = manager.transformResource(original, tenant)

        transformResponse!!
        assertEquals(localized, transformResponse.resource)
        assertEquals(listOf<Resource<*>>(), transformResponse.embeddedResources)
    }

    @Test
    fun `single transformer qualifies and fails transform`() {
        val normalizer = mockk<Normalizer> {
            every { normalize(original, tenant) } returns normalized
        }
        val transformer1 = mockk<ProfileTransformer<Patient>> {
            every { supportedResource } returns Patient::class
            every { isDefault } returns true
        }
        val transformer2 = mockk<ProfileTransformer<Patient>> {
            every { supportedResource } returns Patient::class
            every { isDefault } returns false
            every { qualifies(normalized) } returns true
            every { transform(normalized, tenant) } returns null
        }
        val transformer3 = mockk<ProfileTransformer<Patient>> {
            every { supportedResource } returns Patient::class
            every { isDefault } returns false
            every { qualifies(normalized) } returns false
        }

        val localizer = mockk<Localizer> { }

        val manager = TransformManager(
            normalizer,
            mappingService,
            validationClient,
            listOf(transformer1, transformer2, transformer3),
            localizer
        )
        val transformResponse = manager.transformResource(original, tenant)
        assertNull(transformResponse)

        verify { localizer wasNot Called }
    }

    @Test
    fun `single transformer qualifies and passes transform`() {
        val normalizer = mockk<Normalizer> {
            every { normalize(original, tenant) } returns normalized
        }
        val transformer1 = mockk<ProfileTransformer<Patient>> {
            every { supportedResource } returns Patient::class
            every { isDefault } returns true
        }
        val transformer2 = mockk<ProfileTransformer<Patient>> {
            every { supportedResource } returns Patient::class
            every { isDefault } returns false
            every { qualifies(normalized) } returns true
            every { transform(normalized, tenant) } returns TransformResponse(transformed)
        }
        val transformer3 = mockk<ProfileTransformer<Patient>> {
            every { supportedResource } returns Patient::class
            every { isDefault } returns false
            every { qualifies(normalized) } returns false
        }

        val localizer = mockk<Localizer> {
            every { localize(transformed, tenant) } returns localized
        }

        val manager = TransformManager(
            normalizer,
            mappingService,
            validationClient,
            listOf(transformer1, transformer2, transformer3),
            localizer
        )
        val transformResponse = manager.transformResource(original, tenant)

        transformResponse!!
        assertEquals(localized, transformResponse.resource)
        assertEquals(listOf<Resource<*>>(), transformResponse.embeddedResources)
    }

    @Test
    fun `multiple transformers qualify and one fails transform`() {
        val normalizer = mockk<Normalizer> {
            every { normalize(original, tenant) } returns normalized
        }
        val transformer1 = mockk<ProfileTransformer<Patient>> {
            every { supportedResource } returns Patient::class
            every { isDefault } returns true
        }
        val transformer2 = mockk<ProfileTransformer<Patient>> {
            every { supportedResource } returns Patient::class
            every { isDefault } returns false
            every { qualifies(normalized) } returns true
            every { transform(normalized, tenant) } returns TransformResponse(transformed)
        }
        val transformer3 = mockk<ProfileTransformer<Patient>> {
            every { supportedResource } returns Patient::class
            every { isDefault } returns false
            every { qualifies(normalized) } returns true
            every { transform(transformed, tenant) } returns null
        }

        val localizer = mockk<Localizer> { }

        val manager = TransformManager(
            normalizer,
            mappingService,
            validationClient,
            listOf(transformer1, transformer2, transformer3),
            localizer
        )
        val transformResponse = manager.transformResource(original, tenant)
        assertNull(transformResponse)

        verify { localizer wasNot Called }
    }

    @Test
    fun `multiple transformers qualify and all transform`() {
        val transformed2 = mockk<Patient>()

        val normalizer = mockk<Normalizer> {
            every { normalize(original, tenant) } returns normalized
        }
        val transformer1 = mockk<ProfileTransformer<Patient>> {
            every { supportedResource } returns Patient::class
            every { isDefault } returns true
        }
        val transformer2 = mockk<ProfileTransformer<Patient>> {
            every { supportedResource } returns Patient::class
            every { isDefault } returns false
            every { qualifies(normalized) } returns true
            every { transform(normalized, tenant) } returns TransformResponse(transformed)
        }
        val transformer3 = mockk<ProfileTransformer<Patient>> {
            every { supportedResource } returns Patient::class
            every { isDefault } returns false
            every { qualifies(normalized) } returns true
            every { transform(transformed, tenant) } returns TransformResponse(transformed2)
        }

        val localizer = mockk<Localizer> {
            every { localize(transformed2, tenant) } returns localized
        }

        val manager = TransformManager(
            normalizer,
            mappingService,
            validationClient,
            listOf(transformer1, transformer2, transformer3),
            localizer
        )
        val transformResponse = manager.transformResource(original, tenant)

        transformResponse!!
        assertEquals(localized, transformResponse.resource)
        assertEquals(listOf<Resource<*>>(), transformResponse.embeddedResources)
    }

    @Test
    fun `multiple transformers qualify and one transforms with embedded resources`() {
        val transformed2 = mockk<Patient>()
        val embedded1 = mockk<Organization>()

        val normalizer = mockk<Normalizer> {
            every { normalize(original, tenant) } returns normalized
        }
        val transformer1 = mockk<ProfileTransformer<Patient>> {
            every { supportedResource } returns Patient::class
            every { isDefault } returns true
        }
        val transformer2 = mockk<ProfileTransformer<Patient>> {
            every { supportedResource } returns Patient::class
            every { isDefault } returns false
            every { qualifies(normalized) } returns true
            every { transform(normalized, tenant) } returns TransformResponse(transformed, listOf(embedded1))
        }
        val transformer3 = mockk<ProfileTransformer<Patient>> {
            every { supportedResource } returns Patient::class
            every { isDefault } returns false
            every { qualifies(normalized) } returns true
            every { transform(transformed, tenant) } returns TransformResponse(transformed2)
        }

        val localizer = mockk<Localizer> {
            every { localize(transformed2, tenant) } returns localized
        }

        val manager = TransformManager(
            normalizer,
            mappingService,
            validationClient,
            listOf(transformer1, transformer2, transformer3),
            localizer
        )
        val transformResponse = manager.transformResource(original, tenant)

        transformResponse!!
        assertEquals(localized, transformResponse.resource)
        assertEquals(listOf(embedded1), transformResponse.embeddedResources)
    }

    @Test
    fun `multiple transformers qualify and multiple transform with embedded resources`() {
        val transformed2 = mockk<Patient>()
        val embedded1 = mockk<Organization>()
        val embedded2 = mockk<Organization>()

        val normalizer = mockk<Normalizer> {
            every { normalize(original, tenant) } returns normalized
        }
        val transformer1 = mockk<ProfileTransformer<Patient>> {
            every { supportedResource } returns Patient::class
            every { isDefault } returns true
        }
        val transformer2 = mockk<ProfileTransformer<Patient>> {
            every { supportedResource } returns Patient::class
            every { isDefault } returns false
            every { qualifies(normalized) } returns true
            every { transform(normalized, tenant) } returns TransformResponse(transformed, listOf(embedded1))
        }
        val transformer3 = mockk<ProfileTransformer<Patient>> {
            every { supportedResource } returns Patient::class
            every { isDefault } returns false
            every { qualifies(normalized) } returns true
            every { transform(transformed, tenant) } returns TransformResponse(transformed2, listOf(embedded2))
        }

        val localizer = mockk<Localizer> {
            every { localize(transformed2, tenant) } returns localized
        }

        val manager = TransformManager(
            normalizer,
            mappingService,
            validationClient,
            listOf(transformer1, transformer2, transformer3),
            localizer
        )
        val transformResponse = manager.transformResource(original, tenant)

        transformResponse!!
        assertEquals(localized, transformResponse.resource)
        assertEquals(listOf(embedded1, embedded2), transformResponse.embeddedResources)
    }
}
