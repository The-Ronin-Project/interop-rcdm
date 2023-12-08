package com.projectronin.interop.rcdm.validate.element

import com.projectronin.interop.fhir.r4.datatype.Meta
import com.projectronin.interop.fhir.r4.datatype.primitive.Uri
import com.projectronin.interop.fhir.r4.resource.Patient
import com.projectronin.interop.fhir.validate.LocationContext
import com.projectronin.interop.rcdm.common.enums.RoninProfile
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

@Suppress("ktlint:standard:max-line-length")
class MetaValidatorTest {
    private val validator = MetaValidator()

    @Test
    fun `returns supported element`() {
        assertEquals(Meta::class, validator.supportedElement)
    }

    @Test
    fun `meta does not contain single profile`() {
        val meta =
            Meta(
                profile = listOf(),
                source = Uri("http://example.org/source"),
            )

        val validation = validator.validate(meta, listOf(RoninProfile.PATIENT), LocationContext(Patient::meta))
        assertEquals(1, validation.issues().size)
        assertEquals(
            "ERROR RONIN_META_001: one or more expected profiles are missing. Expected: ${RoninProfile.PATIENT.value} @ Patient.meta.profile",
            validation.issues().first().toString(),
        )
    }

    @Test
    fun `meta does not contain one of the profiles`() {
        val meta =
            Meta(
                profile = listOf(RoninProfile.PATIENT.canonical),
                source = Uri("http://example.org/source"),
            )

        val validation =
            validator.validate(
                meta,
                listOf(RoninProfile.PATIENT, RoninProfile.APPOINTMENT),
                LocationContext(Patient::meta),
            )
        assertEquals(1, validation.issues().size)
        assertEquals(
            "ERROR RONIN_META_001: one or more expected profiles are missing. Expected: ${RoninProfile.PATIENT.value}, ${RoninProfile.APPOINTMENT.value} @ Patient.meta.profile",
            validation.issues().first().toString(),
        )
    }

    @Test
    fun `meta does not contain source`() {
        val meta =
            Meta(
                profile = listOf(RoninProfile.PATIENT.canonical),
                source = null,
            )

        val validation = validator.validate(meta, listOf(RoninProfile.PATIENT), LocationContext(Patient::meta))
        assertEquals(1, validation.issues().size)
        assertEquals(
            "ERROR REQ_FIELD: source is a required element @ Patient.meta.source",
            validation.issues().first().toString(),
        )
    }

    @Test
    fun `validation succeeds with single profile`() {
        val meta =
            Meta(
                profile = listOf(RoninProfile.PATIENT.canonical),
                source = Uri("http://example.org/source"),
            )

        val validation = validator.validate(meta, listOf(RoninProfile.PATIENT), LocationContext(Patient::meta))
        assertEquals(0, validation.issues().size)
    }

    @Test
    fun `validation succeeds with multiple profiles`() {
        val meta =
            Meta(
                profile = listOf(RoninProfile.PATIENT.canonical, RoninProfile.APPOINTMENT.canonical),
                source = Uri("http://example.org/source"),
            )

        val validation =
            validator.validate(
                meta,
                listOf(RoninProfile.PATIENT, RoninProfile.APPOINTMENT),
                LocationContext(Patient::meta),
            )
        assertEquals(0, validation.issues().size)
    }
}
