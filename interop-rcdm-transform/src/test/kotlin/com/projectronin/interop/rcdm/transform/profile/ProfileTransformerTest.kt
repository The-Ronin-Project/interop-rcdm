package com.projectronin.interop.rcdm.transform.profile

import com.projectronin.interop.fhir.r4.datatype.Meta
import com.projectronin.interop.fhir.r4.datatype.primitive.Canonical
import com.projectronin.interop.fhir.r4.resource.Organization
import com.projectronin.interop.fhir.r4.resource.Patient
import com.projectronin.interop.fhir.r4.resource.Resource
import com.projectronin.interop.rcdm.common.enums.RCDMVersion
import com.projectronin.interop.rcdm.common.enums.RoninProfile
import com.projectronin.interop.rcdm.transform.model.TransformResponse
import com.projectronin.interop.tenant.config.model.Tenant
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import kotlin.reflect.KClass

class ProfileTransformerTest {
    private val transformer = object : ProfileTransformer<Patient>() {
        var response: TransformResponse<Patient>? = null

        override fun transformInternal(original: Patient, tenant: Tenant): TransformResponse<Patient>? = response

        override val supportedResource: KClass<Patient> = Patient::class
        override val profile: RoninProfile = RoninProfile.PATIENT
        override val rcdmVersion: RCDMVersion = RCDMVersion.V3_19_0
        override val profileVersion: Int = 1
    }

    private val tenant = mockk<Tenant>()

    @Test
    fun `returns isDefault as true`() {
        assertTrue(transformer.isDefault)
    }

    @Test
    fun `returns qualifies as true`() {
        assertTrue(transformer.qualifies(mockk()))
    }

    @Test
    fun `transforms meta with no profiles`() {
        val patient = Patient()

        transformer.response = TransformResponse(patient)

        val transformResponse = transformer.transform(patient, tenant)
        transformResponse!!

        val expectedPatient = Patient(meta = Meta(profile = listOf(RoninProfile.PATIENT.canonical)))
        assertEquals(expectedPatient, transformResponse.resource)
        assertEquals(listOf<Resource<*>>(), transformResponse.embeddedResources)
    }

    @Test
    fun `transforms meta with no Ronin profiles`() {
        val patient = Patient(
            meta = Meta(profile = listOf(Canonical("http://example.org/profile")))
        )

        transformer.response = TransformResponse(patient)

        val transformResponse = transformer.transform(patient, tenant)
        transformResponse!!

        val expectedPatient = Patient(meta = Meta(profile = listOf(RoninProfile.PATIENT.canonical)))
        assertEquals(expectedPatient, transformResponse.resource)
        assertEquals(listOf<Resource<*>>(), transformResponse.embeddedResources)
    }

    @Test
    fun `transforms meta with prior Ronin profiles`() {
        val patient = Patient(
            meta = Meta(profile = listOf(RoninProfile.APPOINTMENT.canonical))
        )

        transformer.response = TransformResponse(patient)

        val transformResponse = transformer.transform(patient, tenant)
        transformResponse!!

        val expectedPatient =
            Patient(meta = Meta(profile = listOf(RoninProfile.APPOINTMENT.canonical, RoninProfile.PATIENT.canonical)))
        assertEquals(expectedPatient, transformResponse.resource)
        assertEquals(listOf<Resource<*>>(), transformResponse.embeddedResources)
    }

    @Test
    fun `transforms handles when child returns null`() {
        val patient = Patient()

        transformer.response = null

        val transformResponse = transformer.transform(patient, tenant)
        assertNull(transformResponse)
    }

    @Test
    fun `transforms with embedded resources`() {
        val patient = Patient()
        val organization = mockk<Organization>()

        transformer.response = TransformResponse(patient, listOf(organization))

        val transformResponse = transformer.transform(patient, tenant)
        transformResponse!!

        val expectedPatient = Patient(meta = Meta(profile = listOf(RoninProfile.PATIENT.canonical)))
        assertEquals(expectedPatient, transformResponse.resource)
        assertEquals(listOf(organization), transformResponse.embeddedResources)
    }
}
